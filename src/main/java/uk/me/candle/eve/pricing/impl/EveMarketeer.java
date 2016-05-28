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

/**
 *
 * @author Candle
 */
public class EveMarketeer extends AbstractPricingEasy {

    @Override
    protected Node getNode(Document d, int typeID, PricingType type, PricingNumber number) {
        StringBuilder xPath = new StringBuilder("/result/row[type_id=");
        xPath.append(typeID);
        xPath.append("]/");
        switch (number) {
        case BUY:
            xPath.append("buy_");
            break;
        case SELL:
            xPath.append("sell_");
            break;
        default:
            throw new UnsupportedOperationException("Unable to use the 'number': " + number);
        }
        switch (type) {
        case HIGH:
            xPath.append("highest");
            break;
        case LOW:
            xPath.append("lowest");
            break;
        case MEAN:
            xPath.append("avg");
            break;
        case MEDIAN:
            xPath.append("geo_mean"); //harm_mean
            break;
        case PERCENTILE:
            if (number == PricingNumber.BUY){
                xPath.append("highest5");
            } else {
                xPath.append("lowest5");
            }
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

        //Request Type
        if (getPricingOptions().getLocationType() == LocationType.STATION){
            query.append("station_info/");
        } else if (getPricingOptions().getLocationType() == LocationType.SYSTEM){
            query.append("system_info/");
        } else if (getPricingOptions().getLocationType() == LocationType.REGION){
            query.append("info/");
        }

        //TypeIDs
        boolean space = false;
        for (Integer i : itemIDs) {
            if (space){
                query.append('_');
            } else {
                space = true;
            }
            query.append(i);
        }
        query.append("/");

        query.append("xml/");

        //LocationIDs
        if (!getPricingOptions().getLocations().isEmpty()){ //We only want one location at the time
            query.append(getPricingOptions().getLocations().get(0));
        }

        return new URL("http://www.evemarketeer.com/api/" + query.toString());
    }
}
