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

/**
 *
 * @author Candle
 */
public enum PriceType implements java.io.Serializable {
    BUY_HIGH,
    BUY_LOW,
    BUY_MEAN,
    BUY_MEDIAN,
    BUY_PERCENTILE,
    SELL_HIGH,
    SELL_LOW,
    SELL_MEAN,
    SELL_MEDIAN,
    SELL_PERCENTILE;
}
