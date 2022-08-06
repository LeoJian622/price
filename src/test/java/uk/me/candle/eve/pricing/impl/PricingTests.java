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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingListener;
import uk.me.candle.eve.pricing.options.PriceLocation;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultLocation;
import uk.me.candle.eve.pricing.utils.Item;
import uk.me.candle.eve.pricing.utils.ItemsReader;

/**
 *
 * @author Candle
 */
public class PricingTests {

    public static final PriceLocation REGION_THE_FORGE = new DefaultLocation(10000002L, 10000002L);  //The Forge
    public static final PriceLocation SYSTEM_JITA = new DefaultLocation(10000002L, 30000142L);  //Jita
    public static final PriceLocation STATION_JITA_4_4 = new DefaultLocation(10000002L, 60003760L);  //Jita 4-4

    private static final int PRICES = 500; //-1 for all

    protected static void setLoggingLevel(Level level) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(level);
    }

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("http.agent", "Price");
        System.setProperty("log.home", "");
        setLoggingLevel(Level.INFO);
    }

    @AfterClass
    public static void tearDownClass() {
        setLoggingLevel(Level.INFO);
    }

    private Map<Integer, Item> items;

    public Set<Integer> synchronousPriceFetch(Pricing pricing, int typeID) {
         return synchronousPriceFetch(pricing, Collections.singleton(typeID));
    }

    public Set<Integer> synchronousPriceFetch(Pricing pricing, Set<Integer> typeIDs) {
        long time = System.currentTimeMillis();
        SynchronousPriceListener listener = new SynchronousPriceListener(pricing, typeIDs);
        listener.start();
        try {
            listener.join();
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }
        System.out.println("    " + listener.getOK().size() + " of " + typeIDs.size() + " done - " + listener.getFailed().size() + " failed - " + listener.getEmpty().size() + " empty - completed in " + PricingTests.formatTime(System.currentTimeMillis() - time));
        return listener.getFailed();
    }

    protected static class SynchronousPriceListener extends Thread implements PricingListener {
        private final Set<Integer> queue;
        private final Set<Integer> typeIDs;
        private final Set<Integer> ok;
        private final Set<Integer> failed;
        private final Set<Integer> empty;
        private final Pricing pricing;

        public SynchronousPriceListener(Pricing pricing, Set<Integer> typeIDs) {
            super("SynchronousPriceListener");
            this.pricing = pricing;
            this.typeIDs = Collections.synchronizedSet(new HashSet<>(typeIDs));
            this.queue = Collections.synchronizedSet(new HashSet<>(typeIDs));
            this.ok = Collections.synchronizedSet(new HashSet<>());
            this.failed = Collections.synchronizedSet(new HashSet<>());
            this.empty = Collections.synchronizedSet(new HashSet<>());
        }

        public Set<Integer> getFailed() {
            return failed;
        }

        public Set<Integer> getEmpty() {
            return empty;
        }

        public Set<Integer> getOK() {
            return ok;
        }

        public Set<Integer> getTypeIDs() {
            return typeIDs;
        }

        @Override
        public void run() {
            doStuff();
        }

        public void doStuff() {
            pricing.addPricingListener(this);
            //update prices
            pricing.updatePrices(typeIDs);
            while (!queue.isEmpty()) {
                try {
                    synchronized(this) {
                        wait(1000); // this is notified in the SynchronousPriceListener2
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
            boolean isEmpty = true;
            PricingFetch pricingFetchImpl = pricing.getPricingFetchImplementation();
            for (PriceType type : pricingFetchImpl.getSupportedPricingTypes()) {
                Double price = pricing.getPrice(typeID, type);
                if (price != 0) {
                    isEmpty = false;
                }
                if (price == null) {
                    done = false;
                }
            }
            if (isEmpty) {
                empty.add(typeID);
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
        testAll(pricing, null, null);
    }

    void testAll(Pricing pricing, int max){
        testAll(pricing, null, max);
    }

    void testAll(Pricing pricing, String type){
        testAll(pricing, type, null);
    }

    void testAll(Pricing pricing, String type, Integer max){
        if (type == null) { //Default
            type = pricing.getPricingOptions().getLocationType().name().toLowerCase();
        }
        if (max == null) { //Default
            max = PRICES;
        }
        System.out.println("Testing "
                + pricing.getPricingFetchImplementation().name()
                + " ("
                + type
                + ")"
                );

        //will be zero:
        Set<Integer> typeIDs = getTypeIDs(max);
        Set<Integer> failed = synchronousPriceFetch(pricing, typeIDs);
        assertTrue(failed.isEmpty());
    }

    protected Set<Integer> getTypeIDs(final int max) {
        Set<Integer> typeIDs = new HashSet<>();
        if (items == null) { //Only load items once
            items = ItemsReader.load();
        }
        if (items != null) {
            for (Item item : items.values()) {
                if (item.isMarketGroup()) {
                    typeIDs.add(item.getTypeID());
                    if (typeIDs.size() >= max && max > 0) {
                        break;
                    }
                }
            }
        } else {
            typeIDs = new HashSet<>();
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
            typeIDs.add(33578);
            typeIDs.add(33579);
        }
        System.out.println("Testing " + typeIDs.size() + " items");
        return typeIDs;
    }

    public static String formatTime(long time) {
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
