package uk.me.candle.eve.pricing;

// <editor-fold defaultstate="collapsed" desc="imports">

import uk.me.candle.eve.pricing.options.PricingOptions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

// </editor-fold>
/**
 *
 * @author Candle
 */
public abstract class AbstractPricing implements Pricing {
    private static final Logger logger = Logger.getLogger(AbstractPricing.class);

    // <editor-fold defaultstate="collapsed" desc="fields">
    /**
     * internal mem-cache of prices.
     */
    private Map<Integer, CachedPrice> cache;
    /**
     * list of listeners to notify when a price has been fetched.
     */
    private List<WeakReference<PricingListener>> pricingListeners = new ArrayList<WeakReference<PricingListener>>();
    /**
     * list of listeners to notify when a fetch is starting and finishing.
     */
    private List<WeakReference<PricingFetchListener>> pricingFetchListeners = new ArrayList<WeakReference<PricingFetchListener>>();
    /**
     * queue of itemIDs that are waiting to be fetched
     */
    private Queue<Integer> waitingQueue = new ConcurrentLinkedQueue<Integer>();
    /**
     * list of item IDs that are being fetched - this is here so that we don't queue an ID that is in the process of
     * being fetched.
     */
    private List<Integer> currentlyEvaluating = Collections.synchronizedList(new ArrayList<Integer>());
    /**
     * single thread that handles the price fetching.
     */
    private PriceFetchingThread priceFetchingThread;

    /**
     * maintains the list of failed attempts to fetch prices.
     */
    private final Map<Integer, AtomicInteger> failedFetchAttempts = Collections.synchronizedMap(new HashMap<Integer, AtomicInteger>());

    /**
     * map of reasons why fetching a price has failed.
     */
    private final Map<Integer, List<String>> failedFetchReasons = Collections.synchronizedMap(new HashMap<Integer, List<String>>());

    /**
     * the length of time to keep prices in the cache.
     */
    long cacheTimer = 60*60*1000l;

	/**
	 * If this is true then the fetching thread will exit.
	 */
	private boolean shuttingDown = false;

	/**
	 * If this is true then the fetch thread will stop fetching when it next
	 * hits a check on it. Should help to fix
	 * http://code.google.com/p/jeveassets/issues/detail?id=130
	 */
	private boolean cancelAll = false;

    /**
     * defines some options.
     */
    PricingOptions options;
    // </editor-fold>

    public AbstractPricing() { }
    
    //@NotNull
    private Map<Integer, CachedPrice> createCache() {
        if (cache == null) {
            if (options != null) {
                try {
                    read(options.getCacheInputStream());
                } catch (IOException ioe) {
                    logger.error("Error reading the cache file (Other IO error)", ioe);
                    cache = Collections.synchronizedMap(new HashMap<Integer, CachedPrice>());
                } catch (ClassNotFoundException cnfe) {
                    logger.error("Error reading the cache file (incompatible classes)", cnfe);
                    cache = Collections.synchronizedMap(new HashMap<Integer, CachedPrice>());
                }
            } else {
                cache = Collections.synchronizedMap(new HashMap<Integer, CachedPrice>());
            }
        }
        return cache;
    }

    @Override
    public Double getPrice(int itemID, PricingType type, PricingNumber number) {
		if (priceFetchingThread == null) {
			priceFetchingThread = new PriceFetchingThread();
			priceFetchingThread.start();
		}

        CachedPrice cp = createCache().get(itemID);
        double price;
        // Queue the price for fetching, if: we do not have a cached price
        // If we have a cached price, then queue the fetch only if the
        // cache timers are enabled.
        boolean cpDoesNotExists = cp == null;
        boolean cacheTimout = cp != null && (cp.getTime()+cacheTimer) < System.currentTimeMillis();
        boolean useCaches = options.getCacheTimersEnabled();
        if (cpDoesNotExists || ( useCaches && cacheTimout )) {
            queuePrice(itemID);
            return null;
        } else {
            price = cp.getContainer().getPrice(type, number);
        }
        return price;
    }

    @Override
    public long getNextUpdateTime(int itemID) {
        CachedPrice cp = createCache().get(itemID);
        if (cp == null) return -1; // needs an update.
        return cp.getTime();
    }

    @Override
    public Double getPrice(int itemID) {
        return getPrice(itemID, options.getPricingType(), options.getPricingNumber());
    }

    /**
     * fetches an XML document.
     * @param url
     * @return
     * @throws java.net.SocketTimeoutException if the source data took too long to retrieve
     * @throws org.dom4j.DocumentException if the source data could not be parsed into a Document
     * @throws java.io.IOException
     */
    protected Document getDocument(URL url) throws SocketTimeoutException, DocumentException, IOException {
        if (logger.isDebugEnabled()) logger.debug("Fetching URL: " + url);
        URLConnection urlCon = url.openConnection(options.getProxy());
        urlCon.setReadTimeout(180000); // 3 minute timeout.
        urlCon.setDoInput(true);
        //urlCon.setRequestProperty("User-Agent", "uk.me.candle.eve.pricing : Java Pricing");

        BufferedReader br = new BufferedReader(new InputStreamReader(urlCon.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while (null != (line = br.readLine())) {
            sb.append(line);
        }
        br.close();

        Document d = DocumentHelper.parseText(sb.toString());

        if (logger.isDebugEnabled()) logger.debug("Fetched URL");
        return d;
    }

    @Override
    public void setPricingOptions(PricingOptions options) {
        this.options = options;
        cacheTimer = options.getPriceCacheTimer();
    }

    /**
     * fetches a single price from the data source
     * @param itemID the item ID for the item
     * @return a double representing the price of the item
     */
    abstract protected PriceContainer fetchPrice(int itemID);
    /**
     * fetches the prices for the collection of itemIDs
     * @param itemIDs
     * @return a map of itemID => price
     */
    abstract protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> itemIDs);


    /**
     * defines the batch size for fetching grouped prices.
     * less then or equal to 0 means that there is no limit.
     * @return
     */
    protected int getbatchSize() {
        return -1;
    }

    public long getCacheTimer() {
        return cacheTimer;
    }

    public void setCacheTimer(long cacheTimer) {
        this.cacheTimer = cacheTimer;
    }

    @Override
    public void setPrice(int itemID, PricingType type, PricingNumber number, Double price) {
        long cacheTime = 0;
        if (price > 0) {
            // can't use the Long.MAX_VALUE as the check for "does this item need updating"
            // is the cacheTime + cacheTimer, which, if the cacheTime is MAX_VALUE, the sum
            // overflows, causing the it to update immediatly.
            cacheTime = System.currentTimeMillis()+(10L*365L*24L*60L*60L);
        } else {
            cacheTime = -1;
        }
        CachedPrice current = createCache().get(itemID);
        PriceContainer.PriceContainerBuilder builder;
        if (current != null) {
            builder = current.getContainer().createClone();
        } else {
            builder = new PriceContainer.PriceContainerBuilder();
        }
        builder.putPrice(type, number, price);
        createCache().put(itemID, new CachedPrice(cacheTime, builder.build()));
        notifyPricingListeners(itemID);
    }

    @Override
    public void setPrice(int itemID, Double price) {
        setPrice(itemID, options.getPricingType(), options.getPricingNumber(), price);
    }

    public Map<Integer, CachedPrice> getCache() {
        return Collections.unmodifiableMap(createCache());
    }

    public void setOptions(PricingOptions options) {
        this.options = options;
        this.cacheTimer = options.getPriceCacheTimer();
    }

    public PricingOptions getPricingOptions() {
        return options;
    }

    @Override
    public boolean removePricingListener(PricingListener o) {
        for (int i = pricingListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingListener> wpl = pricingListeners.get(i);
            PricingListener pl = wpl.get();
            if (pl == null) {
                pricingListeners.remove(i);
                continue;
            } else {
                if (pl.equals(o)) {
                    pricingListeners.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    private void notifyPricingListeners(Integer itemId) {
        if (logger.isDebugEnabled()) logger.debug("notifying " + pricingListeners.size() + " listeners with " + itemId);
        for (int i = pricingListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingListener> wpl = pricingListeners.get(i);
            PricingListener pl = wpl.get();
            if (pl == null) {
                pricingListeners.remove(i);
                continue;
            } else {
                pl.priceUpdated(itemId, this);
            }
        }
    }

    @Override
    public boolean addPricingListener(PricingListener pl) {
        return pricingListeners.add(new WeakReference<PricingListener>(pl));
    }

    @Override
    public boolean addPricingFetchListener(PricingFetchListener pfl) {
        return pricingFetchListeners.add(new WeakReference<PricingFetchListener>(pfl));
    }

    @Override
    public boolean removePricingFetchListener(PricingFetchListener o) {
        for (int i = pricingFetchListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingFetchListener> wpl = pricingFetchListeners.get(i);
            PricingFetchListener pl = wpl.get();
            if (pl == null) {
                pricingFetchListeners.remove(i);
                continue;
            } else {
                if (pl.equals(o)) {
                    pricingFetchListeners.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    private void notifyPricingFetchListeners(boolean started) {
        if (logger.isDebugEnabled()) logger.debug("notifying " + pricingFetchListeners.size() + " fetch listeners. [" + started + "]");
        for (int i = pricingFetchListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingFetchListener> wpl = pricingFetchListeners.get(i);
            PricingFetchListener pl = wpl.get();
            if (pl == null) {
                pricingFetchListeners.remove(i);
                continue;
            } else {
                if (started) {
                    pl.fetchStarted();
                } else {
                    pl.fetchEnded();
                }
            }
        }
    }

    public static class CachedPrice implements Serializable {
        private static final long serialVersionUID = 76589234632l;
        long time;
        PriceContainer container;

        public CachedPrice(long time, PriceContainer container) {
            this.time = time;
            this.container = container;
        }

        public PriceContainer getContainer() {
            return container;
        }

        public long getTime() {
            return time;
        }
    }

    /**
     * only adds items that are not already in the queue, or currently being evaluated.
     * @param itemID
     */
    private void queuePrice(int itemID) {
        if (!waitingQueue.contains(itemID) && !currentlyEvaluating.contains(itemID) && checkFailureCountExceeded(itemID)) {
            if (logger.isTraceEnabled()) logger.trace("queued " + itemID + " for price fetching");
            waitingQueue.add(itemID);
        }
        synchronized(priceFetchingThread) {
            priceFetchingThread.notify();
        }
    }

    /**
     * The thread that handles the price fetching.
     * It wait()s until there are items in the queue (queuePrice(...) notifies it) then removes items from the queue,
     * and fetches the price using the specific implementation
     */
    private class PriceFetchingThread extends Thread {
        boolean started = false;
        Set<Integer> evaluate = new HashSet<Integer>();

        public PriceFetchingThread() {
            super("Price Fetching");
        }

        @Override
        public void run() {
            started = true;
            while (!shuttingDown) {
                if (waitingQueue.size() == 0 || cancelAll) {
					cancelAll = false;
                    try {
                        synchronized(this) {
                            if (logger.isDebugEnabled()) logger.debug("Pricing fetch thread is waiting.");
                            this.wait();
                        }
                    } catch (InterruptedException ie) {
                        logger.warn("Pricing fetch thread interrupted.");
                        // just continue.
                    }
                }
                notifyPricingFetchListeners(true);
                // fill the queue that is waiting, up to the size of the batch size.
                // A = queueSize > 0
                // B = batchSize > 0
                // C = curSize < batchSize
                // I need the truth table:
                // A B C ==> R
                // 0 0 0     0
                // 0 0 1     0
                // 0 1 0     0
                // 0 1 1     0
                // 1 0 0     1
                // 1 0 1     1
                // 1 1 0     0
                // 1 1 1     1
                // ==> A & ((B & C) | C)
                // ==> A & (B | C)
                if (logger.isDebugEnabled()) logger.debug("There are " + waitingQueue.size() + " items in the queue.");
                while (waitingQueue.size() > 0 && (!(getbatchSize() > 0) || evaluate.size() < getbatchSize())) {
                    Integer num = waitingQueue.remove();
                    if (checkFailureCountExceeded(num)) {
                        evaluate.add(num);
                        currentlyEvaluating.add(num);
                    }
                }
                if (logger.isDebugEnabled()) logger.debug("Starting price fetch for " + evaluate.size() + " items.");
                // handle the list.
                Map<Integer, PriceContainer> prices = fetchPrices(evaluate);
                // for each of the prices, cache, and notify listeners
                for (Integer itemId : evaluate) {
                    if (prices.containsKey(itemId)) {
                        PriceContainer priceContainer = prices.get(itemId);
                        cache.put(itemId, new CachedPrice(System.currentTimeMillis(), priceContainer));
                        notifyPricingListeners(itemId);
                    } else {
                        addFailureCount(itemId);
                        waitingQueue.add(itemId); // add prices that were unable to be fetched back onto the queue.
                    }
                }
                evaluate.clear();
                currentlyEvaluating.clear();
                if (logger.isDebugEnabled()) logger.debug("finished fetch");
                notifyPricingFetchListeners(false);
            }

			logger.debug("Price Fetching Thread ending.");
			priceFetchingThread = null;
        }

        public boolean isStarted() {
            return started;
        }
    }

    @Override
    public void writeCache() throws IOException {
        write(options.getCacheOutputStream());
    }

    @SuppressWarnings("unchecked")
    private void read(InputStream input) throws IOException, ClassNotFoundException {
        if (input != null) {
            ObjectInputStream oos = new ObjectInputStream(input);
            cache = (Map<Integer, CachedPrice>)oos.readObject();
        }
        if (cache == null) {
            cache = new HashMap<Integer, CachedPrice>();
        }
    }

    private void write(OutputStream output) throws IOException {
        if (output != null) {
            ObjectOutputStream oos = new ObjectOutputStream(output);
            oos.writeObject(cache);
            oos.flush();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        write(options.getCacheOutputStream());
    }

    @Override
    public void cancelAll() {
		cancelAll = true;
        waitingQueue.clear();
    }

    public void shutdown() {
        shuttingDown = true;
        waitingQueue.clear();
        if (priceFetchingThread != null) {
            priceFetchingThread.interrupt();
        }
    }

    @Override
    public void cancelSingle(int itemID) {
        waitingQueue.remove(itemID);
    }

    @Override
    public void resetAllAttemptCounters() {
        failedFetchAttempts.clear();
        failedFetchReasons.clear();
    }

    @Override
    public void resetAttemptCounter(int itemID) {
        fetchAttemptCount(itemID).set(0);
        fetchAttemptReason(itemID).clear();
    }

    /**
     * returns true if another attempt to fetch the item should be made.
     * @param itemID
     * @return
     */
    private boolean checkFailureCountExceeded(int itemID) {
        int maxFailures = options.getAttemptCount();
        if (maxFailures <= 0) {
            return true;
        }
        return fetchAttemptCount(itemID).get() < maxFailures;
    }

    private AtomicInteger fetchAttemptCount(int itemID) {
        if (!failedFetchAttempts.containsKey(itemID)) {
            failedFetchAttempts.put(itemID, new AtomicInteger(0));
        }
        return failedFetchAttempts.get(itemID);
    }

	protected void addFailureCount(int itemID) {
		int i = fetchAttemptCount(itemID).incrementAndGet();
		if (i >= options.getAttemptCount()) {
			notifyFailedFetch(itemID, this);
		}
	}

	protected void notifyFailedFetch(int itemID, Pricing pricing) {
        if (logger.isDebugEnabled()) logger.debug("notifying " + pricingListeners.size() + " listeners. [" + itemID + "]");
        for (int i = pricingListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingListener> wpl = pricingListeners.get(i);
            PricingListener pl = wpl.get();
            if (pl == null) {
                pricingListeners.remove(i);
                continue;
            } else {
                pl.priceUpdateFailed(itemID, pricing);
            }
        }
	}

    private List<String> fetchAttemptReason(int itemID) {
        if (!failedFetchReasons.containsKey(itemID)) {
            failedFetchReasons.put(itemID, new ArrayList<String>());
        }
        return failedFetchReasons.get(itemID);
    }

    protected void addFetchFailureReason(int itemID, String reason) {
        fetchAttemptReason(itemID).add(reason);
    }

    protected void addFailureReasons(Collection<Integer> itemIDs, String reason) {
        for (Integer i : itemIDs) {
            addFetchFailureReason(i, reason);
        }
    }

    @Override
    public int getFailedAttempts(int itemID) {
        return fetchAttemptCount(itemID).get();
    }

    @Override
    public List<String> getFetchErrors(int typeID) {
        // TODO This needs to have some concrete implementation
        return Collections.unmodifiableList(fetchAttemptReason(typeID));
    }
}
