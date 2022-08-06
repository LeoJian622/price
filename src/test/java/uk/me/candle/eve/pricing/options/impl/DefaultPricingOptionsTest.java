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
package uk.me.candle.eve.pricing.options.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static junit.framework.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Candle
 */
public class DefaultPricingOptionsTest {
    static DefaultPricingOptions options;

    @BeforeClass
    public static void setup() {
        options = new DefaultPricingOptions();
    }

    @Test
    public void testGetPriceCacheTimer() {
        // bleugh
    }

    @Test
    public void testGetRegions() {
        assertNotNull(options.getLocation());
    }

    @Test
    public void testGetCacheInputStream() throws Exception {
        try {
            InputStream os = options.getCacheInputStream();
        } catch (IOException ioe) {
            // FileNotFound is expected.
            if (!(ioe instanceof FileNotFoundException)) {
                throw ioe;
            }
        }
    }

    @Test
    public void testGetCacheOutputStream() throws Exception {
        try {
            OutputStream os = options.getCacheOutputStream();
        } catch (IOException ioe) {
            // FileNotFound is expected.
            if (!(ioe instanceof FileNotFoundException)) {
                throw ioe;
            }
        }
    }
}
