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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import uk.me.candle.eve.pricing.PricingFetchListener;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;


public class CancelShutdownTest extends PricingTests {
	boolean ended;
	private static Pricing pricing;

	private CancelShutdownTest getThis() {
		return this;
	}

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
                return PricingFetch.EVE_CENTRAL;
            }
        });
	}

	@Before
	public void setUp() {
		
	
	}

    @Test
    public void testCancel() {
		System.out.println("Testing cancel recovery (fast)");
		SynchronousPriceListener listener = startThread();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		pricing.cancelAll(); //Cancel price fetch
		listener.interrupt(); //Stop thread
		listener = startThread();
		try {
			listener.join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(listener.getFailed().isEmpty());
	}

    @Test
    public void testCancelSlow() {
		System.out.println("Testing cancel recovery (slow)");
		ended = false;
		FetchListener fetchListener = new FetchListener();
		pricing.addPricingFetchListener(fetchListener);
		SynchronousPriceListener listener = startThread();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		pricing.cancelAll(); //Cancel price fetch
		listener.interrupt(); //Stop thread
		while(!ended) {
			try {
				synchronized(getThis()){
					getThis().wait();
				}
			} catch (InterruptedException ex) {
				break;
			}
		}
		pricing.removePricingFetchListener(fetchListener);
		listener = startThread();
		try {
			listener.join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(listener.getFailed().isEmpty());
	}

    @Test
    public void testShutdown() {
		System.out.println("Testing shutdown recovery (fast)");
		SynchronousPriceListener listener = startThread();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		pricing.shutdown();
		listener.interrupt(); //Stop thread
		listener = startThread();
		try {
			listener.join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(listener.getFailed().isEmpty());
	}

    @Test
    public void testShutdownSlow() {
		System.out.println("Testing shutdown recovery (slow)");
		FetchListener fetchListener = new FetchListener();
		pricing.addPricingFetchListener(fetchListener);
		SynchronousPriceListener listener = startThread();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		pricing.shutdown();
		listener.interrupt(); //Stop thread
		while(!ended) {
			try {
				synchronized(getThis()){
					getThis().wait();
				}
			} catch (InterruptedException ex) {
				break;
			}
		}
		pricing.removePricingFetchListener(fetchListener);
		listener = startThread();
		try {
			listener.join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(listener.getFailed().isEmpty());
	}

	private SynchronousPriceListener startThread() {
		SynchronousPriceListener listener = new SynchronousPriceListener(pricing, getTypeIDs());
		listener.start();
		return listener;
	}

	private class FetchListener implements PricingFetchListener {
		@Override
		public void fetchStarted() {
			ended = false;
		}
		@Override
		public void fetchEnded() {
			ended = true;
			synchronized(getThis()){
				getThis().notify();
			}
		}
	}
}
