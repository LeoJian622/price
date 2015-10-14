/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.me.candle.eve.pricing.tests;

import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import uk.me.candle.eve.pricing.PricingFetchListener;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;

/**
 *
 * @author Niklas
 */
public class CancelShutdownTest extends PricingTests {
	boolean ended;
	private static Pricing pricing;

	private CancelShutdownTest getThis() {
		return this;
	}

	@BeforeClass
	public static void setUpClass() {
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
                return PricingFetch.EVE_MARKETDATA;
            }
        });
	}

	@Before
	public void setUp() {
		
	
	}

    @Test
    public void testCancel() {
		System.out.println("Testing cancel recovery (fast)");
		SynchronousPriceListener listener = startThread(true);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		pricing.cancelAll(); //Cancel price fetch
		listener.interrupt(); //Stop thread
		listener = startThread(false);
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
		SynchronousPriceListener listener = startThread(true);
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
		listener = startThread(false);
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
		SynchronousPriceListener listener = startThread(true);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		pricing.shutdown();
		listener.interrupt(); //Stop thread
		listener = startThread(false);
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
		SynchronousPriceListener listener = startThread(true);
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
		listener = startThread(false);
		try {
			listener.join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(listener.getFailed().isEmpty());
	}

	private SynchronousPriceListener startThread(boolean all) {
		SynchronousPriceListener listener = new SynchronousPriceListener(pricing, getTypeIDs(all));
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
