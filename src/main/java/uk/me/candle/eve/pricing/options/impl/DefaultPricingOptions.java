package uk.me.candle.eve.pricing.options.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.util.Collections;
import java.util.List;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingOptions;
import uk.me.candle.eve.pricing.options.PricingType;

/**
 *
 * @author Candle
 */
public class DefaultPricingOptions implements PricingOptions {


    @Override
    public long getPriceCacheTimer() {
        return 60*60*1000l; // 1 hour
    }

    @Override
    public PricingFetch getPricingFetchImplementation() {
        return PricingFetch.EVE_MARKETDATA;
    }

    @Override
    public List<Long> getLocations() {
        return Collections.singletonList(new Long(10000002)); // The Forge
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.REGION;
    }

    @Override
    public PricingType getPricingType() {
        return PricingType.LOW;
    }

    @Override
    public PricingNumber getPricingNumber() {
        return PricingNumber.SELL;
    }

    @Override
    public InputStream getCacheInputStream() throws IOException {
        return null; //return new FileInputStream(new File("pricing.serial"));
    }

    @Override
    public OutputStream getCacheOutputStream() throws IOException {
        return null; //return new FileOutputStream(new File("pricing.serial"));
    }

    @Override
    public boolean getCacheTimersEnabled() {
        return true;
    }

    @Override
    public Proxy getProxy() {
        return Proxy.NO_PROXY;
    }

    @Override
    public int getAttemptCount() {
        return 1000; // return a number that is not infinite but is sufficiently large.
    }
}
