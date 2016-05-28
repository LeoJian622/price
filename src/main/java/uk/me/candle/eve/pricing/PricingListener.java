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

/**
 *
 * When a price has been fetched, implementations of this listener are informed
 * that the price has been updated. The prices should now be able to be fetched by the
 * "getPrice(...)" method on the Pricing paramater.
 *
 * @author Candle
 */
public interface PricingListener {
    public void priceUpdated(int typeID, Pricing pricing);
    /**
     * This method is called when fetching prices failed.
     * use Pricing.getFetchErrors(int typeID) to obtain a list of errors.
     * @param typeID
     * @param pricing
     */
    public void priceUpdateFailed(int typeID, Pricing pricing);
}
