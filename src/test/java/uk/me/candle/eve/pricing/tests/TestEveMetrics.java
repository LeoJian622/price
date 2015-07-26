package uk.me.candle.eve.pricing.tests;

// <editor-fold defaultstate="collapsed" desc="imports">
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
import uk.me.candle.eve.pricing.impl.EveMetrics;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;

// </editor-fold>
/**
 *
 * @author Candle
 */
public class TestEveMetrics extends PricingTests {

    @Test
    public void testGetPriceGlobal1() {
        EveMetricsDummy dummyPricing = new EveMetricsDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.emptyList();
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 34, PricingType.LOW, PricingNumber.SELL);
        assertEquals(2.4, price, 0.001);
    }

    @Test
    public void testSetPrice() {
        EveMetricsDummy dummyPricing = new EveMetricsDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.emptyList();
            }
        });
        dummyPricing.setPrice(34, 4.0);

        Double price = dummyPricing.getPrice(34);
        if (price == null) fail("The price was null after having been manually set.");
        assertEquals(price.doubleValue(), 4.0, 0.00001);
    }

    @Test
    public void testGetPriceGlobal2() {
        EveMetricsDummy dummyPricing = new EveMetricsDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.emptyList();
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 35, PricingType.LOW, PricingNumber.SELL);
        assertEquals(3.24, price, 0.001);
    }

    @Test
    public void testGetPriceRegion1() {
        EveMetricsDummy dummyPricing = new EveMetricsDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.singletonList(10000002L);
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 34, PricingType.LOW, PricingNumber.SELL);
        assertEquals(4.0, price, 0.001);
    }

    @Test
    public void testGetPriceRegion2() {
        EveMetricsDummy dummyPricing = new EveMetricsDummy();
        dummyPricing.setOptions(new DummyPricingOptions() {
            @Override
            public List<Long> getRegions() {
                return Collections.singletonList(10000002L);
            }
        });
        double price = synchronousPriceFetch(dummyPricing, 34, PricingType.HIGH, PricingNumber.BUY);
        assertEquals(4.21, price, 0.001);
    }

    @Test
    public void testGetPriceGlobalList() {
        EveMetricsDummy dummyPricing = new EveMetricsDummy();
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

        assertEquals(2.4, prices.get(34).doubleValue(), 0.001);
        assertEquals(3.24, prices.get(35).doubleValue(), 0.001);
    }

    /**
     * This dummy class just replaces the getDocument() function so that test data does not change and does not require an internet connection.
     */
    class EveMetricsDummy extends EveMetrics {

        @Override
        protected Document getDocument(URL url) throws SocketTimeoutException, DocumentException, IOException {

            String resourceName = "uk/me/candle/eve/pricing/tests/eve-metrics.xml";
            InputStream input = EveMetrics.class.getClassLoader().getResourceAsStream(resourceName);

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
