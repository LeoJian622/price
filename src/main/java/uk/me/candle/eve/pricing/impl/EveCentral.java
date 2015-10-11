package uk.me.candle.eve.pricing.impl;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collection;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import uk.me.candle.eve.pricing.AbstractPricingEasy;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class EveCentral extends AbstractPricingEasy {
   
    @Override
    protected Node getNode(Document d, int typeID, PricingType type, PricingNumber number) {
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
        case PERCENTILE:
            xPath.append("percentile");
            break;
        default:
            throw new UnsupportedOperationException("Unable to use the 'type': " + type);
        }

        return d.selectSingleNode(xPath.toString());
    }

    @Override
    protected int getBatchSize() {
        return 99;
    }

    @Override
    protected URL getURL(Collection<Integer> itemIDs) throws SocketTimeoutException, DocumentException, IOException {
        StringBuilder query = new StringBuilder();

        //TypeIDs
        for (Integer i : itemIDs) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append("typeid=");
            query.append(i);
        }
        //Location
        if (getPricingOptions().getLocationType() == LocationType.STATION) {
            throw new UnsupportedOperationException(LocationType.STATION.name() + " is not supported by EveCentral");
        } else if (getPricingOptions().getLocationType() == LocationType.SYSTEM
                   && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            query.append("&usesystem=");
            query.append(getPricingOptions().getLocations().get(0));

        } else if (getPricingOptions().getLocationType() == LocationType.REGION) {
            for (Long l : getPricingOptions().getLocations()) { //Region(s)
                query.append("&regionlimit=");
                query.append(l);
            }
        }
        //Max order age
        query.append("&hours=96");

        return new URL("http://api.eve-central.com/api/marketstat?" + query.toString());
    }
}
