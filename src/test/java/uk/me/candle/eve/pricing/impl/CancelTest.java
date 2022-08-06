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
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import static uk.me.candle.eve.pricing.impl.PricingTests.setLoggingLevel;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceLocation;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;


public class CancelTest extends PricingTests {

    private static Pricing pricing;

    @Before
    public void before() {
        setLoggingLevel(Level.INFO);
    }

    @BeforeClass
    public static void setUpClass() {
        PricingTests.setUpClass();
        pricing = PricingFactory.getPricing(PricingFetch.EVE_TYCOON, new DefaultPricingOptions() {
            @Override
            public PriceLocation getLocation() {
                return REGION_THE_FORGE;
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.REGION;
            }
        });
    }

    @Test
    public void testCancel() {
        test(500);
        test(1000);
        test(2000);
    }

    private void test(int delay) {
        long time = System.currentTimeMillis();
        System.out.println("Testing cancel recovery (" + delay + "ms)");
        Set<Integer> typeIDs = getTypeIDs(500);
        PricingTests.SynchronousPriceListener listener = new PricingTests.SynchronousPriceListener(pricing, typeIDs);
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
        System.out.println("    " + listener.getOK().size() + " of " + typeIDs.size() + " done - " + listener.getFailed().size() + " failed - " + listener.getEmpty().size() + " empty - completed in " + formatTime(System.currentTimeMillis() - time));
        assertEquals(listener.getFailed().size() + listener.getOK().size(), listener.getTypeIDs().size());
    }
}
