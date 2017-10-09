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
package uk.me.candle.eve.pricing;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public abstract class AbstractPricingFast extends AbstractPricing {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPricingEasy.class);

    public AbstractPricingFast(int threads) {
        super(threads);
    }

    protected abstract URL getURL(Collection<Integer> itemIDs) throws 
            SocketTimeoutException, DocumentException, IOException;

    protected abstract Map<Integer, PriceContainer> extractPrices(Element element);

    @Override
    protected final PriceContainer fetchPrice(int itemID) {
        return fetchPrices(Collections.singletonList(itemID)).get(itemID);
    }

    @Override
    protected final Map<Integer, PriceContainer> fetchPrices(Collection<Integer> itemIDs) {
        LOG.info("getting " + itemIDs.size() + " prices");
        Map<Integer, PriceContainer> returnMap = new HashMap<Integer, PriceContainer>();
        //System.out.println("failed: " + failed.size());
        URL url = null;
        try {
            url = getURL(itemIDs);
            Element element = getElement(url);
            returnMap.putAll(extractPrices(element));
        } catch (SocketTimeoutException ex) {
            //Critical failure
            LOG.error("Error fetching price", ex);
            addFailureReasons(itemIDs, ex.getMessage());
        } catch (DocumentException ex) {
            //Critical failure
            LOG.error("Error fetching price", ex);
            addFailureReasons(itemIDs, ex.getMessage());
        } catch (ParserConfigurationException ex) {
            //Critical failure
            LOG.error("Error fetching price", ex);
            addFailureReasons(itemIDs, ex.getMessage());
        } catch (SAXException ex) {
            //Critical failure
            LOG.error("Error fetching price", ex);
            addFailureReasons(itemIDs, ex.getMessage());
        } catch (IOException ex) {
            //Critical failure
            LOG.error("Error fetching price", ex);
            addFailureReasons(itemIDs, ex.getMessage());
        }
        return returnMap;
    }
}
