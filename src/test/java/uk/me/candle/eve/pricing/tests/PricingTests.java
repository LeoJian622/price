package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import static org.junit.Assert.*;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingListener;
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
        Logger.getRootLogger().setLevel(Level.ALL);
    }
    final PricingTests lock = this;

    public double synchronousPriceFetch(Pricing pricing, int typeID, PricingType type, PricingNumber number) {
        SynchronousPriceListener listener = new SynchronousPriceListener();
        pricing.addPricingListener(listener);

        Double price = pricing.getPrice(typeID, type, number);
        if (price == null) {
            try {
                synchronized(this) {
                    wait();
                }
            } catch (InterruptedException ie) {
                fail("waiting thread was interrupted.");
            }
        }
        price = pricing.getPrice(typeID, type, number); // since we've waited, this should now not be null
        assertNotNull(price);

        return price;
    }

    class SynchronousPriceListener implements PricingListener {
        @Override
        public void priceUpdated(int typeID, Pricing pricing) {
            synchronized(lock) {
                lock.notify();
            }
        }

        @Override
        public void priceUpdateFailed(int typeID, Pricing pricing) {
            // do nothing.
        }
    }

    public Map<Integer, Double> synchronousPriceFetch(Pricing pricing, List<Integer> typeIDs, PricingType type, PricingNumber number) {
        Map<Integer, Double> ret = new HashMap<Integer, Double>();
        
        SynchronousPriceListener2 listener = new SynchronousPriceListener2(ret);
        pricing.addPricingListener(listener);

        for (int i : typeIDs) {
            Double price = pricing.getPrice(i, type, number);
            if (price != null) {
                ret.put(i, price);
            }
        }
        while (ret.size() < typeIDs.size()) {
            try {
                synchronized(this) {
                    wait(); // this is notified in the SynchronousPriceListener2
                    // once it is notified, it checks the loop condition again
                }
            } catch (InterruptedException ie) {
                fail("waiting thread was interrupted.");
            }
        }

        return ret;
    }

    class SynchronousPriceListener2 implements PricingListener {
        Map<Integer, Double> ret;

        public SynchronousPriceListener2(Map<Integer, Double> ret) {
            this.ret = ret;
        }

        @Override
        public void priceUpdated(int typeID, Pricing pricing) {
            ret.put(typeID, pricing.getPrice(typeID));
            synchronized(lock) {
                lock.notify();
            }
        }

        @Override
        public void priceUpdateFailed(int typeID, Pricing pricing) {
            // do nothing.
        }
    }
}
