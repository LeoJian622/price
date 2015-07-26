package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static junit.framework.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class TestDefaultPricingOptions {
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
    public void testGetPricingFetchImplementation() {
        assertNotNull(options.getPricingFetchImplementation());
        assertTrue("length > 0", options.getPricingFetchImplementation().length() > 0);
    }

    @Test
    public void testGetRegions() {
        assertNotNull(options.getRegions());
    }

    @Test
    public void testGetPricingType() {
        assertNotNull(options.getPricingType());
    }

    @Test
    public void testGetPricingNumber() {
        assertNotNull(options.getPricingNumber());
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
