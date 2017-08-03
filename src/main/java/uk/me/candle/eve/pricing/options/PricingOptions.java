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
package uk.me.candle.eve.pricing.options;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.util.List;


public interface PricingOptions {
    public long getPriceCacheTimer();
    public PricingFetch getPricingFetchImplementation();
    public List<Long> getLocations();
    public LocationType getLocationType();
    public PricingType getPricingType();
    public PricingNumber getPricingNumber();
	/**
	 * Binary search is a legacy feature used when eve-central returned an error on unknown IDs
	 * Eve-Central now fails silently, so the feature is not needed anymore, but, may still be useful in the future.
	 * @return true to do binary search for error ID(s) - false to add failed ID(s) back into the queue and keep trying until getAttemptCount() is reached
	 */
    public boolean getUseBinaryErrorSearch();
    /**
     * return the input stream to read the price cache from.
     * @return an input stream to try to read the price cache from.
     * @throws IOException If this is thrown, then reading is aborted.
     */
    public InputStream getCacheInputStream() throws IOException;
    /**
     * return an output stream to write the price cache to.
     * @return an output stream to write the cache to. If this method returns null, then caching is ignored.
     * @throws IOException if this is thrown, then writing is aborted.
     */
    public OutputStream getCacheOutputStream() throws IOException;

    /**
     *
     * @return true if the cache timers are disabled - i.e. just use the cache, don't hit the web
     * for any pricing data.
     */
    public boolean getCacheTimersEnabled();

    /**
     * return the proxy needed to get a connection or null
     * @return the proxy used for connections.
     */
    public Proxy getProxy();

    /**
     * A price will be attempted to be fetched n times, where
     * n is the number returned by this method, 0 or negative numbers
     * mean there will be no end to the fetch attempts.
     * @return the number of attempts that should be made to fetch a price for a particular item.
     */
    public int getAttemptCount();
}
