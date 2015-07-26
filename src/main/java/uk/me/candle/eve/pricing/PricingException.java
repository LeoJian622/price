package uk.me.candle.eve.pricing;

/**
 *
 * @author Candle
 */
public class PricingException extends RuntimeException {
    private static final long serialVersionUID = 1l;

    public PricingException(Throwable cause) {
        super(cause);
    }

    public PricingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PricingException(String message) {
        super(message);
    }

    public PricingException() {
    }
}
