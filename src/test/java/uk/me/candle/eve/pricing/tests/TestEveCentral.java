package uk.me.candle.eve.pricing.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.junit.Test;
import static org.junit.Assert.*;
import uk.me.candle.eve.pricing.impl.EveCentral;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

/**
 *
 * @author Candle
 */
public class TestEveCentral extends PricingTests {

    @Test
    public void testGetPriceGlobal1() {
        EveCentralDummy dummyPricing = new EveCentralDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.emptyList();
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 34, PricingType.LOW, PricingNumber.SELL);
        assertEquals(1.75, price, 0.001);
    }

    @Test
    public void testGetPriceGlobal2() {
        EveCentralDummy dummyPricing = new EveCentralDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.emptyList();
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 35, PricingType.LOW, PricingNumber.SELL);
        assertEquals(1.5, price, 0.001);
    }

    @Test
    public void testGetPriceRegion1() {
        EveCentralDummy dummyPricing = new EveCentralDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.singletonList(10000002L);
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 34, PricingType.LOW, PricingNumber.SELL);
        assertEquals(2.65, price, 0.001);
    }

    @Test
    public void testGetPriceRegion2() {
        EveCentralDummy dummyPricing = new EveCentralDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.singletonList(10000002L);
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 34, PricingType.HIGH, PricingNumber.BUY);
        assertEquals(2.75, price, 0.001);
    }

    @Test
    public void testGetPriceRegion2Mean() {
        EveCentralDummy dummyPricing = new EveCentralDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.singletonList(10000002L);
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 34, PricingType.MEAN, PricingNumber.BUY);
        assertEquals(2.61422340506, price
                   , 0.000001);
    }

    @Test
    public void testGetPriceGlobalList() {
        EveCentralDummy dummyPricing = new EveCentralDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.emptyList();
            }
        });
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(34);
        ids.add(35);
        Map<Integer, Double> prices = synchronousPriceFetch(dummyPricing, ids, PricingType.LOW, PricingNumber.SELL);

        assertEquals(1.75, prices.get(34).doubleValue(), 0.001);
        assertEquals(1.5, prices.get(35).doubleValue(), 0.001);
    }

    /**
     * This dummy class just replaces the getDocument() function so that test data does not change and does not require an internet connection.
     */
    class EveCentralDummy extends EveCentral {

        @Override
        protected Document getDocument(URL url) throws SocketTimeoutException, DocumentException, IOException {
            String[] queryBits = url.getQuery().split("&");
            List<Integer> typeIDs = new ArrayList<Integer>();
            List<Long> regionIDs = new ArrayList<Long>();

            for (String queryBit : queryBits) {
                String[] parts = queryBit.split("=");
                if (parts[0].equalsIgnoreCase("typeid")) {
                    typeIDs.add(Integer.parseInt(parts[1]));
                } else if (parts[0].equalsIgnoreCase("regionlimit")) {
                    regionIDs.add(Long.parseLong(parts[1]));
                }
            }

            StringBuilder resourceName = new StringBuilder("uk/me/candle/eve/pricing/tests/eve-central");

            for (Integer i : typeIDs) {
                resourceName.append("-");
                resourceName.append(i);
            }

            resourceName.append("_");

            boolean comma = false;
            for (Long i : regionIDs) {
                resourceName.append(i);
                if (comma) resourceName.append("-");
                comma = true;
            }

            resourceName.append(".xml");

            String res = resourceName.toString();
            InputStream input = EveCentral.class.getClassLoader().getResourceAsStream(res);

            if (input == null) {
                fail(res + " does not exist");
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(input));
            StringBuilder sb = new StringBuilder();
            String line;
            while (null != (line = br.readLine())) {
                sb.append(line);
            }
            br.close();

            Document d = DocumentHelper.parseText(sb.toString());

            return d;
        }
    }
}
