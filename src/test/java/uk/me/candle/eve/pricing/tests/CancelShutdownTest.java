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

	private CancelShutdownTest getThis() {
		return this;
	}

	@Before
	public void setUp() { }

    @Test
    public void testCancel() {
		System.out.println("Testing cancel recovery (fast)");
		Container container = startThread(true);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		container.getPricing().cancelAll(); //Cancel price fetch
		container.getListener().interrupt(); //Stop thread
		container = startThread(false);
		try {
			container.getListener().join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(container.getListener().getFailed().isEmpty());
	}

    @Test
    public void testCancelSlow() {
		System.out.println("Testing cancel recovery (slow)");
		Container container = startThread(true);
		ended = false;
		container.getPricing().addPricingFetchListener(new PricingFetchListener() {
			@Override
			public void fetchStarted() {
				
			}
			@Override
			public void fetchEnded() {
				ended = true;
				synchronized(getThis()){
					getThis().notify();
				}
			}
		});
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		container.getPricing().cancelAll(); //Cancel price fetch
		container.getListener().interrupt(); //Stop thread
		while(!ended) {
			try {
				synchronized(getThis()){
					getThis().wait();
				}
			} catch (InterruptedException ex) {
				break;
			}
		}
		container = startThread(false);
		try {
			container.getListener().join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(container.getListener().getFailed().isEmpty());
	}

    @Test
    public void testShutdown() {
		System.out.println("Testing shutdown recovery (fast)");
		Container container = startThread(true);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		container.getPricing().shutdown();
		container.getListener().interrupt(); //Stop thread
		container = startThread(false);
		try {
			container.getListener().join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(container.getListener().getFailed().isEmpty());
	}

    @Test
    public void testShutdownSlow() {
		System.out.println("Testing shutdown recovery (slow)");
		Container container = startThread(true);
		ended = false;
		container.getPricing().addPricingFetchListener(new PricingFetchListener() {
			@Override
			public void fetchStarted() {
				
			}
			@Override
			public void fetchEnded() {
				ended = true;
				synchronized(getThis()){
					getThis().notify();
				}
			}
		});
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		container.getPricing().shutdown();
		container.getListener().interrupt(); //Stop thread
		while(!ended) {
			try {
				synchronized(getThis()){
					getThis().wait();
				}
			} catch (InterruptedException ex) {
				break;
			}
		}
		container = startThread(false);
		try {
			container.getListener().join();
		} catch (InterruptedException ex) {
			fail("Thread interrupted");
		}
		assertTrue(container.getListener().getFailed().isEmpty());
	}

	private Container startThread(boolean all) {
		Pricing pricing = PricingFactory.getPricing(new DefaultPricingOptions() {
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
		SynchronousPriceListener listener = new SynchronousPriceListener(pricing, getTypeIDs(all));
		Container container = new Container(listener, pricing);
		listener.start();
		return container;
	}

	private static class Container {
		private final SynchronousPriceListener listener;
		private final Pricing pricing;

		public Container(SynchronousPriceListener listener, Pricing pricing) {
			this.listener = listener;
			this.pricing = pricing;
		}

		public SynchronousPriceListener getListener() {
			return listener;
		}

		public Pricing getPricing() {
			return pricing;
		}
	}
}
