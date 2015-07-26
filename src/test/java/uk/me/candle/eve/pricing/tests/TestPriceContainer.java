package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">

import static org.junit.Assert.*;
import org.junit.Test;
import uk.me.candle.eve.pricing.PriceContainer;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class TestPriceContainer {

    @Test
    public void testPut() {
        PriceContainer pc = new PriceContainer.PriceContainerBuilder()
                .putPrice(PricingType.HIGH, PricingNumber.BUY, 42)
                .build();

        assertEquals(42, pc.getPrice(PricingType.HIGH, PricingNumber.BUY), 0.0001);
    }
}
