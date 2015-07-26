package uk.me.candle.eve.pricing.options;

/**
 *
 * @author Niklas
 */
public enum PricingFetch {
    EVE_CENTRAL {
        @Override
        public PricingNumber[] getSupportedPricingNumbers() {
            return PricingNumber.values();
        }
        @Override
        public PricingType[] getSupportedPricingTypes() {
            return PricingType.values();
        }
    },
    EVE_MARKETDATA {
        @Override
        public PricingNumber[] getSupportedPricingNumbers() {
            return PricingNumber.values();
        }
        @Override
        public PricingType[] getSupportedPricingTypes() {
            PricingType[] pricingTypes = {PricingType.PERCENTILE};
            return pricingTypes;
        }
    },
    EVEMARKETEER {
        @Override
        public PricingNumber[] getSupportedPricingNumbers() {
            return PricingNumber.values();
        }
        @Override
        public PricingType[] getSupportedPricingTypes() {
            return PricingType.values();
        }
    },
    EVE_ADDICTS{
        @Override
        public PricingNumber[] getSupportedPricingNumbers() {
            return PricingNumber.values();
        }
        @Override
        public PricingType[] getSupportedPricingTypes() {
            PricingType[] pricingTypes = {PricingType.MEAN, PricingType.PERCENTILE};
            return pricingTypes;
        }
    };
    public abstract PricingNumber[] getSupportedPricingNumbers();
    public abstract PricingType[] getSupportedPricingTypes();
    
}
