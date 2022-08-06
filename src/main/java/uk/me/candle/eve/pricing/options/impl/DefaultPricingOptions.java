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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceLocation;
import uk.me.candle.eve.pricing.options.PricingOptions;

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
    public PriceLocation getLocation() {
        return new DefaultLocation(10000002L, 10000002L); // The Forge
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.REGION;
    }

    @Override
    public InputStream getCacheInputStream() throws IOException {
        return null;
    }

    @Override
    public OutputStream getCacheOutputStream() throws IOException {
        return null;
    }

    @Override
    public boolean getCacheTimersEnabled() {
        return true;
    }

    @Override
    public Proxy getProxy() {
        return null;
    }

    @Override
    public int getAttemptCount() {
        return 2;
    }

    @Override
    public boolean getUseBinaryErrorSearch() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 20000;
    }

    @Override
    public String getUserAgent() {
        String userAgent = System.getProperty("http.agent");
        if (userAgent == null) {
            userAgent = "Price/slack:@goldengnu";
        }
        return userAgent;
    }
}
