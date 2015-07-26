package uk.me.candle.eve.pricing;

// <editor-fold defaultstate="collapsed" desc="imports">
// </editor-fold>
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
