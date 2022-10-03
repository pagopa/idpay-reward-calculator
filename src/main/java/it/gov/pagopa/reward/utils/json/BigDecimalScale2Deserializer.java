package it.gov.pagopa.reward.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalScale2Deserializer extends NumberDeserializers.BigDecimalDeserializer {
    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return super.deserialize(p, ctxt).setScale(2, RoundingMode.HALF_DOWN);
    }
}