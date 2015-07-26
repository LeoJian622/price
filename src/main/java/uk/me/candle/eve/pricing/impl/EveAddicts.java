package uk.me.candle.eve.pricing.impl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import uk.me.candle.eve.pricing.AbstractPricingEasy;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

/**
 *
 * @author Niklas
 */
public class EveAddicts  extends AbstractPricingEasy {
    private static final Logger logger = Logger.getLogger(EveAddicts.class);

    @Override
    protected Node getNode(Document d, int typeID, PricingType type, PricingNumber number) {
        StringBuilder xPath = new StringBuilder("/prices/typeID[@id='");
        xPath.append(typeID);
        xPath.append("']/");
        switch (number) {
        case BUY:
            if (type == PricingType.PERCENTILE) {
                xPath.append("five_percent_buy_price");
            } else {
                xPath.append("buy_price");
            }
            break;
        case SELL:
            if (type == PricingType.PERCENTILE){
                xPath.append("five_percent_sell_price");
            } else {
                xPath.append("sell_price");
            }
            break;
        default:
            throw new UnsupportedOperationException("Unable to use the 'number': " + number);
        }
        return d.selectSingleNode(xPath.toString());
    }

    @Override
    public int getBatchSize() {
        return 99;
    }

    @Override
    protected URL getURL(Collection<Integer> itemIDs) throws SocketTimeoutException, DocumentException, IOException {
        StringBuilder query = new StringBuilder();

        query.append("detailed=true");

        //TypeIDs
        if (!itemIDs.isEmpty()){
            if (query.length() > 0) query.append('&');
            query.append("typeID=");
        }
        boolean comma = false;
        for (Integer i : itemIDs) {
            if (comma) {
                query.append(",");
            } else {
                comma = true;
            }
            query.append(i);
        }

        if (getPricingOptions().getLocationType() == LocationType.STATION
                && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            if (query.length() > 0) query.append('&');
            query.append("stationID=");
            query.append(getPricingOptions().getLocations().get(0));
        } else if (getPricingOptions().getLocationType() == LocationType.SYSTEM) { //System
            throw new UnsupportedOperationException(LocationType.SYSTEM.name() + " is not supported by EveAddicts");
        } else if (getPricingOptions().getLocationType() == LocationType.REGION
                && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            if (query.length() > 0) query.append('&');
            query.append("regionID=");
            query.append(getPricingOptions().getLocations().get(0));
        }

        return new URL("http://eve.addicts.nl/api/prices.php?" + query.toString());
    }
}
