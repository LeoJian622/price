/*
 * Copyright 2015-2016, Niklas Kyster Rasmussen, Flaming Candle
 *
 * This file is part of Price
 *
 * Price is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * Price is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Price; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package uk.me.candle.eve.pricing;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.Proxy;
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
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingOptions;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;
import uk.me.candle.eve.pricing.util.SplitList;

/**
 *
 * @author Candle
 */
public abstract class AbstractPricing implements Pricing {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPricing.class);
    // <editor-fold defaultstate="collapsed" desc="fields">
    /**
     * internal mem-cache of prices.
     */
    private final Map<Integer, CachedPrice> cache = Collections.synchronizedMap(new HashMap<>());;
    /**
     * list of listeners to notify when a price has been fetched.
     */
    private final List<WeakReference<PricingListener>> pricingListeners = new ArrayList<>();
    /**
     * list of listeners to notify when a fetch is starting and finishing.
     */
    private final List<WeakReference<PricingFetchListener>> pricingFetchListeners = new ArrayList<>();
    /**
     * queue of typeIDs that are waiting to be fetched
     */
    private final Queue<Integer> waitingQueue = new ConcurrentLinkedQueue<>();
    /**
     * queue of typeIDs that failed on last fetched
     */
    /**
     * list of item IDs that are being fetched - this is here so that we don't queue an ID that is in the process of
     * being fetched.
     */
    private final List<Integer> currentlyEvaluating = Collections.synchronizedList(new ArrayList<>());
    /**
     * single thread that handles the price fetching.
     */
    private final List<PriceFetchingThread> priceFetchingThreads = Collections.synchronizedList(new ArrayList<>());

    /**
     * maintains the list of failed attempts to fetch prices.
     */
    private final Map<Integer, AtomicInteger> failedFetchAttempts = Collections.synchronizedMap(new HashMap<>());

    /**
     * map of reasons why fetching a price has failed.
     */
    private final Map<Integer, List<String>> failedFetchReasons = Collections.synchronizedMap(new HashMap<>());

    /**
     * GSON Object
     */
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * HttpLoggingInterceptor
     */
    private static final HttpLoggingInterceptor HTTP_LOGGING_INTERCEPTOR = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                                    @Override
                                    public void log(String string) {
                                        LOG.debug(string);
                                    }
                                });
    /**
     * HTTP Client
     */
    private final OkHttpClient client;
    /**
     * the length of time to keep prices in the cache.
     */
    long cacheTimer = 60*60*1000l;

    /**
     * defines some options.
     */
    PricingOptions options = new DefaultPricingOptions();
    // </editor-fold>

    public AbstractPricing(int threads) {
        if (LOG.isDebugEnabled()) {
            client = new OkHttpClient().newBuilder()
                .addNetworkInterceptor(HTTP_LOGGING_INTERCEPTOR)
                .build();
            HTTP_LOGGING_INTERCEPTOR.setLevel(HttpLoggingInterceptor.Level.BASIC);
        } else {
             client = new OkHttpClient().newBuilder().build();
        }
        for (int i = 1; i <= threads; i++) {
            PriceFetchingThread priceFetchingThread = new PriceFetchingThread(i);
            priceFetchingThreads.add(priceFetchingThread);
            priceFetchingThread.start();
        }
    }

    public Gson getGSON() {
        return GSON;
    }

    public OkHttpClient getClient() {
        return client;
    }

    @Override
    public Double getPriceCache(int typeID, PriceType type) {
        CachedPrice cp = cache.get(typeID);
        if (cp != null) {
            return cp.getContainer().getPrice(type);
        } else {
            return null;
        }
    }

    @Override
    public Double getPrice(int typeID, PriceType type) {
        CachedPrice cp = cache.get(typeID);
        // Queue the price for fetching, if: we do not have a cached price
        // If we have a cached price, then queue the fetch only if the
        // cache timers are enabled.
        boolean cacheTimout = cp != null && (cp.getTime()+cacheTimer) < System.currentTimeMillis();
        if (cp == null //DoesNotExists
                || (options.getCacheTimersEnabled() && cacheTimout)) {
            queuePrice(typeID);
            return null;
        } else {
            return cp.getContainer().getPrice(type);
        }
    }

    @Override
    public void updatePrices(Set<Integer> typeIDs) {
        queuePrices(typeIDs);
    }

    @Override
    public long getNextUpdateTime(int typeID) {
        CachedPrice cp = cache.get(typeID);
        if (cp == null) return -1; // needs an update.
        return cp.getTime();
    }

    protected Element getElement(URL url) throws SocketTimeoutException, IOException, ParserConfigurationException, SAXException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(getInputStream(url)).getDocumentElement();
    }

    protected InputStream getInputStream(URL url) throws SocketTimeoutException, IOException {
        if (LOG.isDebugEnabled()) LOG.debug("Fetching URL: " + url);
        Proxy proxy = options.getProxy();
        URLConnection urlCon;
        if (proxy != null) {
            urlCon = url.openConnection(proxy);
        } else {
            urlCon = url.openConnection();
        }
        urlCon.setReadTimeout(options.getTimeout());
        urlCon.setDoInput(true);
        urlCon.setRequestProperty("Accept-Encoding", "gzip");

        if ("gzip".equals(urlCon.getContentEncoding())) {
            return new GZIPInputStream(urlCon.getInputStream());
        } else {
            return urlCon.getInputStream();
        }
    }

    @Override
    public void setPricingOptions(PricingOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Options can not be null");
        }
        this.options = options;
        cacheTimer = options.getPriceCacheTimer();
        read();
    }

    /**
     * fetches the prices for the collection of typeIDs.
     * @param typeIDs
     * @return a map of typeID => price
     */
    abstract protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> typeIDs);

    /**
     * defines the batch size for fetching grouped prices.
     * less then or equal to 0 means that there is no limit.
     * @return
     */
    protected int getBatchSize() {
        return -1;
    }

    public long getCacheTimer() {
        return cacheTimer;
    }

    public void setCacheTimer(long cacheTimer) {
        this.cacheTimer = cacheTimer;
    }

    @Override
    public void clearPrice(int typeID) {
        long cacheTime = -1;
        CachedPrice current = cache.get(typeID);
        if (current != null) {
            cache.put(typeID, new CachedPrice(cacheTime, current.getContainer().createClone().build()));
        } else {
            cache.remove(typeID);
        }
        notifyPricingListeners(typeID);
    }

    @Override
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
            } else {
                if (pl.equals(o)) {
                    pricingListeners.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    private void notifyPricingListeners(Integer typeID) {
        LOG.debug("notifying " + pricingListeners.size() + " pricing listeners with " + typeID);
        for (int i = pricingListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingListener> wpl = pricingListeners.get(i);
            PricingListener pl = wpl.get();
            if (pl == null) {
                pricingListeners.remove(i);
            } else {
                pl.priceUpdated(typeID, this);
            }
        }
    }

    @Override
    public boolean addPricingListener(PricingListener pl) {
        return pricingListeners.add(new WeakReference<>(pl));
    }

    @Override
    public boolean addPricingFetchListener(PricingFetchListener pfl) {
        return pricingFetchListeners.add(new WeakReference<>(pfl));
    }

    @Override
    public boolean removePricingFetchListener(PricingFetchListener o) {
        for (int i = pricingFetchListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingFetchListener> wpl = pricingFetchListeners.get(i);
            PricingFetchListener pl = wpl.get();
            if (pl == null) {
                pricingFetchListeners.remove(i);
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
        LOG.debug("notifying " + pricingFetchListeners.size() + " fetch listeners. [" + started + "]");
        for (int i = pricingFetchListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingFetchListener> wpl = pricingFetchListeners.get(i);
            PricingFetchListener pl = wpl.get();
            if (pl == null) {
                pricingFetchListeners.remove(i);
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
        queuePrices(Collections.singleton(itemID));
    }

    private void queuePrices(Set<Integer> itemIDs) {
        for (int itemID : itemIDs) {
            if (!waitingQueue.contains(itemID) && !currentlyEvaluating.contains(itemID) && checkFailureCountExceeded(itemID)) {
                if (LOG.isTraceEnabled()) LOG.trace("queued " + itemID + " for price fetching");
                waitingQueue.add(itemID);
            }
        }
        for (PriceFetchingThread priceFetchingThread : priceFetchingThreads) {
            synchronized(priceFetchingThread) {
                priceFetchingThread.notifyAll();
            }
        }
    }

    /**
     * The thread that handles the price fetching.
     * It wait()s until there are items in the queue (queuePrice(...) notifies it) then removes items from the queue,
     * and fetches the price using the specific implementation
     */
    private class PriceFetchingThread extends Thread {
        private final Set<Integer> evaluate = new HashSet<>();
        private SplitList failed;

        public PriceFetchingThread(int id) {
            super("Price Fetching " + id);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (waitingQueue.isEmpty()) {
                        synchronized(this) {
                            if (LOG.isDebugEnabled()) LOG.debug("Pricing fetch thread is waiting.");
                            this.wait();
                        }
                    }
                } catch (InterruptedException ie) {
                    LOG.warn("Pricing fetch thread interrupted.");
                    // just continue.
                }
                notifyPricingFetchListeners(true);

                //Do a binary search for the failed IDs
                if (failed != null) {
                    List<Integer> nextList = failed.getNextList();
                    if (nextList != null) { //Next list
                        evaluate.addAll(nextList);
                    } else { //List is empty - do we need to do it again?
                        failed = null; //Search is done - We don't want to do it again!!! (you will get banned mate)
                    }
                }
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
                if (LOG.isDebugEnabled()) LOG.debug("There are " + waitingQueue.size() + " items in the queue.");
                while (failed == null && waitingQueue.size() > 0 && (!(getBatchSize() > 0) || evaluate.size() < getBatchSize())) {
                    try {
                        Integer num = waitingQueue.remove();
                        if (checkFailureCountExceeded(num)) {
                            evaluate.add(num);
                            currentlyEvaluating.add(num);
                        }
                    } catch (NoSuchElementException ex) {
                        //This is not a problem
                    }
                }
                if (LOG.isDebugEnabled()) LOG.debug("Starting price fetch for " + evaluate.size() + " items.");
                // handle the list.
                Map<Integer, PriceContainer> prices = fetchPrices(evaluate);
                // for each of the prices, cache, and notify listeners
                boolean doBinarySearch = false;
                for (Integer itemId : evaluate) {
                    if (prices.containsKey(itemId)) { //OK
                        PriceContainer priceContainer = prices.get(itemId);
                        cache.put(itemId, new CachedPrice(System.currentTimeMillis(), priceContainer));
                        notifyPricingListeners(itemId);
                    } else { //Fail
                        if (options.getUseBinaryErrorSearch()) {
                            if (evaluate.size() == 1) {
                                notifyFailedFetch(itemId);
                            } else {
                                //New Search
                                doBinarySearch = true;
                            }
                        } else {
                            addFailureCount(itemId);
                            waitingQueue.add(itemId); // add prices that were unable to be fetched back onto the queue.
                        }
                    }
                }
                if (doBinarySearch && failed == null) { //Start new search for the error
                    failed = new SplitList(evaluate);
                }
                if (!doBinarySearch && failed != null) { //OK
                    failed.removeLast();
                }
                synchronized (currentlyEvaluating) {
                    currentlyEvaluating.removeAll(evaluate);
                    currentlyEvaluating.notifyAll();
                }
                evaluate.clear();
                if (LOG.isDebugEnabled()) LOG.debug("finished fetch");
                notifyPricingFetchListeners(false);
            }
        }
    }

    @Override
    public void writeCache() throws IOException {
        write();
    }

    @SuppressWarnings("unchecked")
    private synchronized void read() { //Only read/write one at the time
        InputStream input = null;
        try {
            input = options.getCacheInputStream();
            if (input != null) {
                ObjectInputStream oos = new ObjectInputStream(input);
                cache.putAll((Map<Integer, CachedPrice>)oos.readObject()); //synchronized by map
            }
        } catch (IOException ioe) {
            LOG.error("Error reading the cache file (Other IO error)", ioe);
        } catch (ClassNotFoundException cnfe) {
            LOG.error("Error reading the cache file (incompatible classes)", cnfe);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {

                }
            }
        }
    }

    private synchronized void write() throws IOException { //Only read/write one at the time
        OutputStream output = null;
        try {
            output = options.getCacheOutputStream();
            if (output != null) {
                synchronized(cache) { //synchronize map access
                    ObjectOutputStream oos = new ObjectOutputStream(output);
                    oos.writeObject(cache);
                    oos.flush();
                }
            }
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {

                }
            }
        }
    }

    @Override
    public void cancelAll() {
        while (!currentlyEvaluating.isEmpty() || !waitingQueue.isEmpty()) { //While evaluating
            //Clear queue
            while(!waitingQueue.isEmpty()) {
                try {
                    notifyFailedFetch(waitingQueue.remove()); //Notify of failed price updates
                } catch (NoSuchElementException ex) {
                    //This is not a problem
                }
            }
            //Wait for evaluating to finish
            synchronized (currentlyEvaluating) {
                try {
                    if (!currentlyEvaluating.isEmpty()) { //If evaluating
                        currentlyEvaluating.wait(); //wait to finish
                    }
                } catch (InterruptedException ex) {
                   //Continue
                }
            }

        }
    }

    @Override
    public void cancelSingle(int itemID) {
        waitingQueue.remove(itemID);
        notifyFailedFetch(itemID);
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
            notifyFailedFetch(itemID);
        }
    }

    protected void notifyFailedFetch(int itemID) {
        if (LOG.isDebugEnabled()) LOG.debug("notifying " + pricingListeners.size() + " listeners. [" + itemID + "]");
        for (int i = pricingListeners.size()-1; i >= 0; --i) {
            WeakReference<PricingListener> wpl = pricingListeners.get(i);
            PricingListener pl = wpl.get();
            if (pl == null) {
                pricingListeners.remove(i);
            } else {
                pl.priceUpdateFailed(itemID, this);
            }
        }
    }

    private List<String> fetchAttemptReason(int itemID) {
        if (!failedFetchReasons.containsKey(itemID)) {
            failedFetchReasons.put(itemID, new ArrayList<>());
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
