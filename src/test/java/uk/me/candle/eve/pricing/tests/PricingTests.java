package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import static org.junit.Assert.*;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingListener;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class PricingTests {
    static {
        Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("%-5p [%t]: %m%n")));
        Logger.getRootLogger().setLevel(Level.OFF);
        //Logger.getRootLogger().setLevel(Level.ERROR);
        //Logger.getRootLogger().setLevel(Level.INFO);
        //Logger.getRootLogger().setLevel(Level.ALL);
    }
	
	public Set<Integer> synchronousPriceFetch(Pricing pricing, int typeID) {
		 return synchronousPriceFetch(pricing, Collections.singleton(typeID));
	}

    public Set<Integer> synchronousPriceFetch(Pricing pricing, Set<Integer> typeIDs) {
        SynchronousPriceListener listener = new SynchronousPriceListener();
        return listener.run(pricing, typeIDs);
    }

    private class SynchronousPriceListener implements PricingListener {
        private final Set<Integer> ok = new HashSet<Integer>();
        private final Set<Integer> failed = new HashSet<Integer>();

        private Set<Integer> run(Pricing pricing, Set<Integer> typeIDs) {
            pricing.addPricingListener(this);
            //clear prices
            for (int typeID : typeIDs) {
                pricing.setPrice(typeID, -1d);
            }
            //update prices
            for (int typeID : typeIDs) {
                getPrice(pricing, typeID);
            }
            failed.clear();
            while (typeIDs.size() > (ok.size() + failed.size())) {
                try {
                    synchronized(this) {
                        wait(); // this is notified in the SynchronousPriceListener2
                        // once it is notified, it checks the loop condition again
                        //System.out.println("Working >> " + ok.size() + " of " + typeIDs.size() + " done - " + failed.size() + " failed");
                    }
                } catch (InterruptedException ie) {
                    fail("waiting thread was interrupted.");
                }
            }
            pricing.removePricingListener(this);
            return failed;
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
            synchronized(this) {
                notify();
            }
        }

        @Override
        public void priceUpdateFailed(int typeID, Pricing pricing) {
            failed.add(typeID);
            synchronized(this) {
                notify();
            }
        }
    }

    void testAll(Pricing pricing, Integer... allowedToFail){
        System.out.println("Testing "
                + pricing.getPricingOptions().getPricingFetchImplementation().name()
                + " ("
                + (pricing.getPricingOptions().getLocations().size() == 1 ? "Single" : "Multi")
                + " "
                + pricing.getPricingOptions().getLocationType().name().toLowerCase()
                + ")"
                );

        Set<Integer> typeIDs = new HashSet<Integer>();
        Set<Integer> okFailes = new HashSet<Integer>(Arrays.asList(allowedToFail));
        //Must not be zero
        for (int i = 178; i <= 267; i++) {
            if (i == 214) {
                continue;
            }
            typeIDs.add(i);
        }
        typeIDs.add(34);
        typeIDs.add(627);
        typeIDs.add(17865);
        typeIDs.add(17347);
        typeIDs.add(20183);
        typeIDs.add(20183);
        typeIDs.add(455);
        //Failed Eve-Central
        typeIDs.add(33578);
        typeIDs.add(33579);
        //will be zero:
        long time = System.currentTimeMillis();
        Set<Integer> failedAll = synchronousPriceFetch(pricing, typeIDs);
        Set<Integer> failed = new HashSet<Integer>(failedAll);
        failed.removeAll(okFailes);
        failedAll.removeAll(failed);
        System.out.println("    " + (typeIDs.size() - failed.size()) + " of " + typeIDs.size() + " done - " + failed.size() + " failed - " + failedAll.size() + " accepted fails - completed in " + formatTime(System.currentTimeMillis() - time));
        if (!failed.isEmpty()) {
            System.out.println("        Failed:");
            for (Integer typeID : failed) {
                System.out.println("        " + typeID);
            }
        }
        assertTrue(failed.isEmpty());
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
