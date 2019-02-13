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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.impl.EveMarketer;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;

public class ReadWriteTest extends PricingTests {

    private static final String FILENAME = "pricing.serial";
    private static final int PRICES = 400;
    private static final int THREADS = 10;
    private static final int ITERATIONS = 1;

    private final ReadWriteOptions readWriteOptions = new ReadWriteOptions();
    private final File file = new File(FILENAME);
    private Pricing pricing;
    private Set<Integer> typeIDs;

    @Test
    public void readWriteTest() {
        //Cleanup
        if (file.exists()) {
            file.delete();
        }
        //Create file data to read
        typeIDs = getTypeIDs(PRICES);
        pricing = new EveMarketer(2); //need new instant to complete faster
        Set<Integer> failed = synchronousPriceFetch(pricing, typeIDs);
        assertEquals(0, failed.size());
        try {
            pricing.setPricingOptions(readWriteOptions);
            pricing.writeCache();
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
        //Iterations
        for (int i = 1; i <= ITERATIONS; i++) {
            System.out.println("Iteration: " + i);
            testReadWrite();
        }
        //Cleanup
        assertTrue(file.delete());
    }

    public void testReadWrite() {
        //Create threads
        List<Thread> threads = new ArrayList<Thread>();
        threads.add(new Update());
        for (int i = 1; i <= THREADS; i++) {
            threads.add(new Read(i));
            threads.add(new Write(i));
        }
        //Start threads
        for (Thread thread : threads) {
            thread.start();
        }
        //Wait for threads to finish
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                fail(ex.getMessage());
            }
        }
    }

    private class Read extends Thread {

        public Read(int number) {
            super("Read " + number);
        }

        @Override
        public void run() {
            pricing.setPricingOptions(readWriteOptions);
        }

    }

    private class Write extends Thread {

        public Write(int number) {
            super("Write " + number);
        }

        @Override
        public void run() {
            try {
                pricing.writeCache();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private class Update extends Thread {

        public Update() {
            super("Update");
        }

        @Override
        public void run() {
            Set<Integer> failed = synchronousPriceFetch(pricing, typeIDs);
        }
    }

    private class ReadWriteOptions extends DefaultPricingOptions {

        @Override
        public PricingFetch getPricingFetchImplementation() {
            return PricingFetch.EVEMARKETER;
        }

        @Override
        public InputStream getCacheInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public OutputStream getCacheOutputStream() throws IOException {
            return new FileOutputStream(file);
        }

    }
}
