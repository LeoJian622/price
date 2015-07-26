package uk.me.candle.eve.pricing.tests;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import uk.me.candle.eve.pricing.Pricing;
import uk.me.candle.eve.pricing.PricingFactory;
import uk.me.candle.eve.pricing.impl.EveCentral;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;

/**
 *
 * @author Candle
 */
public class TestEveCentral extends PricingTests {

    private static final Integer[] OK_FAILED = new Integer[2];
    @Before
	public void setUp() {
        OK_FAILED[0] = 33578;
        OK_FAILED[1] = 33579;
	}

    @Test
    public void testGetPriceOnlineRegions() {
        //Empire
        final List<Long> locations = new ArrayList<Long>();
        //Amarr
        locations.add(10000054L); //Amarr: Aridia
        locations.add(10000036L); //Amarr: Devoid
        locations.add(10000043L); //Amarr: Domain
        locations.add(10000067L); //Amarr: Genesis
        locations.add(10000052L); //Amarr: Kador
        locations.add(10000065L); //Amarr: Kor-Azor
        locations.add(10000020L); //Amarr: Tash-Murkon
        locations.add(10000038L); //Amarr: The Bleak Lands
        //Caldari
        locations.add(10000069L); //Caldari: Black Rise
        locations.add(10000016L); //Caldari: Lonetrek
        locations.add(10000033L); //Caldari: The Citadel
        locations.add(10000002L); //Caldari: The Forge
        //Gallente
        locations.add(10000064L); //Gallente: Essence
        locations.add(10000037L); //Gallente: Everyshore
        locations.add(10000048L); //Gallente: Placid
        locations.add(10000032L); //Gallente: Sinq Laison
        locations.add(10000044L); //Gallente: Solitude
        locations.add(10000068L); //Gallente: Verge Vendor
        //Minmatar
        locations.add(10000042L); //Minmatar : Metropolis
        locations.add(10000030L); //Minmatar : Heimatar
        locations.add(10000028L); //Minmatar : Molden Heath
        //Others
        locations.add(10000001L); //Ammatar: Derelik
        locations.add(10000049L); //Khanid: Khanid

        Pricing pricing = PricingFactory.getPricing(new DummyPricingOptions() {
            @Override public List<Long> getLocations() {
                return locations;
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.REGION;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.EVE_CENTRAL;
            }
        });
        testAll(pricing, OK_FAILED);
    }
    @Test
    public void testGetPriceOnlineRegion() {
        Pricing pricing = PricingFactory.getPricing(new DummyPricingOptions() {
            @Override public List<Long> getLocations() {
                return Collections.singletonList(10000002L);
            }
            @Override
            public LocationType getLocationType() {
                return LocationType.REGION;
            }
            @Override
            public PricingFetch getPricingFetchImplementation() {
                return PricingFetch.EVE_CENTRAL;
            }
        });
        testAll(pricing, OK_FAILED);
    }
    
    @Test
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
                return PricingFetch.EVE_CENTRAL;
            }
        });
        testAll(pricing, OK_FAILED);
    }

    @Test
    public void testGetPriceFail() {
        System.out.println("EveCentral Fail Test");
        final EveCentral dummyPricing = new EveCentralEmptyDummy();
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

	class EveCentralEmptyDummy extends EveCentral {
        @Override
        protected Document getDocument(URL url) throws SocketTimeoutException, DocumentException, IOException {
			throw  new DocumentException("Test");
        }
    }
}
