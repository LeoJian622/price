package uk.me.candle.eve.pricing;

// <editor-fold defaultstate="collapsed" desc="imports">

import uk.me.candle.eve.pricing.options.PricingOptions;
import uk.me.candle.eve.pricing.impl.EveCentral;
import uk.me.candle.eve.pricing.impl.EveMarketData;
import uk.me.candle.eve.pricing.impl.EveMetrics;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class PricingFactory {
    static PricingOptions options;
    static EveCentral eveCentral;
    static EveMetrics eveMetrics;
    static EveMarketData eveMarketData;

    public static Pricing getPricing() {
        if (options == null) {
            options = new DefaultPricingOptions();
        }
        String impl = options != null ? options.getPricingFetchImplementation() : null;
        if ("eve-central".equalsIgnoreCase(impl)) {
            return getEveCentral();
        } else if ("eve-metrics".equalsIgnoreCase(impl)
            || "eve-marketdata".equalsIgnoreCase(impl)) {
            return getEveMarketData();
        } else {
            return getEveMarketData();
        }
    }

    public static Pricing getEveMarketData() {
        if (eveMarketData == null) {
            eveMarketData = new EveMarketData();
            if (options != null) eveMarketData.setOptions(options);
        }
        return eveMarketData;
    }
    public static Pricing getEveMetrics() {
        if (eveMetrics == null) {
            eveMetrics = new EveMetrics();
            if (options != null) eveMetrics.setOptions(options);
        }
        return eveMetrics;
    }
    public static Pricing getEveCentral() {
        if (eveCentral == null) {
            eveCentral = new EveCentral();
            eveCentral.setOptions(options);
            if (options != null) eveCentral.setOptions(options);
        }
        return eveCentral;
    }

    public static void setPricingOptions(PricingOptions opts) {
        options = opts;
        if (eveMetrics != null) eveMetrics.setOptions(options);
        if (eveCentral != null) eveCentral.setOptions(options);
        if (eveMarketData != null) eveMarketData.setOptions(options);
    }
}
