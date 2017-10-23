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

import uk.me.candle.eve.pricing.impl.*;
import uk.me.candle.eve.pricing.options.PricingOptions;

/**
 *
 * @author Candle
 */
public class PricingFactory {
    static EveCentral eveCentral;
    static EveMarketData eveMarketData;
    static EveMarketeer eveMarketeer;
    static EveMarketer eveMarketer;
    static EveAddicts eveAddicts;

    public static Pricing getPricing(PricingOptions options) {
        switch (options.getPricingFetchImplementation()) {
            case EVEMARKETEER: return getEveMarketeer(options);
            case EVE_ADDICTS: return getEveAddicts(options);
            case EVE_CENTRAL: return getEveCentral(options);
            case EVE_MARKETDATA: return getEveMarketData(options);
            case EVEMARKETER: return getEveMarketer(options);
            default: return getEveCentral(options);
        }
    }

    private static Pricing getEveMarketData(PricingOptions options) {
        if (eveMarketData == null) {
            eveMarketData = new EveMarketData(2);
        } else {
            eveMarketData.resetAllAttemptCounters();
        }
        if (options != null) {
            eveMarketData.setPricingOptions(options);
        }
        return eveMarketData;
    }
    private static Pricing getEveMarketeer(PricingOptions options) {
        if (eveMarketeer == null) {
            eveMarketeer = new EveMarketeer(1);
        } else {
            eveMarketeer.resetAllAttemptCounters();
        }
        if (options != null) {
            eveMarketeer.setPricingOptions(options);
        }
        return eveMarketeer;
    }
    private static Pricing getEveMarketer(PricingOptions options) {
        if (eveMarketer == null) {
            eveMarketer = new EveMarketer(2);
        } else {
            eveMarketer.resetAllAttemptCounters();
        }
        if (options != null) {
            eveMarketer.setPricingOptions(options);
        }
        return eveMarketer;
    }
    private static Pricing getEveCentral(PricingOptions options) {
        if (eveCentral == null) {
            eveCentral = new EveCentral(1);
        } else {
            eveCentral.resetAllAttemptCounters();
        }
        if (options != null) {
            eveCentral.setPricingOptions(options);
        }
        return eveCentral;
    }
    private static Pricing getEveAddicts(PricingOptions options) {
        if (eveAddicts == null) {
            eveAddicts = new EveAddicts(1);
        } else {
            eveAddicts.resetAllAttemptCounters();
        }
        if (options != null) {
            eveAddicts.setPricingOptions(options);
        }
        return eveAddicts;
    }
}
