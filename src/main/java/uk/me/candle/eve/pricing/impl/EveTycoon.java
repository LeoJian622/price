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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import okhttp3.Call;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.me.candle.eve.pricing.AbstractPricing;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingFetch;


public class EveTycoon extends AbstractPricing {

    private static final Logger LOG = LoggerFactory.getLogger(EveTycoon.class);

    private static final int STACK = 50;
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(STACK);

    public EveTycoon() {
        super(1);
    }

    @Override
    public PricingFetch getPricingFetchImplementation() {
        return PricingFetch.EVE_TYCOON;
    }

    @Override
    public List<PriceType> getSupportedPricingTypes() {
        List<PriceType> types = new ArrayList<>();
        types.add(PriceType.BUY_PERCENTILE);
        types.add(PriceType.BUY_HIGH);
        types.add(PriceType.SELL_PERCENTILE);
        types.add(PriceType.SELL_LOW);
        return types;
    }

    @Override
    public List<LocationType> getSupportedLocationTypes() {
        List<LocationType> types = new ArrayList<>();
        types.add(LocationType.REGION);
        types.add(LocationType.SYSTEM);
        types.add(LocationType.STATION);
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
        return STACK;
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> typeIDs) {
        //Validate
        Map<Integer, PriceContainer> returnMap = new HashMap<>();
        if (typeIDs.isEmpty()) {
            return returnMap;
        }
        if (getPricingOptions().getLocation() == null) {
            throw new UnsupportedOperationException("A location is required for EveTycoon");
        }
        LocationType locationType = getPricingOptions().getLocationType();
        if (!getSupportedLocationTypes().contains(locationType)) {
            throw new UnsupportedOperationException(locationType + " is not supported by EveTycoon");
        }
        //Update
		Map<Integer, Future<String>> futures = new HashMap<>();
		for (Integer typeID : typeIDs) {
			futures.put(typeID, THREAD_POOL.submit(new Updater(typeID)));
		}
        try {
            int done = 0;
            while (done < futures.size()) {
                done = 0;
                for (Map.Entry<Integer, Future<String>> entry : futures.entrySet()) {
                    Future<String> future = entry.getValue();
                    Integer typeID = entry.getKey();
                    if (future.isDone()) {
                        done++;
                        if (!returnMap.containsKey(typeID)) {
                            String body = future.get(); //Get data from ESI
                            if (body != null) {
                                try {
                                    TycoonPrice tycoonPrice = getGSON().fromJson(body, TycoonPrice.class);
                                    if (tycoonPrice != null) {
                                        returnMap.put(entry.getKey(), tycoonPrice.getPriceContainer());
                                    }
                                } catch (JsonParseException ex) {
                                    LOG.error(ex.getMessage(), ex);
                                }
                            }
                        }
                    }
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException ex) {
            LOG.error(ex.getMessage(), ex);
        } catch (ExecutionException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return returnMap;
    }

    private String getURL(Integer typeID) {
        if (getPricingOptions().getLocation() == null) {
            throw new UnsupportedOperationException("A location is required for EveTycoon");
        }
        StringBuilder query = new StringBuilder();
        query.append("https://evetycoon.com/api/v1/market/stats/");
        query.append(getPricingOptions().getRegionID());
        query.append("/");
        query.append(typeID);
        query.append("/");
        if (getPricingOptions().getLocationType() == LocationType.STATION) { //Station
            query.append("?locationId=");
            query.append(getPricingOptions().getLocationID());
        } else if (getPricingOptions().getLocationType() == LocationType.SYSTEM) { //System
            query.append("?systemId=");
            query.append(getPricingOptions().getLocationID());
        }
        return query.toString();
    }

    public Call getCall(Integer typeID) {
        Request.Builder request = new Request.Builder()
                .url(getURL(typeID))
                .addHeader("User-Agent", getPricingOptions().getUserAgent());
        //Headers
        for (Map.Entry<String, String> entry : getPricingOptions().getHeaders().entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        return getClient().newCall(request.build());
    }

    private class Updater implements Callable<String> {

        private final Call call;

        public Updater(Integer typeID) {
            call = getCall(typeID);
        }

        @Override
        public String call() throws Exception {
            return call.execute().body().string();
        }

    }

    private static class TycoonPrice {
        public int typeID;
        public long buyVolume;
        public long sellVolume;
        public long buyOrders;
        public long sellOrders;
        public long buyOutliers;
        public long sellOutliers;
        public double buyThreshold;
        public double sellThreshold;
        public double buyAvgFivePercent;
        public double sellAvgFivePercent;
        public double maxBuy;
        public double minSell;

        int getTypeID() {
            return typeID;
        }

        PriceContainer getPriceContainer() {
            PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
            builder.putPrice(PriceType.BUY_HIGH, maxBuy);
            builder.putPrice(PriceType.BUY_PERCENTILE, buyAvgFivePercent);
            builder.putPrice(PriceType.SELL_LOW, minSell);
            builder.putPrice(PriceType.SELL_PERCENTILE, sellAvgFivePercent);
            return builder.build();
        }
    }
}
