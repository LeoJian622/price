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
package uk.me.candle.eve.pricing.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import uk.me.candle.eve.pricing.impl.Fuzzwork;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;


public class TestFuzzwork extends PricingTests {

    @Test
    public void testGetPriceOnlineJita() {
        Pricing pricing = PricingFactory.getPricing(new DefaultPricingOptions() {
            @Override public List<Long> getLocations() {
                return Collections.singletonList(60003760L); //Jita;
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.STATION;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.FUZZWORK;
            }
        });
        testAll(pricing);
    }

    @Test
    public void testGetPriceOnlineRegion() {
        Pricing pricing = PricingFactory.getPricing(new DefaultPricingOptions() {
            @Override public List<Long> getLocations() {
                return Collections.singletonList(10000002L);
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.REGION;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.FUZZWORK;
            }
        });
        testAll(pricing);
    }

    @Test
    public void testGetPriceFail() {
        System.out.println("Testing FUZZWORK errors");
        final Fuzzwork pricing = new FuzzworkEmptyDummy();
        pricing.setPricingOptions(new DefaultPricingOptions() {
            @Override
            public List<Long> getLocations() {
                return Collections.singletonList(10000002L);
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.REGION;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.FUZZWORK;
            }
        });
        pricing.setPrice(34, -1d);
        Set<Integer> failed = synchronousPriceFetch(pricing, 34);
        assertEquals(failed.size(), 1);
    }

    class FuzzworkEmptyDummy extends Fuzzwork {

        public FuzzworkEmptyDummy() {
            super(1);
        }

        @Override
        protected InputStream getInputStream(Collection<Integer> itemIDs) throws MalformedURLException, IOException {
            throw new IOException("TESTING EXCEPTION");
        }

    }
}
