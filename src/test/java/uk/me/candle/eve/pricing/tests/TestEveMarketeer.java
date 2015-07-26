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
import uk.me.candle.eve.pricing.impl.EveMarketeer;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class TestEveMarketeer extends PricingTests {

    //@Test
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
                return PricingFetch.EVEMARKETEER;
            }
        });
        testAll(pricing);
    }

    //@Test
    public void testGetPriceOnlineSystem() {
        Pricing pricing = PricingFactory.getPricing(new DummyPricingOptions() {
            @Override
            public List<Long> getLocations() {
                return Collections.singletonList(30000142L);
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.SYSTEM;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.EVEMARKETEER;
            }
        });
        testAll(pricing);
    }
    //@Test
    public void testGetPriceOnlineStation() {
        EveMarketeer pricing = new EveMarketeer();
        pricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getLocations() {
                return Collections.singletonList(60003760L);
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.STATION;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.EVEMARKETEER;
            }
        });
        testAll(pricing);
    }
	
	@Test
    public void testGetPriceFail() {
        System.out.println("EveMarketeer Fail Test");
        final EveMarketeer dummyPricing = new EveMarketeerEmptyDummy();
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

	class EveMarketeerEmptyDummy extends EveMarketeer {
        @Override
        protected Document getDocument(URL url) throws SocketTimeoutException, DocumentException, IOException {
			throw  new DocumentException("Test");
        }
    }
}
