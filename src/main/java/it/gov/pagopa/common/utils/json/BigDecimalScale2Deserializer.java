package it.gov.pagopa.common.utils.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalScale2Deserializer extends ValueDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt){
        String value = p.getString();
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

}