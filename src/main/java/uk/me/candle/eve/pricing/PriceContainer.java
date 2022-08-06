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
package uk.me.candle.eve.pricing;

import java.util.EnumMap;
import java.util.Map;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;


public class PriceContainer implements java.io.Serializable {
    private static final long serialVersionUID = 1l;

    private final Map<PricingType, Map<PricingNumber, Double>> prices;

    public PriceContainer() {
        this.prices = new EnumMap<>(PricingType.class);
    }

    private PriceContainer(Map<PricingType, Map<PricingNumber, Double>> prices) {
        this.prices = prices;
    }

    public PriceContainerBuilder createClone() {
        return PriceContainerBuilder.createClone(this);
    }

    public Double getPrice(PriceType priceType) {
        return getPrice(getType(priceType), getNumber(priceType));
    }

    private double getPrice(PricingType type, PricingNumber number) {
        // null pointer/contains checks because if the internet
        // connection is removed during a fetch, this will consistantly
        // throw a null pointer exception.
        // http://code.google.com/p/jeveassets/issues/detail?id=130
        if (prices.containsKey(type)
                && prices.get(type).containsKey(number)) {
            return prices.get(type).get(number);
        } else {
            return 0.0;
        }
    }

    private static PricingType getType(PriceType priceType) {
        switch (priceType) {
            case BUY_HIGH: return PricingType.HIGH;
            case BUY_LOW: return PricingType.LOW;
            case BUY_MEAN: return PricingType.MEAN;
            case BUY_MEDIAN: return PricingType.MEDIAN;
            case BUY_PERCENTILE: return PricingType.PERCENTILE;
            case SELL_HIGH: return PricingType.HIGH;
            case SELL_LOW: return PricingType.LOW;
            case SELL_MEAN: return PricingType.MEAN;
            case SELL_MEDIAN: return PricingType.MEDIAN;
            case SELL_PERCENTILE: return PricingType.PERCENTILE;
            default: throw new RuntimeException();
        }
    }

    private static PricingNumber getNumber(PriceType priceType) {
        switch (priceType) {
            case BUY_HIGH: return PricingNumber.BUY;
            case BUY_LOW: return PricingNumber.BUY;
            case BUY_MEAN: return PricingNumber.BUY;
            case BUY_MEDIAN: return PricingNumber.BUY;
            case BUY_PERCENTILE: return PricingNumber.BUY;
            case SELL_HIGH: return PricingNumber.SELL;
            case SELL_LOW: return PricingNumber.SELL;
            case SELL_MEAN: return PricingNumber.SELL;
            case SELL_MEDIAN: return PricingNumber.SELL;
            case SELL_PERCENTILE: return PricingNumber.SELL;
            default: throw new RuntimeException();
        }
    }

    public static class PriceContainerBuilder {
        Map<PricingType, Map<PricingNumber, Double>> pricesTemp = new EnumMap<>(PricingType.class);

        public static PriceContainerBuilder createClone(PriceContainer container) {
            PriceContainerBuilder pcb = new PriceContainerBuilder();
            for (Map.Entry<PricingType, Map<PricingNumber, Double>> e1 : container.prices.entrySet()) {
                for (Map.Entry<PricingNumber, Double> e2 : e1.getValue().entrySet()) {
                    pcb.putPrice(e1.getKey(), e2.getKey(), e2.getValue());
                }
            }
            return pcb;
        }

        private PriceContainerBuilder putPrice(PricingType type, PricingNumber number, double price) {
            Map<PricingNumber, Double> typeMap;
            if (pricesTemp.containsKey(type)) {
                typeMap = pricesTemp.get(type);
            } else {
                typeMap = new EnumMap<>(PricingNumber.class);
                pricesTemp.put(type, typeMap);
            }
            typeMap.put(number, price);
            return this;
        }

        public PriceContainerBuilder putPrice(PriceType priceType, double price) {
            return putPrice(getType(priceType), getNumber(priceType), price);
        }

        public PriceContainer build() {
            if (pricesTemp.isEmpty()) {
                return null;
            } else {
                return new PriceContainer(pricesTemp);
            }
        }
    }
}
