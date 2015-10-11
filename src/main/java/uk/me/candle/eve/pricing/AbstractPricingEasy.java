/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.me.candle.eve.pricing;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

/**
 *
 * @author Niklas
 */
public abstract class AbstractPricingEasy extends AbstractPricing {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPricingEasy.class);

    private URL url;

    protected abstract URL getURL(Collection<Integer> itemIDs) throws 
            SocketTimeoutException, DocumentException, IOException;

    abstract protected Node getNode(Document d, int typeID, PricingType type, PricingNumber number);

    private PriceContainer extractPrice(Document d, Integer typeID) {
        if (d == null) {
            //throw new RuntimeException("Document == null >> typeID: " + typeID + "\n@" + url);
        }
        PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
        for (PricingType type : PricingType.values()) {
            for (PricingNumber number : PricingNumber.values()) {
                Node node = getNode(d, typeID, type, number);
                if (node == null) {
                    //throw new RuntimeException("Node == null >> typeID: " + typeID + " Type: " + type.name() + " Number:" + number + "\n@" + url);
                }
                Double price = getNodeValue(node);
                builder.putPrice(type, number, price);
            }
        }
        return builder.build();
    }

    private double getNodeValue(Node node) {
        if (node == null) {
            return 0.0;
        }
        try {
            return Double.valueOf(node.getStringValue()); //Sometimes return null
        } catch (NumberFormatException ex){
            return 0.0;
        }
    }

    @Override
    protected final PriceContainer fetchPrice(int itemID) {
        return fetchPrices(Collections.singletonList(itemID)).get(itemID);
    }

    @Override
    protected final Map<Integer, PriceContainer> fetchPrices(Collection<Integer> itemIDs) {
        LOG.info("getting " + itemIDs.size() + " prices");
        Map<Integer, PriceContainer> returnMap = new HashMap<Integer, PriceContainer>();
        //System.out.println("failed: " + failed.size());
        url = null;
        try {
            url = getURL(itemIDs);
            Document d = getDocument(url);
            for (Integer i : itemIDs) {
                PriceContainer price = extractPrice(d, i);
                returnMap.put(i, price);
            }
        } catch (SocketTimeoutException ste) {
            //Minor failure
            LOG.error("Timeout while fetching URL:" + url, ste);
            LOG.debug("Reducing the batch size by -1 from " + getBatchSize());
            addFailureReasons(itemIDs, ste.getMessage());
        } catch (DocumentException de) {
            //Critical failure
            LOG.error("Error fetching price", de);
            addFailureReasons(itemIDs, de.getMessage());
        } catch (IOException ioe) {
            //Critical failure
            LOG.error("Error fetching price", ioe);
            addFailureReasons(itemIDs, ioe.getMessage());
        }
        return returnMap;
    }
}
