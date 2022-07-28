package it.gov.pagopa.reward.drools.utils;

import it.gov.pagopa.reward.drools.model.DroolsRuleTemplateParam;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

public class DroolsTemplateRuleUtilsTest {

    @Test
    void testToTemplateParam() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.toTemplateParam(null));

        DroolsRuleTemplateParam templateParam = new DroolsRuleTemplateParam("asd");
        Assertions.assertEquals(templateParam, DroolsTemplateRuleUtils.toTemplateParam(templateParam));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildStringDroolsParam(null));
        String stringObject = "asd";
        DroolsRuleTemplateParam expectedString = new DroolsRuleTemplateParam("\"asd\"");
        Assertions.assertEquals(expectedString, DroolsTemplateRuleUtils.buildStringDroolsParam(stringObject));
        Assertions.assertEquals(expectedString, DroolsTemplateRuleUtils.toTemplateParam(stringObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildCollectionDroolsParam(null));
        Set<String> setObject = new HashSet<>();
        DroolsRuleTemplateParam expectedSet = new DroolsRuleTemplateParam("new java.util.HashSet(java.util.Arrays.asList())");
        Assertions.assertEquals(expectedSet, DroolsTemplateRuleUtils.buildCollectionDroolsParam(setObject));
        Assertions.assertEquals(expectedSet, DroolsTemplateRuleUtils.toTemplateParam(setObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildCollectionDroolsParam(null));
        List<String> listObject = new ArrayList<>();
        DroolsRuleTemplateParam expectedList = new DroolsRuleTemplateParam("new java.util.ArrayList(java.util.Arrays.asList())");
        Assertions.assertEquals(expectedList, DroolsTemplateRuleUtils.buildCollectionDroolsParam(listObject));
        Assertions.assertEquals(expectedList, DroolsTemplateRuleUtils.toTemplateParam(listObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildLocalDateDroolsParam(null));
        LocalDate localDateObject = LocalDate.of(2000, 1, 2);
        DroolsRuleTemplateParam expectedLocalDate = new DroolsRuleTemplateParam("java.time.LocalDate.of(2000,1,2)");
        Assertions.assertEquals(expectedLocalDate, DroolsTemplateRuleUtils.buildLocalDateDroolsParam(localDateObject));
        Assertions.assertEquals(expectedLocalDate, DroolsTemplateRuleUtils.toTemplateParam(localDateObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildLocalTimeDroolsParam(null));
        LocalTime localTimeObject = LocalTime.of(1, 5, 4);
        DroolsRuleTemplateParam expectedLocalTime = new DroolsRuleTemplateParam("java.time.LocalTime.of(1,5,4,0)");
        Assertions.assertEquals(expectedLocalTime, DroolsTemplateRuleUtils.buildLocalTimeDroolsParam(localTimeObject));
        Assertions.assertEquals(expectedLocalTime, DroolsTemplateRuleUtils.toTemplateParam(localTimeObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildLocalDateTimeDroolsParam(null));
        LocalDateTime localDateTimeObject = LocalDateTime.of(localDateObject, localTimeObject);
        DroolsRuleTemplateParam expectedLocalDateTime = new DroolsRuleTemplateParam("java.time.LocalDateTime.of(java.time.LocalDate.of(2000,1,2), java.time.LocalTime.of(1,5,4,0))");
        Assertions.assertEquals(expectedLocalDateTime, DroolsTemplateRuleUtils.buildLocalDateTimeDroolsParam(localDateTimeObject));
        Assertions.assertEquals(expectedLocalDateTime, DroolsTemplateRuleUtils.toTemplateParam(localDateTimeObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildZoneOffsetDroolsParam(null));
        ZoneOffset zoneOffsetObject = ZoneOffset.ofHours(0);
        DroolsRuleTemplateParam expectedZoneOffset = new DroolsRuleTemplateParam("java.time.ZoneOffset.of(\"Z\")");
        Assertions.assertEquals(expectedZoneOffset, DroolsTemplateRuleUtils.buildZoneOffsetDroolsParam(zoneOffsetObject));
        Assertions.assertEquals(expectedZoneOffset, DroolsTemplateRuleUtils.toTemplateParam(zoneOffsetObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildZoneIdDroolsParam(null));
        ZoneId zoneIdObject = ZoneId.of(zoneOffsetObject.toString());
        DroolsRuleTemplateParam expectedZoneId = new DroolsRuleTemplateParam("java.time.ZoneId.of(\"Z\")");
        Assertions.assertEquals(expectedZoneId, DroolsTemplateRuleUtils.buildZoneIdDroolsParam(zoneIdObject));
        Assertions.assertEquals(expectedZoneOffset, DroolsTemplateRuleUtils.toTemplateParam(zoneIdObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildOffsetDateTimeDroolsParam(null));
        OffsetDateTime offsetDateTimeObject = OffsetDateTime.of(localDateObject, localTimeObject, zoneOffsetObject);
        DroolsRuleTemplateParam expectedOffsetDateTime = new DroolsRuleTemplateParam("java.time.OffsetDateTime.of(java.time.LocalDate.of(2000,1,2), java.time.LocalTime.of(1,5,4,0), java.time.ZoneOffset.of(\"Z\"))");
        Assertions.assertEquals(expectedOffsetDateTime, DroolsTemplateRuleUtils.buildOffsetDateTimeDroolsParam(offsetDateTimeObject));
        Assertions.assertEquals(expectedOffsetDateTime, DroolsTemplateRuleUtils.toTemplateParam(offsetDateTimeObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildZonedDateTimeDroolsParam(null));
        ZonedDateTime zonedDateTimeObject = ZonedDateTime.of(localDateObject, localTimeObject, zoneIdObject);
        DroolsRuleTemplateParam expectedZonedDateTime = new DroolsRuleTemplateParam("java.time.ZonedDateTime.of(java.time.LocalDate.of(2000,1,2), java.time.LocalTime.of(1,5,4,0), java.time.ZoneId.of(\"Z\"))");
        Assertions.assertEquals(expectedZonedDateTime, DroolsTemplateRuleUtils.buildZonedDateTimeDroolsParam(zonedDateTimeObject));
        Assertions.assertEquals(expectedZonedDateTime, DroolsTemplateRuleUtils.toTemplateParam(zonedDateTimeObject));

        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildNewObjectDroolsParam(null));
        ExtraFilterTestModelSample newObjectObject = new ExtraFilterTestModelSample();
        DroolsRuleTemplateParam expectedNewObject = new DroolsRuleTemplateParam("((%s)(new java.util.function.Supplier<%s>(){public %s get(){%s varExtraFilterTestModelSample = new %s();SETTERS;return varExtraFilterTestModelSample;}}).get())".replace("%s", ExtraFilterTestModelSample.class.getName().replace('$', '.')));
        Set<String> expectedNewObjectSetters = Set.of(
                "varExtraFilterTestModelSample.setZonedDateTimeObject(null)",
                "varExtraFilterTestModelSample.setZoneOffsetObject(null)",
                "varExtraFilterTestModelSample.setLocalDateTimeObject(null)",
                "varExtraFilterTestModelSample.setCollectionObject(null)",
                "varExtraFilterTestModelSample.setOffsetDateTimeObject(null)",
                "varExtraFilterTestModelSample.setLocalDateObject(null)",
                "varExtraFilterTestModelSample.setLocalTimeObject(null)",
                "varExtraFilterTestModelSample.setZoneIdObject(null)",
                "varExtraFilterTestModelSample.setNumberObject(null)",
                "varExtraFilterTestModelSample.setBooleanObject(null)",
                "varExtraFilterTestModelSample.setStringObject(null)",
                "varExtraFilterTestModelSample.setEnumObject(null)",
                "return" // put for commodity
        );
        String newObjectBuild = DroolsTemplateRuleUtils.buildNewObjectDroolsParam(newObjectObject).getParam();
        String newObjectToTemplateParam = DroolsTemplateRuleUtils.toTemplateParam(newObjectObject).getParam();
        String setterRegexp = "varExtraFilterTestModelSample\\.set.*;return";
        String replaceString = "SETTERS;return";
        Assertions.assertEquals(expectedNewObject.getParam(), newObjectBuild.replaceAll(setterRegexp, replaceString));
        Assertions.assertEquals(expectedNewObject.getParam(), newObjectToTemplateParam.replaceAll(setterRegexp, replaceString));
        String setterMatchGroup = ".*\\(\\);(%s).*".formatted(setterRegexp);
        Assertions.assertEquals(expectedNewObjectSetters, Set.of(newObjectBuild.replaceFirst(setterMatchGroup, "$1").split(";")));
        Assertions.assertEquals(expectedNewObjectSetters, Set.of(newObjectToTemplateParam.replaceFirst(setterMatchGroup, "$1").split(";")));
    }

    @Data
    public static class ExtraFilterTestModelSample {
        private String stringObject;
        private Number numberObject;
        private DayOfWeek enumObject;
        private Collection<String> collectionObject;
        private LocalDate localDateObject;
        private LocalTime localTimeObject;
        private LocalDateTime localDateTimeObject;
        private ZoneOffset zoneOffsetObject;
        private ZoneId zoneIdObject;
        private ZonedDateTime zonedDateTimeObject;
        private OffsetDateTime offsetDateTimeObject;
        private Boolean booleanObject;
    }
}
