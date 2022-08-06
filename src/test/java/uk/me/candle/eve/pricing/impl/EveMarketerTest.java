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

import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import static uk.me.candle.eve.pricing.impl.PricingTests.SYSTEM_JITA;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceLocation;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;


public class EveMarketerTest extends PricingTests {

    @Test
    public void testGetPriceOnlineRegion() {
        Pricing pricing = PricingFactory.getPricing(PricingFetch.EVEMARKETER, new DefaultPricingOptions() {
            @Override
            public PriceLocation getLocation() {
                return REGION_THE_FORGE;
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.REGION;
            }
        });
        testAll(pricing);
    }

    @Test
    public void testGetPriceOnlineSystem() {
        Pricing pricing = PricingFactory.getPricing(PricingFetch.EVEMARKETER, new DefaultPricingOptions() {
            @Override
            public PriceLocation getLocation() {
                return SYSTEM_JITA;
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.SYSTEM;
            }
        });
        testAll(pricing);
    }
}
