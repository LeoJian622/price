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

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.me.candle.eve.pricing.AbstractPricing;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingFetch;


public class Fuzzwork extends AbstractPricing {

    private static final Logger LOG = LoggerFactory.getLogger(Fuzzwork.class);

    public Fuzzwork() {
        super(2);
    }

    @Override
    public PricingFetch getPricingFetchImplementation() {
        return PricingFetch.FUZZWORK;
    }

    @Override
    protected int getBatchSize() {
        return 1000;
    }

    @Override
    public List<PriceType> getSupportedPricingTypes() {
        List<PriceType> types = new ArrayList<>();
        types.add(PriceType.BUY_MEAN);
        types.add(PriceType.BUY_MEDIAN);
        types.add(PriceType.BUY_PERCENTILE);
        types.add(PriceType.BUY_HIGH);
        types.add(PriceType.BUY_LOW);
        types.add(PriceType.SELL_MEAN);
        types.add(PriceType.SELL_MEDIAN);
        types.add(PriceType.SELL_PERCENTILE);
        types.add(PriceType.SELL_HIGH);
        types.add(PriceType.SELL_LOW);
        return types;
    }

    @Override
    public List<LocationType> getSupportedLocationTypes() {
        List<LocationType> types = new ArrayList<>();
        types.add(LocationType.REGION);
        types.add(LocationType.STATION);
        return types;
    }

    @Override
    public List<Long> getSupportedLocations(LocationType locationType) {
        if (getSupportedLocationTypes().contains(locationType)) {
            if (locationType == LocationType.STATION) {
                List<Long> list = new ArrayList<>();
                list.add(60003760L); //Jita 4-4 CNAP
                list.add(60008494L); //Amarr VIII
                list.add(60011866L); //Dodixie
                list.add(60004588L); //Rens
                list.add(60005686L); //Hek
            } else {
                return new ArrayList<>();
            }
        }
        return null;
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> typeIDs) {
        //Validate
        Map<Integer, PriceContainer> returnMap = new HashMap<>();
        if (typeIDs.isEmpty()) {
            return returnMap;
        }
        if (getPricingOptions().getLocation() == null) {
            throw new UnsupportedOperationException("A location is required for Fuzzwork");
        }
        LocationType locationType = getPricingOptions().getLocationType();
        if (!getSupportedLocationTypes().contains(locationType)) {
            throw new UnsupportedOperationException(locationType + " is not supported by Fuzzwork");
        }
        if (locationType == LocationType.STATION) { //Station
            long locationID = getPricingOptions().getLocationID();
            if (   locationID != 60003760 //Jita 4-4 CNAP
                && locationID != 60008494 //Amarr VIII
                && locationID != 60011866 //Dodixie
                && locationID != 60004588 //Rens
                && locationID != 60005686 //Hek
                ) {
                throw new UnsupportedOperationException(locationID + " is not supported by Fuzzwork");
            }
        }
        //Update
        try {
            Map<Integer, FuzzworkPrice> results = getGSON().fromJson(getCall(typeIDs).execute().body().string(), new TypeToken<Map<Integer, FuzzworkPrice>>() {}.getType());
            if (results == null) {
                LOG.error("Error fetching price", new Exception("results is null"));
                addFailureReasons(typeIDs, "results is null");
                return returnMap;
            }
            //Updated OK
            for (Map.Entry<Integer, FuzzworkPrice> entry : results.entrySet()) {
                returnMap.put(entry.getKey(), entry.getValue().getPriceContainer());
            }
        } catch (IllegalArgumentException | IOException | JsonParseException ex) {
            LOG.error("Error fetching price", ex);
            addFailureReasons(typeIDs, ex.getMessage());
        }
        return returnMap;
    }

    public Call getCall(Collection<Integer> typeIDs) {
        Request.Builder request = new Request.Builder()
                .url(getURL(typeIDs))
                .addHeader("User-Agent", getPricingOptions().getUserAgent());
        //Headers
        for (Map.Entry<String, String> entry : getPricingOptions().getHeaders().entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        return getClient().newCall(request.build());
    }

    protected String getURL(Collection<Integer> itemIDs) {
        StringBuilder query = new StringBuilder();
        LocationType locationType = getPricingOptions().getLocationType();
        if (locationType == LocationType.STATION) { //Station
            long locationID = getPricingOptions().getLocationID();
            if (   locationID != 60003760 //Jita 4-4 CNAP
                && locationID != 60008494 //Amarr VIII
                && locationID != 60011866 //Dodixie
                && locationID != 60004588 //Rens
                && locationID != 60005686 //Hek
                ) {
                throw new UnsupportedOperationException(locationID + " is not supported by Fuzzwork");
            }
            query.append("station=");
            query.append(getPricingOptions().getLocationID());
        } else if (locationType == LocationType.REGION) { //Region
            query.append("region=");
            query.append(getPricingOptions().getLocationID());
        } else { //System/etc.
            throw new UnsupportedOperationException(locationType.name() + " is not supported by Fuzzwork");
        }
        query.append("&types=");
        boolean comma = false;
        for (Integer i : itemIDs) {
            if (comma) query.append(',');
            query.append(i);
            comma = true;
        }
        return "https://market.fuzzwork.co.uk/aggregates/?" + query.toString();
    }

    private static class FuzzworkPrice {
        public FuzzworkPriceData buy;
        public FuzzworkPriceData sell;

        PriceContainer getPriceContainer() {
            PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
            builder.putPrice(PriceType.BUY_HIGH, buy.max);
            builder.putPrice(PriceType.BUY_LOW, buy.min);
            builder.putPrice(PriceType.BUY_MEAN, buy.weightedAverage);
            builder.putPrice(PriceType.BUY_MEDIAN, buy.median);
            builder.putPrice(PriceType.BUY_PERCENTILE, buy.percentile);
            builder.putPrice(PriceType.SELL_HIGH, sell.max);
            builder.putPrice(PriceType.SELL_LOW, sell.min);
            builder.putPrice(PriceType.SELL_MEAN, sell.weightedAverage);
            builder.putPrice(PriceType.SELL_MEDIAN, sell.median);
            builder.putPrice(PriceType.SELL_PERCENTILE, sell.percentile);
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
