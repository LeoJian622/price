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

import ch.qos.logback.classic.Level;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import uk.me.candle.eve.pricing.impl.Janice.JaniceLocation;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceLocation;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;


public class JaniceTest extends PricingTests {

    private static final String JANICE_API_KEY = "JANICE_API_KEY";
    private String janiceKey;

    @Before
    public void before() {
        setLoggingLevel(Level.INFO);
        janiceKey = System.getenv().get(JANICE_API_KEY);
    }

    @Test
    public void testGetPriceOnlineJita() {
        Pricing pricing = PricingFactory.getPricing(PricingFetch.JANICE, new DefaultPricingOptions() {
            @Override
            public PriceLocation getLocation() {
                return JaniceLocation.JITA_4_4.getPriceLocation();
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.STATION;
            }
        });
        if (janiceKey != null) {
            pricing.getPricingOptions().addHeader("X-ApiKey", janiceKey);
        } else {
            fail("janiceKey is null");
        }
        testAll(pricing, "Jita 4-4");
    }

    @Test
    public void testGetPriceOnlinePerimeter() {
        Pricing pricing = PricingFactory.getPricing(PricingFetch.JANICE, new DefaultPricingOptions() {
            @Override
            public PriceLocation getLocation() {
                return JaniceLocation.PERIMETER_TTT.getPriceLocation();
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.STATION;
            }
        });
        if (janiceKey != null) {
            pricing.getPricingOptions().addHeader("X-ApiKey", janiceKey);
        } else {
            fail("janiceKey is null");
        }
        testAll(pricing, "Perimeter TTT");
    }

    @Test
    public void testGetPriceOnlineJitaPerimeter() {
        Pricing pricing = PricingFactory.getPricing(PricingFetch.JANICE, new DefaultPricingOptions() {
            @Override
            public PriceLocation getLocation() {
                return JaniceLocation.JITA_4_4_AND_PERIMETER_TTT.getPriceLocation();
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.STATION;
            }
        });
        if (janiceKey != null) {
            pricing.getPricingOptions().addHeader("X-ApiKey", janiceKey);
        } else {
            fail("janiceKey is null");
        }
        testAll(pricing, "Jita 4-4 + Perimeter TTT");
    }

}
