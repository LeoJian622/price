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
package uk.me.candle.eve.pricing.impl;

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


public class EveAddicts extends AbstractPricingEasy {

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
