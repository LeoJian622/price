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
public class EveMarketData extends AbstractPricingEasy {

    @Override
    protected Node getNode(Document d, int typeID, PricingType type, PricingNumber number) {
        StringBuilder xPath = new StringBuilder();
        xPath.append("/emd/result/rowset/row[@typeID='");
        xPath.append(typeID);
        xPath.append("'");
        switch (number) {
            case BUY:
                xPath.append(" and @buysell='b']");
                break;
            case SELL:
                xPath.append(" and @buysell='s']");
                break;
            default:
                throw new UnsupportedOperationException("Unable to use the 'number': " + number);
        }
        xPath.append("/@price");
		
        return d.selectSingleNode(xPath.toString());
    }

    @Override
    protected int getBatchSize() {
        return 99;
    }

    @Override
    protected URL getURL(Collection<Integer> itemIDs) throws SocketTimeoutException, DocumentException, IOException {
        StringBuilder query = new StringBuilder();

        query.append("&type_ids=");
        boolean comma = false;
        for (Integer i : itemIDs) {
            if (comma) query.append(',');
            query.append(i);
            comma = true;
        }

        if (getPricingOptions().getLocationType() == LocationType.STATION) {
            throw new UnsupportedOperationException(LocationType.STATION.name() + " is not supported by EveMarketData");

        } else if (getPricingOptions().getLocationType() == LocationType.SYSTEM) { //System
            throw new UnsupportedOperationException(LocationType.SYSTEM.name() + " is not supported by EveMarketData");

        } else if (getPricingOptions().getLocationType() == LocationType.REGION
                && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            if (query.length() > 0) query.append('&');
            query.append("region_ids=");
            query.append(getPricingOptions().getLocations().get(0));
        }

        return new URL("http://api.eve-marketdata.com/api/item_prices2.xml?char_name=Golden%20Gnu&buysell=a" + query.toString());
    }
}
