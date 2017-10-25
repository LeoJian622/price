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
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.me.candle.eve.pricing.AbstractPricingFast;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

/**
 *
 * @author Candle
 */
public class EveMarketData extends AbstractPricingFast {

    public EveMarketData(int threads) {
        super(threads);
    }

    @Override
    protected Map<Integer, PriceContainer> extractPrices(Element element) {
        Map<Integer, PriceContainer.PriceContainerBuilder> builders = new HashMap<Integer, PriceContainer.PriceContainerBuilder>();
        NodeList rows = element.getElementsByTagName("row");
        for (int i = 0; i < rows.getLength(); i++) { //Read prices from XML
            Element row = (Element) rows.item(i);
            Node buysellNode = row.getAttributes().getNamedItem("buysell");
            Node priceNode = row.getAttributes().getNamedItem("price");
            Node typeIdNode = row.getAttributes().getNamedItem("typeID");

            if (buysellNode != null && priceNode != null && typeIdNode != null) {
                String buysell = buysellNode.getNodeValue();
                Double price;
                Integer typeID;
                try {
                    price = Double.valueOf(priceNode.getNodeValue());
                    typeID = Integer.valueOf(typeIdNode.getNodeValue()) ;
                } catch (NumberFormatException ex) {
                    price = null;
                    typeID = null;
                }
                if (buysell != null && price != null && typeID != null) {
                    PriceContainer.PriceContainerBuilder builder = builders.get(typeID);
                    if (builder == null) {
                        builder = new PriceContainer.PriceContainerBuilder();
                        builders.put(typeID, builder);
                    }
                    PricingNumber number;
                    if (buysell.toLowerCase().equals("s")) {
                        number = PricingNumber.SELL;
                    } else {
                        number = PricingNumber.BUY;
                    }
                    for (PricingType type : PricingType.values()) {
                        builder.putPrice(type, number, price);
                    }

                }
            }
        }
        //Build prices
        Map<Integer, PriceContainer> prices = new HashMap<Integer, PriceContainer>();
        for (Map.Entry<Integer, PriceContainer.PriceContainerBuilder> entry : builders.entrySet()) {
            PriceContainer container = entry.getValue().build();
            if (container != null) {
                prices.put(entry.getKey(), container);
            }
        }
        return prices;
    }

    @Override
    protected int getBatchSize() {
        return 750;
    }

    @Override
    protected URL getURL(Collection<Integer> itemIDs) throws SocketTimeoutException, IOException {
        StringBuilder query = new StringBuilder();

        query.append("char_name=Golden%20Gnu&buysell=a");
        if (getPricingOptions().getLocationType() == LocationType.STATION //Station
            && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            if (query.length() > 0) query.append('&');
            query.append("station_ids=");
            query.append(getPricingOptions().getLocations().get(0));
        } else if (getPricingOptions().getLocationType() == LocationType.SYSTEM //System
            && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            if (query.length() > 0) query.append('&');
            query.append("solarsystem_ids=");
            query.append(getPricingOptions().getLocations().get(0));
        } else if (getPricingOptions().getLocationType() == LocationType.REGION
                && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            if (query.length() > 0) query.append('&');
            query.append("region_ids=");
            query.append(getPricingOptions().getLocations().get(0));
        }

        query.append("&type_ids=");
        boolean comma = false;
        for (Integer i : itemIDs) {
            if (comma) query.append(',');
            query.append(i);
            comma = true;
        }

        return new URL("https://api.eve-marketdata.com/api/item_prices2.xml?" + query.toString());
    }
}
