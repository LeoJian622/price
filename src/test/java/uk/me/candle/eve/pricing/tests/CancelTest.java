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


import java.util.Collections;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;


public class CancelTest extends PricingTests {

    private static final int ITERATIONS = 5;

    private static Pricing pricing;

    @BeforeClass
    public static void setUpClass() {
        PricingTests.setUpClass();
        pricing = PricingFactory.getPricing(new DefaultPricingOptions() {
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
                return PricingFetch.EVEMARKETER;
            }
        });
    }

    @Test
    public void testCancel() {
        Random rand = new Random();
        test(500);
        for (int i = 0; i < ITERATIONS; i++) {
            int n = rand.nextInt(9000) + 1000;
            test(n);
        }
    }

    private void test(int delay) {
        System.out.println("Testing cancel recovery (" + delay + "ms)");
        PricingTests.SynchronousPriceListener listener = new PricingTests.SynchronousPriceListener(pricing, getTypeIDs());
        listener.start();
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            fail("Thread interrupted");
        }
        pricing.cancelAll(); //Cancel price fetch
        try {
            listener.join();
        } catch (InterruptedException ex) {
            fail("Thread interrupted");
        }
        assertTrue(listener.getFailed().size() > 1000);
        assertEquals(listener.getFailed().size() + listener.getOK().size(), listener.getTypeIDs().size());
    }
}
