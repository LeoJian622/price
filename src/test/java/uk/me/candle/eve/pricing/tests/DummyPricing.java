package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import uk.me.candle.eve.pricing.AbstractPricing;
import uk.me.candle.eve.pricing.PriceContainer;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class DummyPricing extends AbstractPricing {

    @Override
    protected PriceContainer fetchPrice(int itemID) {
        return fetchPrices(Collections.singletonList(itemID)).get(itemID);
    }

    @Override
    protected Map<Integer, PriceContainer> fetchPrices(Collection<Integer> itemIDs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
