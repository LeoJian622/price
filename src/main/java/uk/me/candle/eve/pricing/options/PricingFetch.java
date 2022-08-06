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
package uk.me.candle.eve.pricing.options;

import java.util.List;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.impl.EveMarketer;
import uk.me.candle.eve.pricing.impl.EveTycoon;
import uk.me.candle.eve.pricing.impl.Fuzzwork;
import uk.me.candle.eve.pricing.impl.Janice;


public enum PricingFetch {
    EVEMARKETER {
        @Override
        public Pricing getNewInstance() {
            return new EveMarketer();
        }
    },
    FUZZWORK {
        @Override
        public Pricing getNewInstance() {
            return new Fuzzwork();
        }
    },
    EVE_TYCOON {
        @Override
        public Pricing getNewInstance() {
            return new EveTycoon();
        }
    },
    JANICE() {
        @Override
        public Pricing getNewInstance() {
            return new Janice();
        }
    },
    ;
    private Pricing pricing;

    public Pricing getPricing(PricingOptions options) {
        if (pricing == null) {
            pricing = getNewInstance();
        } else {
            pricing.resetAllAttemptCounters();
        }
        if (options != null) {
            pricing.setPricingOptions(options);
        }
        return pricing;
    }

    protected abstract Pricing getNewInstance();

    public List<PriceType> getSupportedPricingTypes() {
        return getNewInstance().getSupportedPricingTypes();
    }
    public List<LocationType> getSupportedLocationTypes() {
        return getNewInstance().getSupportedLocationTypes();
    }
}
