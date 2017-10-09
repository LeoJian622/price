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
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.me.candle.eve.pricing.options.PricingNumber;
import uk.me.candle.eve.pricing.options.PricingType;


public abstract class AbstractPricingEasy extends AbstractPricing {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPricingEasy.class);

    public AbstractPricingEasy(int threads) {
        super(threads);
    }

    protected abstract URL getURL(Collection<Integer> itemIDs) throws 
            SocketTimeoutException, DocumentException, IOException;

    abstract protected Node getNode(Document d, int typeID, PricingType type, PricingNumber number);

    private PriceContainer extractPrice(Document d, Integer typeID) throws DocumentException {
		if (d == null) {
			throw new DocumentException("Document is null");
		}
        PriceContainer.PriceContainerBuilder builder = new PriceContainer.PriceContainerBuilder();
        for (PricingType type : PricingType.values()) {
            for (PricingNumber number : PricingNumber.values()) {
                Node node = getNode(d, typeID, type, number);
				Double price = getNodeValue(node);
				if (price != null) {
					builder.putPrice(type, number, price);
				}
            }
        }
        return builder.build();
    }

    private Double getNodeValue(Node node) {
        if (node == null) {
            return null;
        }
        try {
            return Double.valueOf(node.getStringValue()); //Sometimes return null
        } catch (NumberFormatException ex){
            return null;
        }
    }

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
            Document d = getDocument(url);
            for (Integer i : itemIDs) {
                PriceContainer price = extractPrice(d, i);
				if (price != null) {
					returnMap.put(i, price);
				}
            }
        } catch (SocketTimeoutException ste) {
            //Minor failure
            LOG.error("Timeout while fetching URL:" + url, ste);
            LOG.debug("Reducing the batch size by -1 from " + getBatchSize());
            addFailureReasons(itemIDs, ste.getMessage());
        } catch (DocumentException de) {
            //Critical failure
            LOG.error("Error fetching price", de);
            addFailureReasons(itemIDs, de.getMessage());
        } catch (IOException ioe) {
            //Critical failure
            LOG.error("Error fetching price", ioe);
            addFailureReasons(itemIDs, ioe.getMessage());
        }
        return returnMap;
    }
}
