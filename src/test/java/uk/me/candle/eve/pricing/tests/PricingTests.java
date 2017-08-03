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

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingListener;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;
import uk.me.candle.eve.pricing.tests.reader.Item;
import uk.me.candle.eve.pricing.tests.reader.ItemsReader;

/**
 *
 * @author Candle
 */
public class PricingTests {

	@BeforeClass
	public static void setUpClass() {
		System.setProperty("http.agent", "Price");
		Logger.getRootLogger().setLevel(Level.OFF);
		//Logger.getRootLogger().setLevel(Level.INFO);
		//Logger.getRootLogger().setLevel(Level.DEBUG);
	}
	
	@AfterClass
	public static void tearDownClass() {
		Logger.getRootLogger().setLevel(Level.INFO);
	}

	private static final boolean ALL = false;

	private Set<Integer> typeAll = null;
	private Set<Integer> typeFew = null;
	
	public Set<Integer> synchronousPriceFetch(Pricing pricing, int typeID) {
		 return synchronousPriceFetch(pricing, Collections.singleton(typeID));
	}

    public Set<Integer> synchronousPriceFetch(Pricing pricing, Set<Integer> typeIDs) {
        SynchronousPriceListener listener = new SynchronousPriceListener(pricing, typeIDs);
		listener.doStuff();
		return listener.getFailed();
    }

    protected class SynchronousPriceListener extends Thread implements PricingListener {
        private final Set<Integer> queue;
        private final Set<Integer> typeIDs;
		private final Set<Integer> ok;
        private final Set<Integer> failed;
		private final Pricing pricing;

		public SynchronousPriceListener(Pricing pricing, Set<Integer> typeIDs) {
			this.pricing = pricing;
			this.typeIDs = Collections.synchronizedSet(new HashSet<Integer>(typeIDs));
			this.queue = Collections.synchronizedSet(new HashSet<Integer>(typeIDs));
			this.ok = Collections.synchronizedSet(new HashSet<Integer>());
			this.failed = Collections.synchronizedSet(new HashSet<Integer>());
		}

		public Set<Integer> getFailed() {
			return failed;
		}

		@Override
        public void run() {
			doStuff();
        }

		public void doStuff() {
			//clear prices
            for (int typeID : typeIDs) {
                pricing.setPrice(typeID, -1d);
            }
			pricing.addPricingListener(this);
            //update prices
            for (int typeID : typeIDs) {
                getPrice(pricing, typeID);
            }
            while (!queue.isEmpty()) {
                try {
                    synchronized(this) {
                        wait(); // this is notified in the SynchronousPriceListener2
                        // once it is notified, it checks the loop condition again
                        //System.out.println("Working >> " + ok.size() + " of " + typeIDs.size() + " done - " + failed.size() + " failed");
                    }
                } catch (InterruptedException ie) {
                    break;
                }
            }
            pricing.removePricingListener(this);
        }

        private void getPrice(Pricing pricing, int typeID) {
            boolean done = true;
            PricingFetch pricingFetchImpl = pricing.getPricingOptions().getPricingFetchImplementation();
            for (PricingNumber number : pricingFetchImpl.getSupportedPricingNumbers()) {
                for (PricingType type : pricingFetchImpl.getSupportedPricingTypes()) {
                    Double price = pricing.getPrice(typeID, type, number);
                    if (price == null) {
                        done = false;
                    }
                }
            }
            if (done) {
                ok.add(typeID);
                failed.remove(typeID);
            } else {
                failed.add(typeID);
            }
        }

        @Override
        public void priceUpdated(int typeID, Pricing pricing) {
            getPrice(pricing, typeID);
			queue.remove(typeID);
            synchronized(this) {
                notifyAll();
            }
        }

        @Override
        public void priceUpdateFailed(int typeID, Pricing pricing) {
            failed.add(typeID);
			queue.remove(typeID);
            synchronized(this) {
                notifyAll();
            }
        }
    }

    void testAll(Pricing pricing){
        System.out.println("Testing "
                + pricing.getPricingOptions().getPricingFetchImplementation().name()
                + " ("
                + (pricing.getPricingOptions().getLocations().size() == 1 ? "Single" : "Multi")
                + " "
                + pricing.getPricingOptions().getLocationType().name().toLowerCase()
                + ")"
                );

        
        //will be zero:
        long time = System.currentTimeMillis();
		Set<Integer> typeIDs = getTypeIDs();
        Set<Integer> failed = synchronousPriceFetch(pricing, typeIDs);
        System.out.println("    " + (typeIDs.size() - failed.size()) + " of " + typeIDs.size() + " done - " + failed.size() + " failed - completed in " + formatTime(System.currentTimeMillis() - time));
        assertTrue(failed.isEmpty());
    }

	protected Set<Integer> getTypeIDs() {
		if (ALL) {
			if (typeAll == null) {
				typeAll = new HashSet<Integer>();
				Map<Integer, Item> items = null;
				items = ItemsReader.load();
				if (items == null) {
					
				} else {
					for (Item item : items.values()) {
						if (item.isMarketGroup()) {
							typeAll.add(item.getTypeID());
						}
					}
				}
			}
			return typeAll;
		} else {
			if (typeFew == null) {
				typeFew = new HashSet<Integer>();
				for (int i = 178; i <= 267; i++) {
					if (i == 214) {
						continue;
					}
					typeFew.add(i);
				}
				typeFew.add(34);
				typeFew.add(627);
				typeFew.add(17865);
				typeFew.add(17347);
				typeFew.add(20183);
				typeFew.add(20183);
				typeFew.add(455);
				typeFew.add(33578);
				typeFew.add(33579);
			}
			return typeFew;
		}
	}

    private String formatTime(long time) {
		final StringBuilder timeString = new StringBuilder();
		long days = time / (24 * 60 * 60 * 1000);
		if (days > 0) {
			timeString.append(days);
			timeString.append("d");
		}
		long hours = time / (60 * 60 * 1000) % 24;
		if (hours > 0) {
			if (days > 0) {
				timeString.append(" ");
			}
			timeString.append(hours);
			timeString.append("h");
		}
		long minutes = time / (60 * 1000) % 60;
		if (minutes > 0) {
			if (hours > 0) {
				timeString.append(" ");
			}
			timeString.append(minutes);
			timeString.append("m");
		}
		long seconds = time / (1000) % 60;
		if (seconds > 0) {
			if (minutes > 0) {
				timeString.append(" ");
			}
			timeString.append(seconds);
			timeString.append("s");
		}
		if (days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
			timeString.append(time);
			timeString.append("ms");
		}
		return timeString.toString();
	}
}
