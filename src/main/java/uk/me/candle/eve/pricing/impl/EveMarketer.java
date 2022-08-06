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


public class EveMarketer extends AbstractPricing {

    private static final Logger LOG = LoggerFactory.getLogger(EveMarketer.class);

    public EveMarketer() {
        super(2);
    }

    @Override
    public PricingFetch getPricingFetchImplementation() {
        return PricingFetch.EVEMARKETER;
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
        types.add(LocationType.SYSTEM);
        return types;
    }

    @Override
    public List<Long> getSupportedLocations(LocationType locationType) {
        if (getSupportedLocationTypes().contains(locationType)) {
            return new ArrayList<>();
        }
        return null;
    }

    @Override
    protected int getBatchSize() {
        return 200;
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> typeIDs) {
        //Validate
        Map<Integer, PriceContainer> returnMap = new HashMap<>();
        if (typeIDs.isEmpty()) {
            return returnMap;
        }
        if (getPricingOptions().getLocation() == null) {
            throw new UnsupportedOperationException("A location is required for EveMarketer");
        }
        LocationType locationType = getPricingOptions().getLocationType();
        if (!getSupportedLocationTypes().contains(locationType)) {
            throw new UnsupportedOperationException(locationType + " is not supported by EveMarketer");
        }
        //Update
        try {
            List<Type> results = getGSON().fromJson(getCall(typeIDs).execute().body().string(), new TypeToken<List<Type>>(){}.getType());
            if (results == null) {
                LOG.error("Error fetching price", new Exception("results is null"));
                addFailureReasons(typeIDs, "results is null");
                return returnMap;
            }
            //Updated OK
            for (Type item : results) {
                returnMap.put(item.getTypeID(), item.getPriceContainer());
            }
            if (typeIDs.size() != returnMap.size()) {
                List<Integer> errors = new ArrayList<>(typeIDs);
                errors.removeAll(returnMap.keySet());
                PriceContainer container = new PriceContainer.PriceContainerBuilder().build();
                for (Integer typeID : errors) {
                    returnMap.put(typeID, container);
                }
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

    protected String getURL(final Collection<Integer> typeIDs) {
        StringBuilder query = new StringBuilder();
        //TypeIDs
        query.append("&typeid=");
        boolean comma = false;
        for (Integer i : typeIDs) {
            if (comma) {
                query.append(',');
            } else {
                comma = true;
            }
            query.append(i);
        }
        //Location
        LocationType locationType = getPricingOptions().getLocationType();
        if (locationType == LocationType.STATION) {
            throw new UnsupportedOperationException(locationType + " is not supported by EveMarketer");
        } else if (locationType == LocationType.SYSTEM) { //Not empty
            query.append("&usesystem=");
            query.append(getPricingOptions().getLocationID());
        } else if (locationType == LocationType.REGION) {
            query.append("&regionlimit=");
            query.append(getPricingOptions().getLocationID());
        } else {
            throw new UnsupportedOperationException(locationType.name() + " is not supported by EveMarketer");
        }
        //Max order age
        return "https://api.evemarketer.com/ec/marketstat/json?" + query.toString();
    }

    private static class Type {
        TypeStat buy;
        TypeStat sell;
        public int getTypeID() {
            return buy.forQuery.types.get(0);
        }
        public PriceContainer getPriceContainer() {
            PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
            builder.putPrice(PriceType.BUY_MEAN, buy.avg);
            builder.putPrice(PriceType.BUY_MEDIAN, buy.median);
            builder.putPrice(PriceType.BUY_PERCENTILE, buy.fivePercent);
            builder.putPrice(PriceType.BUY_HIGH, buy.max);
            builder.putPrice(PriceType.BUY_LOW, buy.min);

            builder.putPrice(PriceType.SELL_MEAN, sell.avg);
            builder.putPrice(PriceType.SELL_MEDIAN, sell.median);
            builder.putPrice(PriceType.SELL_PERCENTILE, sell.fivePercent);
            builder.putPrice(PriceType.SELL_HIGH, sell.max);
            builder.putPrice(PriceType.SELL_LOW, sell.min);
            return builder.build();
        }
    }


    private static class TypeStat {
        ForQuery forQuery;
        double max;
        double median;
        long generated;
        double variance;
        double min;
        double avg;
        double stdDev;
        double fivePercent;
        boolean highToLow;
        long volume;
        double wavg;
    }

    private static class ForQuery {
        boolean bid;
        List<Integer> types = new ArrayList<>();
        List<Integer> regions = new ArrayList<>();
        List<Integer> systems = new ArrayList<>();
        Integer hours;
        Integer minq;
    }
}
