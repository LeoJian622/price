package uk.me.candle.eve.pricing;

// <editor-fold defaultstate="collapsed" desc="imports">
// </editor-fold>
/**
 *
 * This listener is informed when the price fetching thread is active and fetching prices.
 *
 * @author Candle
 */
public interface PricingFetchListener {
    public void fetchStarted();
    public void fetchEnded();
}
