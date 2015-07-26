package uk.me.candle.eve.pricing;

// <editor-fold defaultstate="collapsed" desc="imports">

import uk.me.candle.eve.pricing.impl.*;
import uk.me.candle.eve.pricing.options.PricingFetch;
import static uk.me.candle.eve.pricing.options.PricingFetch.EVEMARKETEER;
import uk.me.candle.eve.pricing.options.PricingOptions;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class PricingFactory {
    static EveCentral eveCentral;
    static EveMarketData eveMarketData;
    static EveMarketeer eveMarketeer;
    static EveAddicts eveAddicts;

    public static Pricing getPricing(PricingOptions options) {
        switch (options.getPricingFetchImplementation()) {
            case EVEMARKETEER: return getEveMarketeer(options);
            case EVE_ADDICTS: return getEveAddicts(options);
            case EVE_CENTRAL: return getEveCentral(options);
            case EVE_MARKETDATA: return getEveMarketData(options);
            default: return getEveCentral(options);
        }
    }

    private static Pricing getEveMarketData(PricingOptions options) {
        if (eveMarketData == null) {
            eveMarketData = new EveMarketData();
        } else {
            eveMarketData.resetAllAttemptCounters();
        }
        if (options != null) {
            eveMarketData.setOptions(options);
        }
        return eveMarketData;
    }
    private static Pricing getEveMarketeer(PricingOptions options) {
        if (eveMarketeer == null) {
            eveMarketeer = new EveMarketeer();
        } else {
            eveMarketeer.resetAllAttemptCounters();
        }
        if (options != null) {
            eveMarketeer.setOptions(options);
        }
        return eveMarketeer;
    }
    private static Pricing getEveCentral(PricingOptions options) {
        if (eveCentral == null) {
            eveCentral = new EveCentral();
        } else {
            eveCentral.resetAllAttemptCounters();
        }
        if (options != null) {
            eveCentral.setOptions(options);
        }
        return eveCentral;
    }
    private static Pricing getEveAddicts(PricingOptions options) {
        if (eveAddicts == null) {
            eveAddicts = new EveAddicts();
        } else {
            eveAddicts.resetAllAttemptCounters();
        }
        if (options != null) {
            eveAddicts.setOptions(options);
        }
        return eveAddicts;
    }
}
