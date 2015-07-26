package uk.me.candle.eve.pricing.tests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import uk.me.candle.eve.pricing.options.impl.DefaultPricingOptions;

public class DummyPricingOptions extends DefaultPricingOptions {

    @Override
    public InputStream getCacheInputStream() throws IOException {
        return null;
    }

    @Override
    public OutputStream getCacheOutputStream() throws IOException {
        return null;
    }

    @Override
    public int getAttemptCount() {
        return 2;
    }

    @Override
    public boolean getCacheTimersEnabled() {
        return true;
    }
}
