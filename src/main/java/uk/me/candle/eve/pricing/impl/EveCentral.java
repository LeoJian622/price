package uk.me.candle.eve.pricing.impl;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.io.IOException;
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
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class EveCentral extends AbstractPricing {
    private static final Logger logger = Logger.getLogger(EveCentral.class);

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
        StringBuilder xPath = new StringBuilder("/evec_api/marketstat/type[@id=\"");
        xPath.append(typeID);
        xPath.append("\"]/");
        switch (number) {
        case BUY:
            xPath.append("buy/");
            break;
        case SELL:
            xPath.append("sell/");
            break;
        default:
            throw new UnsupportedOperationException("Unable to use the 'number': " + number);
        }
        switch (type) {
        case HIGH:
            xPath.append("max");
            break;
        case LOW:
            xPath.append("min");
            break;
        case MEAN:
            xPath.append("avg");
            break;
        case MEDIAN:
            xPath.append("median");
            break;
        default:
            throw new UnsupportedOperationException("Unable to use the 'type': " + type);
        }

        Node sn = d.selectSingleNode(xPath.toString());
        if (sn != null) {
            return Double.parseDouble(sn.getStringValue());
        } else {
            return 0;
        }
    }

    @Override
    protected int getbatchSize() {
        return 50;
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> itemIDs) {
        Map<Integer, PriceContainer> returnMap = new HashMap<Integer, PriceContainer>();
        try {
            StringBuilder query = new StringBuilder();
            for (Integer i : itemIDs) {
                if (query.length() > 0) query.append('&');
                query.append("typeid=");
                query.append(i);
            }
            for (Long l : getPricingOptions().getRegions()) {
                if (query.length() > 0) query.append('&');
                query.append("regionlimit=");
                query.append(l);
            }
            URL pricesURL = new URL("http://api.eve-central.com/api/marketstat?" + query.toString());

            Document d = getDocument(pricesURL);

            for (Integer i : itemIDs) {
                PriceContainer price = extractPrice(d, i);
                returnMap.put(i, price);
            }
        } catch (DocumentException de) {
            logger.error("Error fetching price", de);
            addFailureReasons(itemIDs, de.getMessage());
        } catch (IOException ioe) {
            logger.error("Error fetching price", ioe);
            addFailureReasons(itemIDs, ioe.getMessage());
        }

        return returnMap;
    }
}
