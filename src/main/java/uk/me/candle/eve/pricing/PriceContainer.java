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
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;


public class PriceContainer implements java.io.Serializable {
    private static final long serialVersionUID = 1l;

    private Map<PricingType, Map<PricingNumber, Double>> prices;

    private PriceContainer(Map<PricingType, Map<PricingNumber, Double>> prices) {
        this.prices = prices;
    }

    public void putPrice(PricingType type, PricingNumber number, double price) {
        throw new RuntimeException("not supported");
    }

    public double getPrice(PricingType type, PricingNumber number) {
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

    public PriceContainerBuilder createClone() {
        return PriceContainerBuilder.createClone(this);
    }

    public static class PriceContainerBuilder {

        Map<PricingType, Map<PricingNumber, Double>> pricesTemp = new EnumMap<PricingType, Map<PricingNumber, Double>>(PricingType.class);

        public static PriceContainerBuilder createClone(PriceContainer container) {
            PriceContainerBuilder pcb = new PriceContainerBuilder();
            for (Map.Entry<PricingType, Map<PricingNumber, Double>> e1 : container.prices.entrySet()) {
                for (Map.Entry<PricingNumber, Double> e2 : e1.getValue().entrySet()) {
                    pcb.putPrice(e1.getKey(), e2.getKey(), e2.getValue());
                }
            }
            return pcb;
        }

        public PriceContainerBuilder putPrice(PricingType type, PricingNumber number, double price) {
            Map<PricingNumber, Double> typeMap;
            if (pricesTemp.containsKey(type)) {
                typeMap = pricesTemp.get(type);
            } else {
                typeMap = new EnumMap<PricingNumber, Double>(PricingNumber.class);
                pricesTemp.put(type, typeMap);
            }
            typeMap.put(number, price);
            return this;
        }

        public PriceContainer build() {
            // XXX TODO check that the map contains everything.
            return new PriceContainer(pricesTemp);
        }
    }
}
