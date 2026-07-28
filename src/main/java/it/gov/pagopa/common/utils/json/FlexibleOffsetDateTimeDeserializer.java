package it.gov.pagopa.common.utils.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * Accepts ISO datetime with offset (e.g. 2026-07-14T17:58:07.132+02:00)
 * and without offset (e.g. 2026-07-14T17:58:07.132), defaulting to Europe/Rome.
 */
public class FlexibleOffsetDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    public FlexibleOffsetDateTimeDeserializer() {
        super(OffsetDateTime.class);
    }

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext ctxt) {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(value, LOCAL_DATE_TIME_FORMATTER);
                return localDateTime.atZone(DEFAULT_ZONE).toOffsetDateTime();
            } catch (DateTimeParseException ex) {
                throw ctxt.weirdStringException(value, OffsetDateTime.class,
                        "Unsupported datetime format. Expected ISO_OFFSET_DATE_TIME or yyyy-MM-dd'T'HH:mm:ss[.fraction]");
            }
        }
    }
}
