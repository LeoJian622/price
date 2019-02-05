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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import uk.me.candle.eve.pricing.AbstractPricing;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;


public class Fuzzwork extends AbstractPricing {

    public Fuzzwork(int threads) {
        super(threads);
    }

    @Override
    protected PriceContainer fetchPrice(int itemID) {
        return fetchPrices(Collections.singletonList(itemID)).get(itemID);
    }

    @Override
    protected int getBatchSize() {
        return 1000;
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> itemIDs) {
        Map<Integer, PriceContainer> returnMap = new HashMap<Integer, PriceContainer>();
        if (itemIDs.isEmpty()) {
            return returnMap;
        }
        try {
            ObjectMapper mapper = new ObjectMapper(); //create once, reuse
            URL url = getURL(itemIDs);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            Map<Integer, FuzzworkPrice> results = mapper.readValue(con.getInputStream(), new TypeReference<Map<Integer, FuzzworkPrice>>() {
            });
            if (results == null) {
                return returnMap;
            }
            //Updated OK
            for (Map.Entry<Integer, FuzzworkPrice> entry : results.entrySet()) {
                returnMap.put(entry.getKey(), entry.getValue().getPriceContainer());
            }
            return returnMap;
        } catch (IOException ex) {
            System.out.println("ex: " + ex.getMessage());
            ex.printStackTrace();
            return returnMap;
        }
    }

    protected URL getURL(Collection<Integer> itemIDs) throws MalformedURLException {
        StringBuilder query = new StringBuilder();
        if (getPricingOptions().getLocationType() == LocationType.STATION //Station
            && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            Long locationID = getPricingOptions().getLocations().get(0);
            if (   locationID != 60003760 //Jita 4-4 CNAP
                && locationID != 60008494 //Amarr VIII
                && locationID != 60011866 //Dodixie
                && locationID != 60004588 //Rens
                && locationID != 60005686 //Hek
                ) {
                throw new UnsupportedOperationException(locationID + " is not supported by Fuzzwork");
            }
            if (query.length() > 0) query.append('&');
            query.append("station=");
            query.append(getPricingOptions().getLocations().get(0));
        } else if (getPricingOptions().getLocationType() == LocationType.SYSTEM) { //System
            throw new UnsupportedOperationException(LocationType.STATION.name() + " is not supported by Fuzzwork");
        } else if (getPricingOptions().getLocationType() == LocationType.REGION
                && !getPricingOptions().getLocations().isEmpty()) { //Not empty
            if (query.length() > 0) query.append('&');
            query.append("region=");
            query.append(getPricingOptions().getLocations().get(0));
        }

        query.append("&types=");
        boolean comma = false;
        for (Integer i : itemIDs) {
            if (comma) query.append(',');
            query.append(i);
            comma = true;
        }

        return new URL("https://market.fuzzwork.co.uk/aggregates/?" + query.toString());
    }

    private static class FuzzworkPrice {
        public FuzzworkPriceData buy;
        public FuzzworkPriceData sell;

        PriceContainer getPriceContainer() {
            PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
            builder.putPrice(PricingType.HIGH, PricingNumber.BUY, buy.max);
            builder.putPrice(PricingType.LOW, PricingNumber.BUY, buy.min);
            builder.putPrice(PricingType.MEAN, PricingNumber.BUY, buy.weightedAverage);
            builder.putPrice(PricingType.MEDIAN, PricingNumber.BUY, buy.median);
            builder.putPrice(PricingType.PERCENTILE, PricingNumber.BUY, buy.percentile);
            builder.putPrice(PricingType.HIGH, PricingNumber.SELL, sell.max);
            builder.putPrice(PricingType.LOW, PricingNumber.SELL, sell.min);
            builder.putPrice(PricingType.MEAN, PricingNumber.SELL, sell.weightedAverage);
            builder.putPrice(PricingType.MEDIAN, PricingNumber.SELL, sell.median);
            builder.putPrice(PricingType.PERCENTILE, PricingNumber.SELL, sell.percentile);
            return builder.build();
        }
    }

    private static class FuzzworkPriceData {
        public double weightedAverage;
        public double max;
        public double min;
        public double stddev;
        public double median;
        public double volume;
        public double orderCount;
        public double percentile;
    }
    
}
