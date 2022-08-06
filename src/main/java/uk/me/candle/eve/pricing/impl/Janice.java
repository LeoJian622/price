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
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.me.candle.eve.pricing.AbstractPricing;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.NamedPriceLocation;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.NamedLocation;


public class Janice extends AbstractPricing {

    private static final Logger LOG = LoggerFactory.getLogger(Janice.class);

    public static enum JaniceLocation {
        JITA_4_4("Jita 4-4", 2),
        R1O_GN("R1O-GN", 3),
        PERIMETER_TTT("Perimeter TTT", 4),
        JITA_4_4_AND_PERIMETER_TTT("Jita 4-4 + Perimeter TTT", 5),
        NPC("NPC", 6),
        MJ_5F9("MJ-5F9", 114),
        AMARR("Amarr", 115)
        ;
        private final NamedPriceLocation priceLocation;

        private JaniceLocation(String name, Integer marketID) {
            priceLocation = new NamedLocation(name, marketID, marketID);
        }

        public NamedPriceLocation getPriceLocation() {
            return priceLocation;
        }

        public static NamedPriceLocation getLocation(long locationID) {
            for (JaniceLocation janiceLocation : JaniceLocation.values()) {
                if (locationID == janiceLocation.getPriceLocation().getLocationID()) {
                    return janiceLocation.getPriceLocation();
                }
            }
            return null;
        }
    }

    public Janice() {
        super(1);
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
        types.add(LocationType.STATION);
        return types;
    }

    @Override
    public List<Long> getSupportedLocations(LocationType locationType) {
        if (getSupportedLocationTypes().contains(locationType)) {
            ArrayList<Long> list = new ArrayList<>();
            list.add(2L); //Jita 4-4
            list.add(3L); //R1O-GN
            list.add(4L); //Perimeter TTT
            list.add(5L); //Jita 4-4 + Perimeter TTT
            list.add(6L); //NPC
            list.add(114L); //MJ-5F9
            list.add(115L); //Amarr
            return list;
        }
        return null;
    }

    @Override
    public PricingFetch getPricingFetchImplementation() {
        return PricingFetch.JANICE;
    }

    @Override
    protected int getBatchSize() {
        return 100;
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> typeIDs) {
        //Validate
        Map<Integer, PriceContainer> returnMap = new HashMap<>();
        if (typeIDs.isEmpty()) {
            return returnMap;
        }
        if (getPricingOptions().getLocation() == null) {
            throw new UnsupportedOperationException("A location is required for Janice");
        }
        LocationType locationType = getPricingOptions().getLocationType();
        if (!getSupportedLocationTypes().contains(locationType)) {
            throw new UnsupportedOperationException(locationType + " is not supported by Janice");
        }
        long locationID = getPricingOptions().getLocationID();
        if (locationID != 2 //Jita 4-4 == 60003760L
            && locationID != 3 //R1O-GN (Ignore?)
            && locationID != 4 //Perimeter TTT
            && locationID != 5 //Jita 4-4 + Perimeter TTT
            && locationID != 6 //NPC
            && locationID != 114 //MJ-5F9  (Ignore?)
            && locationID != 115 //Amarr
                ) {
            throw new UnsupportedOperationException(locationID + " is not supported by Janice");
        }
        //Update
        try {
            List<PricerItem> results = getGSON().fromJson(getCall(typeIDs).execute().body().string(), new TypeToken<List<PricerItem>>() {}.getType());
            if (results == null) {
                LOG.error("Error fetching price", new Exception("results is null"));
                addFailureReasons(typeIDs, "results is null");
                return returnMap;
            }
            //Updated OK
            for (PricerItem item : results) {
                returnMap.put(item.getTypeID(), item.getPriceContainer());
            }
            if (typeIDs.size() != returnMap.size()) {
                List<Integer> errors = new ArrayList<>(typeIDs);
                errors.removeAll(returnMap.keySet());
                PriceContainer container = new PriceContainer();
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
        StringBuilder body = new StringBuilder();
        for (Integer typeID : typeIDs) {
            body.append(typeID);
            body.append("\r\n");
        }
        Request.Builder request = new Request.Builder()
                .url("https://janice.e-351.com/api/rest/v2/pricer?market=" + getPricingOptions().getLocationID())
                .post(RequestBody.create(body.toString(), MediaType.get("text/plain")))
                .addHeader("User-Agent", getPricingOptions().getUserAgent());
        //Headers
        for (Map.Entry<String, String> entry : getPricingOptions().getHeaders().entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        return getClient().newCall(request.build());
    }

    private static class PricerItem {
        String date;
        PricerMarket market;
        Integer buyOrderCount;
        Long buyVolume;
        Integer sellOrderCount;
        Long sellVolume;
        PricerItemValues immediatePrices;
        PricerItemValues top5AveragePrices;
        ItemType itemType;

        public PriceContainer getPriceContainer() {
            PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
            builder.putPrice(PriceType.BUY_HIGH, immediatePrices.buyPrice);
            builder.putPrice(PriceType.BUY_PERCENTILE, top5AveragePrices.buyPrice);
            builder.putPrice(PriceType.SELL_LOW, immediatePrices.sellPrice);
            builder.putPrice(PriceType.SELL_PERCENTILE, top5AveragePrices.sellPrice);
            return builder.build();
        }

        public Integer getTypeID() {
            return itemType.eid;
        }
    }
    private static class PricerMarket {
        Integer id;
        String name;
    }
    private static class PricerItemValues {
        Double buyPrice;
        Double splitPrice;
        Double sellPrice;
        Double buyPrice5DayMedian;
        Double splitPrice5DayMedian;
        Double sellPrice5DayMedian;
        Double buyPrice30DayMedian;
        Double splitPrice30DayMedian;
        Double sellPrice30DayMedian;
    }
    private static class ItemType {
        Integer eid;
        String name;
        Double volume;
        Double packagedVolume;
    }
}
