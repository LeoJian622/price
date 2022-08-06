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
package uk.me.candle.eve.pricing;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PriceType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.PricingOptions;

/**
 *
 * @author Candle
 */
public interface Pricing {

    /**
     * Update all prices in the Set
     * @param itemIDs
     */
    public void updatePrices(Set<Integer> itemIDs);

    /**
     *
     * @param itemID
     * @param type the type of price: buy price or sell price
     * @return null if the price needs fetching or a Double representing the price.
     */
    public Double getPriceCache(int itemID, PriceType type);

    /**
     *
     * @param itemID
     * @param type the type of price: buy price or sell price
     * @return null if the price needs fetching or a Double representing the price.
     */
    public Double getPrice(int itemID, PriceType type);

    public boolean removePricingListener(PricingListener o);
    public boolean addPricingListener(PricingListener pl);
    public boolean removePricingFetchListener(PricingFetchListener o);
    public boolean addPricingFetchListener(PricingFetchListener pfl);

    /**
     * to unset a price, and queue it for fetching again, set the price to negative.
     * @param typeID
     */
    public void clearPrice(int typeID);

    public PricingFetch getPricingFetchImplementation();
    /**
     * set the options that this pricing implementation should use.
     * @param options
     */
    public void setPricingOptions(PricingOptions options);

    public PricingOptions getPricingOptions();

    /**
     * gets the time that the price for the item will be automatically updated.
     * @param itemID
     * @return a long timestamp, with -1 if the item either needs an update or is not in the local cache.
     */
    public long getNextUpdateTime(int itemID);

    /**
     * forces a write of the pricing data.
     * @throws IOException
     */
    public void writeCache() throws IOException;

    /**
     * cancels any price fetch request for this itemID
     * @param itemID
     */
    public void cancelSingle(int itemID);

    /**
     * cancels all pending pricing requests.
     */
    public void cancelAll();

    /**
     * resets the attempt counters. - if an item fails to be fetched then a counter should
     * be incremented up to the value returned by PricingOptions.getAttemptCount() after which
     * requests to fetch the price will be ignored.
     */
    public void resetAllAttemptCounters();

    /**
     * resets a single attempt counter
     * @See{resetAllAttemptCounters()}
     * @param itemID the item for which to reset the item counter.
     */
    public void resetAttemptCounter(int itemID);

    /**
     * gets the current count of failed attempts for the particular item.
     * @param itemID the itemID for which to get the attempt count.
     * @return the number of failed attempts to fetch the price for the item.
     */
    public int getFailedAttempts(int itemID);

    /**
     * If fetching the price for the given itemID failed, then this list will have error messages.
     * @param typeID
     * @return a list of error messages or an empty list if there were no errors.
     */
    public List<String> getFetchErrors(int typeID);

    /**
     * Supported PricingTypes.
     * @return
     */
    public List<PriceType> getSupportedPricingTypes();
    /**
     * Supported LocationTypes.
     * @return
     */
    public List<LocationType> getSupportedLocationTypes();

    /**
     *
     * @param locationType
     * @return List of support locationIDs. Empty list if all is support. Null if LocationType is not supported.
     */
    public List<Long> getSupportedLocations(LocationType locationType);
}
