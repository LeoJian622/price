/*
 * Copyright 2015-2016, Niklas Kyster Rasmussen, Flaming Candle
 *
 * This file is part of Price
 *
 * Price is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * Price is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Price; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package uk.me.candle.eve.pricing.tests;

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
import uk.me.candle.eve.pricing.impl.EveAddicts;
import uk.me.candle.eve.pricing.options.LocationType;
import uk.me.candle.eve.pricing.options.PricingFetch;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;


public class TestEveAddicts extends PricingTests {

    //@Test
    public void testPars() {
        Double.valueOf("12.163408477416");
        Double.valueOf("11.08");
    }

	//@Test
    public void testGetPriceOnlineRegion() {
        Pricing pricing = PricingFactory.getPricing(new DefaultPricingOptions() {
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
                return PricingFetch.EVE_ADDICTS;
            }
            
        });
        testAll(pricing);
    }
    
    //@Test
    public void testGetPriceOnlineStation() {
        Pricing pricing = PricingFactory.getPricing(new DefaultPricingOptions() {
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
                return PricingFetch.EVE_ADDICTS;
            }
        });
        testAll(pricing);
    }
	
    @Test
    public void testGetPriceFail() {
		System.out.println("Testing EVE_ADDICTS errors");
        final EveAddicts pricing = new EveAddictsEmptyDummy();
        pricing.setPricingOptions(new DefaultPricingOptions() {
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
                return PricingFetch.EVE_ADDICTS;
            }
        });
        pricing.setPrice(34, -1d);
        Set<Integer> failed = synchronousPriceFetch(pricing, 34);
		assertEquals(failed.size(), 1);
    }

    class EveAddictsEmptyDummy extends EveAddicts {

        public EveAddictsEmptyDummy() {
            super(1);
        }

        @Override
        protected Document getDocument(URL url) throws SocketTimeoutException, DocumentException, IOException {
			throw  new DocumentException("Test");
        }
    }
    
}
