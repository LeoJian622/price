package uk.me.candle.eve.pricing.impl;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import uk.me.candle.eve.pricing.AbstractPricing;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.PricingException;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class EveMarketData extends AbstractPricing {
    private static final Logger logger = Logger.getLogger(EveMarketData.class);
    int batchSize = 10;
    int failureCount = 0;

    @Override
    protected PriceContainer fetchPrice(int itemID) {
        return fetchPrices(Collections.singletonList(itemID)).get(itemID);
    }

    private PriceContainer extractPrice(Document d, Integer i) {
        PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
        for (PricingType type : PricingType.values()) {
            for (PricingNumber number : PricingNumber.values()) {
                builder.putPrice(type, number, extractPrice(d, i, type, number));
            }
        }
        return builder.build();
    }
    
    private double extractPrice(Document d, int typeID, PricingType type, PricingNumber number) {
        StringBuilder xPathBuilder = new StringBuilder();
        xPathBuilder.append("/eve/price[@id=\"");
        xPathBuilder.append(typeID);
        xPathBuilder.append("\"]");
        String xpath = xPathBuilder.toString();
        Node sn = d.selectSingleNode(xpath);
        if (sn != null) {
            return Double.parseDouble(sn.getStringValue());
        } else {
            return 0;
        }
    }

    @Override
    protected int getbatchSize() {
        return batchSize;
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> itemIDs) {
        Map<Integer, PriceContainer> returnMap = new HashMap<Integer, PriceContainer>();
        URL url = null;
        try {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://www.eve-marketdata.com/api/item_prices.xml?type_ids=");
            boolean comma = false;
            for (int i : itemIDs) {
                if (comma) urlBuilder.append(',');
                urlBuilder.append(i);
                comma = true;
            }
            url = new URL(urlBuilder.toString());

            Document d = getDocument(url);

            for (Integer i : itemIDs) {
                PriceContainer priceContainer = extractPrice(d, i);
                returnMap.put(i, priceContainer);
            }
            if (batchSize < 10) batchSize++; // success, try bigger next time.
        } catch (SocketTimeoutException ste) {
            logger.error("Timeout while fetching URL:" + url, ste);
            failureCount++;
            addFailureReasons(itemIDs, ste.getMessage());
        } catch (DocumentException de) {
            logger.error("Error fetching price", de);
            failureCount++;
            addFailureReasons(itemIDs, de.getMessage());
        } catch (IOException ioe) {
            logger.error("Error fetching price", ioe);
            failureCount++;
            addFailureReasons(itemIDs, ioe.getMessage());
        }
        // make the batch size smaller if we continue to fail.
        if (failureCount > 0 && failureCount%3 == 0) {
            if (logger.isDebugEnabled()) logger.debug("Reducing the batch size from " + getbatchSize());
            batchSize--;
        }
        if (batchSize == 0) {
            batchSize = 10;
            throw new PricingException("failed to fetch prices, several times, aborting.");
        }
        return returnMap;
    }
}
