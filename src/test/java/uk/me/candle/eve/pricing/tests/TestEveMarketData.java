package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import static org.junit.Assert.*;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import uk.me.candle.eve.pricing.impl.EveMarketData;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class TestEveMarketData extends PricingTests {

    @Test
    public void testGetPriceOnlineRegion() {
        Pricing pricing = PricingFactory.getPricing(new DummyPricingOptions() {
            @Override
            public List<Long> getLocations() {
                return Collections.singletonList(10000002L);
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.REGION;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.EVE_MARKETDATA;
            }
        });
        testAll(pricing);
    }
	
    @Test
    public void testGetPriceFail() {
        System.out.println("EveMarketData Fail Test");
        final EveMarketData dummyPricing = new EveMarketDataEmptyDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getLocations() {
                return Collections.emptyList();
            }
        });
        dummyPricing.setPrice(34, -1d);
        Set<Integer> failed = synchronousPriceFetch(dummyPricing, 34);
		assertEquals(failed.size(), 1);
    }

    class EveMarketDataEmptyDummy extends EveMarketData {
        @Override
        protected Document getDocument(URL url) throws SocketTimeoutException, DocumentException, IOException {
			throw  new DocumentException("Test");
        }
    }
}
