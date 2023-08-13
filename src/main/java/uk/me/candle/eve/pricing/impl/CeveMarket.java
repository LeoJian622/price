package uk.me.candle.eve.pricing.impl;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import okhttp3.Call;
import okhttp3.Request;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.me.candle.eve.pricing.AbstractPricing;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingFetch;

import java.io.IOException;
import java.util.*;

/**
 * @author Leojan
 * @date 2023-08-07 11:09
 */

public class CeveMarket extends AbstractPricing {

    private static final Logger LOG = LoggerFactory.getLogger(CeveMarket.class);

    public CeveMarket() {
        super(4);
    }

    @Override
    public PricingFetch getPricingFetchImplementation() {
        return PricingFetch.CEVE_MARKET;
    }

    @Override
    public List<PriceType> getSupportedPricingTypes() {
        List<PriceType> types = new ArrayList<>();
        types.add(PriceType.BUY_MEDIAN);
        types.add(PriceType.BUY_PERCENTILE);
        types.add(PriceType.BUY_HIGH);
        types.add(PriceType.BUY_LOW);
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
        return this.getSupportedLocationTypes().contains(locationType) ? new ArrayList<>() : null;
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
            throw new UnsupportedOperationException("A location is required for Ceve Market");
        }
        LocationType locationType = getPricingOptions().getLocationType();
        if (!this.getSupportedLocationTypes().contains(locationType)) {
            throw new UnsupportedOperationException(locationType + " is not supported by Ceve Market");
        }
        //Update
        try {
            String xmlString = getCall(typeIDs).execute().body().string();
            String jsonString = XML.toJSONObject(xmlString).getJSONObject("evec_api").getJSONObject("marketstat").get("type").toString();
            List<CeveMarketPrice> results = getGSON().fromJson(jsonString, (new TypeToken<List<CeveMarketPrice>>() {
            }).getType());
            if (results == null) {
                LOG.error("Error fetching price", new Exception("results is null"));
                this.addFailureReasons(typeIDs, "results is null");
                return returnMap;
            }
            //Updated OK
            for (CeveMarketPrice item : results) {
                returnMap.put(item.getTypeID(), item.getPriceContainer());
            }

            if (typeIDs.size() != returnMap.size()) {
                List<Integer> errors = new ArrayList<>(typeIDs);
                errors.removeAll(returnMap.keySet());
                PriceContainer container = (new PriceContainer.PriceContainerBuilder()).build();
                for (Integer typeID : errors) {
                    returnMap.put(typeID, container);
                }
            }
        } catch (IOException | JsonParseException | IllegalArgumentException ex) {
            LOG.error("Error fetching price", ex);
            this.addFailureReasons(typeIDs, ex.getMessage());
        }
        return returnMap;
    }

    public Call getCall(Collection<Integer> typeIDs) {
        Request.Builder request = new Request.Builder()
                .url(this.getURL(typeIDs))
                .addHeader("User-Agent", getPricingOptions().getUserAgent());
        //Headers
        for (Map.Entry<String, String> entry : this.getPricingOptions().getHeaders().entrySet()) {
            request.addHeader(entry.getKey(), entry.getValue());
        }
        return this.getClient().newCall(request.build());
    }

    protected String getURL(Collection<Integer> typeIDs) {
        StringBuilder query = new StringBuilder();
        //TypeIDs
        query.append("&typeid=");
        boolean comma = false;
        for (Integer i : typeIDs) {
            if (comma) {
                query.append("&typeid=");
            } else {
                comma = true;
            }
            query.append(i);
        }
        //Location
        LocationType locationType = this.getPricingOptions().getLocationType();
        if (locationType == LocationType.STATION) {
            throw new UnsupportedOperationException(locationType + " is not supported by Ceve Market");
        } else if (locationType == LocationType.SYSTEM) {
            //Not empty
            query.append("&usesystem=");
            query.append(this.getPricingOptions().getLocationID());
        } else if (locationType == LocationType.REGION) {
            query.append("&regionlimit=");
            query.append(this.getPricingOptions().getLocationID());
        } else {
            throw new UnsupportedOperationException(locationType.name() + " is not supported by Ceve Market");
        }

        return "https://www.ceve-market.org/api/marketstat?" + query;
    }


    private static class CeveMarketPriceData {
        public double max;
        public double min;
        public double stddev;
        public double median;
        public double volume;
        public double percentile;

        private CeveMarketPriceData() {
        }
    }


    private static class CeveMarketPrice {
        Integer id;
        CeveMarketPriceData buy;
        CeveMarketPriceData sell;

        private CeveMarketPrice() {
        }

        public int getTypeID() {
            return id;
        }

        PriceContainer getPriceContainer() {
            PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
            builder.putPrice(PriceType.BUY_HIGH, this.buy.max);
            builder.putPrice(PriceType.BUY_LOW, this.buy.min);
            builder.putPrice(PriceType.BUY_MEAN, this.buy.stddev);
            builder.putPrice(PriceType.BUY_MEDIAN, this.buy.median);
            builder.putPrice(PriceType.BUY_PERCENTILE, this.buy.percentile);
            builder.putPrice(PriceType.SELL_HIGH, this.sell.max);
            builder.putPrice(PriceType.SELL_LOW, this.sell.min);
            builder.putPrice(PriceType.SELL_MEAN, this.sell.stddev);
            builder.putPrice(PriceType.SELL_MEDIAN, this.sell.median);
            builder.putPrice(PriceType.SELL_PERCENTILE, this.sell.percentile);
            return builder.build();
        }
    }
}
