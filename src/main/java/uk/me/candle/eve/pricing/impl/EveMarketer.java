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


public class EveMarketer extends AbstractPricingFast {

    public EveMarketer(int threads) {
        super(threads);
    }

    @Override
    protected Map<Integer, PriceContainer> extractPrices(Element element) {
        Map<Integer, PriceContainer.PriceContainerBuilder> builders = new HashMap<Integer, PriceContainer.PriceContainerBuilder>();
        NodeList types = element.getElementsByTagName("type");
        for (int i = 0; i < types.getLength(); i++) { //Read prices from XML
            Element type = (Element) types.item(i);
            if (type == null) {
                continue;
            }
            Node typeIdNode = type.getAttributes().getNamedItem("id");
            if (typeIdNode == null) {
                continue;
            }
            Integer typeID = Integer.valueOf(typeIdNode.getNodeValue()) ;
            if (typeID == null) {
                continue;
            }
            PriceContainer.PriceContainerBuilder builder = builders.get(typeID);
            if (builder == null) {
                builder = new PriceContainer.PriceContainerBuilder();
                builders.put(typeID, builder);
            }
            Element buy  = getElementByTagName(type, "buy");
            add(buy, "avg", builder, PricingType.MEAN, PricingNumber.BUY);
            add(buy, "median", builder, PricingType.MEDIAN, PricingNumber.BUY);
            add(buy, "percentile", builder, PricingType.PERCENTILE, PricingNumber.BUY);
            add(buy, "max", builder, PricingType.HIGH, PricingNumber.BUY);
            add(buy, "min", builder, PricingType.LOW, PricingNumber.BUY);

            Element sell = getElementByTagName(type, "sell");
            add(sell, "avg", builder, PricingType.MEAN, PricingNumber.SELL);
            add(sell, "median", builder, PricingType.MEDIAN, PricingNumber.SELL);
            add(sell, "percentile", builder, PricingType.PERCENTILE, PricingNumber.SELL);
            add(sell, "max", builder, PricingType.HIGH, PricingNumber.SELL);
            add(sell, "min", builder, PricingType.LOW, PricingNumber.SELL);
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
        return 200;
    }

    @Override
    protected URL getURL(final Collection<Integer> itemIDs) throws SocketTimeoutException, IOException {
        StringBuilder query = new StringBuilder();
        //TypeIDs
        query.append("&typeid=");
        boolean comma = false;
        for (Integer i : itemIDs) {
            if (comma) {
                query.append(',');;
            } else {
                comma = true;
            }
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
        return new URL("https://api.evemarketer.com/ec/marketstat?" + query.toString());
    }

    private Element getElementByTagName(Element parent, String name) {
        if (parent == null || name == null) {
            return null;
        }
        NodeList nodeList = parent.getElementsByTagName(name);
        if (nodeList.getLength() == 1) {
            return (Element) nodeList.item(0);
        } else {
            return null;
        }
    }

    private void add(Element parent, String name, PriceContainer.PriceContainerBuilder builder, PricingType type, PricingNumber number) {
        if (parent == null || name == null || builder == null) {
            return;
        }
        NodeList nodeList = parent.getElementsByTagName(name);
        if (nodeList == null) {
            return;
        }
        if (nodeList.getLength() != 1) {
            return;
        }
        Element element = (Element) nodeList.item(0);
        if (element == null) {
            return;
        }
        String textContent = element.getTextContent();
        if (textContent == null) {
            return;
        }
        double price;
        try {
            price = Double.valueOf(textContent);
        } catch (NumberFormatException ex) {
            return;
        }
        builder.putPrice(type, number, price);
    }
}
