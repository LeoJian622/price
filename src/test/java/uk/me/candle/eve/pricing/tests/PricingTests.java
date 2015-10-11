package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingListener;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;
import uk.me.candle.eve.pricing.tests.reader.Item;
import uk.me.candle.eve.pricing.tests.reader.ItemsReader;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class PricingTests {

	@BeforeClass
	public static void setUpClass() {
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
		return listener.doStuff();
    }

    protected class SynchronousPriceListener extends Thread implements PricingListener {
        private final Set<Integer> queue;
        private final Set<Integer> typeIDs;
		private final Set<Integer> ok;
        private final Set<Integer> failed;
		private final Pricing pricing;

		public SynchronousPriceListener(Pricing pricing, Set<Integer> typeIDs) {
			this.pricing = pricing;
			this.typeIDs = typeIDs;
			this.queue = new HashSet<Integer>(typeIDs);
			ok = new HashSet<Integer>();
			failed = new HashSet<Integer>();
		}

		public Set<Integer> getFailed() {
			return failed;
		}

		@Override
        public void run() {
			doStuff();
        }

		public Set<Integer> doStuff() {
			getQueue().addAll(typeIDs);
            //clear prices
            for (int typeID : typeIDs) {
                pricing.setPrice(typeID, -1d);
            }
			pricing.addPricingListener(this);
            //update prices
            for (int typeID : typeIDs) {
                getPrice(pricing, typeID);
            }
			int progress = 0;
            while (!getQueue().isEmpty()) {
                try {
                    synchronized(this) {
                        wait(); // this is notified in the SynchronousPriceListener2
                        // once it is notified, it checks the loop condition again
                        //System.out.println("Working >> " + ok.size() + " of " + typeIDs.size() + " done - " + failed.size() + " failed");
                    }
                } catch (InterruptedException ie) {
                    break;
                }
				int percent = (int)((typeIDs.size() - getQueue().size()) * 100.0 / typeIDs.size());
				if (progress != percent) {
					for (int i = progress; i < percent; i++) {
						System.out.print(".");
					}
					progress = percent;
				}
            }
			for (int i = 0; i < progress; i++) { //The rest
				System.out.print("\b");
			}
            pricing.removePricingListener(this);
			return failed;
		}

		public synchronized Set<Integer> getQueue() {
			return queue;
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
			getQueue().remove(typeID);
            synchronized(this) {
                notify();
            }
        }

        @Override
        public void priceUpdateFailed(int typeID, Pricing pricing) {
            failed.add(typeID);
			getQueue().remove(typeID);
            synchronized(this) {
                notify();
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
		Set<Integer> typeIDs = getTypeIDs(ALL);
        Set<Integer> failed = synchronousPriceFetch(pricing, typeIDs);
        System.out.println("    " + (typeIDs.size() - failed.size()) + " of " + typeIDs.size() + " done - " + failed.size() + " failed - completed in " + formatTime(System.currentTimeMillis() - time));
		/*
        if (!failed.isEmpty()) {
            System.out.println("        Failed:");
            for (Integer typeID : failed) {
                System.out.println("        " + typeID);
            }
        }
		*/
        assertTrue(failed.isEmpty());
    }

	protected Set<Integer> getTypeIDs(boolean all) {
		if (all) {
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
