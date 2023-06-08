package it.gov.pagopa.common.drools.utils;

import it.gov.pagopa.common.drools.model.DroolsRuleTemplateParam;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

class DroolsTemplateRuleUtilsTest {

    @Test
    void testToTemplateParamFromNull() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.toTemplateParam(null));
    }

    @Test
    void testToTemplateParamFromTemplateParam() {
        DroolsRuleTemplateParam templateParam = new DroolsRuleTemplateParam("asd");
        Assertions.assertSame(templateParam, DroolsTemplateRuleUtils.toTemplateParam(templateParam));
    }

    @Test
    void testToTemplateParamFromString() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildStringDroolsParam(null));
        String stringObject = "asd";
        DroolsRuleTemplateParam expectedString = new DroolsRuleTemplateParam("\"asd\"");
        Assertions.assertEquals(expectedString, DroolsTemplateRuleUtils.buildStringDroolsParam(stringObject));
        Assertions.assertEquals(expectedString, DroolsTemplateRuleUtils.toTemplateParam(stringObject));
    }

    @Test
    void testToTemplateParamFromBoolean() {
        DroolsRuleTemplateParam expectedBoolean = new DroolsRuleTemplateParam("true");
        Assertions.assertEquals(expectedBoolean, DroolsTemplateRuleUtils.buildBooleanDroolsParam(true));
        Assertions.assertEquals(expectedBoolean, DroolsTemplateRuleUtils.toTemplateParam(true));

        expectedBoolean = new DroolsRuleTemplateParam("false");
        Assertions.assertEquals(expectedBoolean, DroolsTemplateRuleUtils.buildBooleanDroolsParam(false));
        Assertions.assertEquals(expectedBoolean, DroolsTemplateRuleUtils.toTemplateParam(false));
    }

    @Test
    void testToTemplateParamFromCollectionSet() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildCollectionDroolsParam(null));
        Set<String> setObject = new HashSet<>();
        DroolsRuleTemplateParam expectedSet = new DroolsRuleTemplateParam("new java.util.HashSet(java.util.Arrays.asList())");
        Assertions.assertEquals(expectedSet, DroolsTemplateRuleUtils.buildCollectionDroolsParam(setObject));
        Assertions.assertEquals(expectedSet, DroolsTemplateRuleUtils.toTemplateParam(setObject));
    }

    @Test
    void testToTemplateParamFromCollectionList() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildCollectionDroolsParam(null));
        List<String> listObject = new ArrayList<>();
        DroolsRuleTemplateParam expectedList = new DroolsRuleTemplateParam("new java.util.ArrayList(java.util.Arrays.asList())");
        Assertions.assertEquals(expectedList, DroolsTemplateRuleUtils.buildCollectionDroolsParam(listObject));
        Assertions.assertEquals(expectedList, DroolsTemplateRuleUtils.toTemplateParam(listObject));
    }

    private final LocalDate localDateObject = LocalDate.of(2000, 1, 2);
    @Test
    void testToTemplateParamFromLocalDate() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildLocalDateDroolsParam(null));
        DroolsRuleTemplateParam expectedLocalDate = new DroolsRuleTemplateParam("java.time.LocalDate.of(2000,1,2)");
        Assertions.assertEquals(expectedLocalDate, DroolsTemplateRuleUtils.buildLocalDateDroolsParam(localDateObject));
        Assertions.assertEquals(expectedLocalDate, DroolsTemplateRuleUtils.toTemplateParam(localDateObject));
    }

    private final LocalTime localTimeObject = LocalTime.of(1, 5, 4);
    @Test
    void testToTemplateParamFromLocalTime() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildLocalTimeDroolsParam(null));
        DroolsRuleTemplateParam expectedLocalTime = new DroolsRuleTemplateParam("java.time.LocalTime.of(1,5,4,0)");
        Assertions.assertEquals(expectedLocalTime, DroolsTemplateRuleUtils.buildLocalTimeDroolsParam(localTimeObject));
        Assertions.assertEquals(expectedLocalTime, DroolsTemplateRuleUtils.toTemplateParam(localTimeObject));
    }

    @Test
    void testToTemplateParamFromLocalDateTime() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildLocalDateTimeDroolsParam(null));
        LocalDateTime localDateTimeObject = LocalDateTime.of(localDateObject, localTimeObject);
        DroolsRuleTemplateParam expectedLocalDateTime = new DroolsRuleTemplateParam("java.time.LocalDateTime.of(java.time.LocalDate.of(2000,1,2), java.time.LocalTime.of(1,5,4,0))");
        Assertions.assertEquals(expectedLocalDateTime, DroolsTemplateRuleUtils.buildLocalDateTimeDroolsParam(localDateTimeObject));
        Assertions.assertEquals(expectedLocalDateTime, DroolsTemplateRuleUtils.toTemplateParam(localDateTimeObject));
    }

    private final ZoneOffset zoneOffsetObject = ZoneOffset.ofHours(0);
    private final DroolsRuleTemplateParam expectedZoneOffset = new DroolsRuleTemplateParam("java.time.ZoneOffset.of(\"Z\")");
    @Test
    void testToTemplateParamFromZoneOffset() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildZoneOffsetDroolsParam(null));
        Assertions.assertEquals(expectedZoneOffset, DroolsTemplateRuleUtils.buildZoneOffsetDroolsParam(zoneOffsetObject));
        Assertions.assertEquals(expectedZoneOffset, DroolsTemplateRuleUtils.toTemplateParam(zoneOffsetObject));
    }

    private final ZoneId zoneIdObject = ZoneId.of(zoneOffsetObject.toString());
    @Test
    void testToTemplateParamFromZoneId() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildZoneIdDroolsParam(null));
        DroolsRuleTemplateParam expectedZoneId = new DroolsRuleTemplateParam("java.time.ZoneId.of(\"Z\")");
        Assertions.assertEquals(expectedZoneId, DroolsTemplateRuleUtils.buildZoneIdDroolsParam(zoneIdObject));
        Assertions.assertEquals(expectedZoneOffset, DroolsTemplateRuleUtils.toTemplateParam(zoneIdObject));
    }

    @Test
    void testToTemplateParamFromOffsetDateTime() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildOffsetDateTimeDroolsParam(null));
        OffsetDateTime offsetDateTimeObject = OffsetDateTime.of(localDateObject, localTimeObject, zoneOffsetObject);
        DroolsRuleTemplateParam expectedOffsetDateTime = new DroolsRuleTemplateParam("java.time.OffsetDateTime.of(java.time.LocalDate.of(2000,1,2), java.time.LocalTime.of(1,5,4,0), java.time.ZoneOffset.of(\"Z\"))");
        Assertions.assertEquals(expectedOffsetDateTime, DroolsTemplateRuleUtils.buildOffsetDateTimeDroolsParam(offsetDateTimeObject));
        Assertions.assertEquals(expectedOffsetDateTime, DroolsTemplateRuleUtils.toTemplateParam(offsetDateTimeObject));
    }

    @Test
    void testToTemplateParamFromZonedDateTime() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildZonedDateTimeDroolsParam(null));
        ZonedDateTime zonedDateTimeObject = ZonedDateTime.of(localDateObject, localTimeObject, zoneIdObject);
        DroolsRuleTemplateParam expectedZonedDateTime = new DroolsRuleTemplateParam("java.time.ZonedDateTime.of(java.time.LocalDate.of(2000,1,2), java.time.LocalTime.of(1,5,4,0), java.time.ZoneId.of(\"Z\"))");
        Assertions.assertEquals(expectedZonedDateTime, DroolsTemplateRuleUtils.buildZonedDateTimeDroolsParam(zonedDateTimeObject));
        Assertions.assertEquals(expectedZonedDateTime, DroolsTemplateRuleUtils.toTemplateParam(zonedDateTimeObject));
    }

    @Test
    void testToTemplateParamFromCustomObject() {
        Assertions.assertEquals(DroolsTemplateRuleUtils.NULL_TEMPLATE_PARAM, DroolsTemplateRuleUtils.buildNewObjectDroolsParam(null));
        TestModelSample newObjectObject = new TestModelSample();
        DroolsRuleTemplateParam expectedNewObject = new DroolsRuleTemplateParam("((%s)(new java.util.function.Supplier<%s>(){public %s get(){%s varTestModelSample = new %s();SETTERS;return varTestModelSample;}}).get())".replace("%s", TestModelSample.class.getName().replace('$', '.')));
        Set<String> expectedNewObjectSetters = new TreeSet<>(Set.of(
                "varTestModelSample.setZonedDateTimeObject(null)",
                "varTestModelSample.setZoneOffsetObject(null)",
                "varTestModelSample.setLocalDateTimeObject(null)",
                "varTestModelSample.setCollectionObject(null)",
                "varTestModelSample.setOffsetDateTimeObject(null)",
                "varTestModelSample.setLocalDateObject(null)",
                "varTestModelSample.setLocalTimeObject(null)",
                "varTestModelSample.setZoneIdObject(null)",
                "varTestModelSample.setNumberObject(null)",
                "varTestModelSample.setBooleanObject(null)",
                "varTestModelSample.setStringObject(null)",
                "varTestModelSample.setEnumObject(null)",
                "return" // put for commodity
        ));
        String newObjectBuild = DroolsTemplateRuleUtils.buildNewObjectDroolsParam(newObjectObject).getParam();
        String newObjectToTemplateParam = DroolsTemplateRuleUtils.toTemplateParam(newObjectObject).getParam();
        String setterRegexp = "varTestModelSample\\.set.*;return";
        String replaceString = "SETTERS;return";
        Assertions.assertEquals(expectedNewObject.getParam(), newObjectBuild.replaceAll(setterRegexp, replaceString));
        Assertions.assertEquals(expectedNewObject.getParam(), newObjectToTemplateParam.replaceAll(setterRegexp, replaceString));
        String setterMatchGroup = ".*\\(\\);(%s).*".formatted(setterRegexp);
        Assertions.assertEquals(expectedNewObjectSetters, new TreeSet<>(Set.of(newObjectBuild.replaceFirst(setterMatchGroup, "$1").split(";"))));
        Assertions.assertEquals(expectedNewObjectSetters, new TreeSet<>(Set.of(newObjectToTemplateParam.replaceFirst(setterMatchGroup, "$1").split(";"))));
    }

    @Data
    static class TestModelSample {
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

        @SuppressWarnings("unused")
        public void nonSetterMethod(){}

        @SuppressWarnings("unused")
        public void setStringObject(int x){
            // non standard setter example
            this.stringObject=x+"";
        }
        @SuppressWarnings("unused")
        public void setStringObject(String x){
            this.stringObject=x;
        }
    }

    @Test
    void testToTemplateParamFromCustomObjectWithNoSetters() {
        Assertions.assertEquals("((it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtilsTest.TestModelSampleNoSetters)(new java.util.function.Supplier<it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtilsTest.TestModelSampleNoSetters>(){public it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtilsTest.TestModelSampleNoSetters get(){it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtilsTest.TestModelSampleNoSetters varTestModelSampleNoSetters = new it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtilsTest.TestModelSampleNoSetters();return varTestModelSampleNoSetters;}}).get())"
                , DroolsTemplateRuleUtils.buildNewObjectDroolsParam(new TestModelSampleNoSetters()).getParam());
    }

    static class TestModelSampleNoSetters {
        @Getter
        private String fieldNoSetter;
    }

    @Test
    void testToTemplateParamFromCustomObjectWithNoGetters() {
        TestModelSampleNoGetters object = new TestModelSampleNoGetters();
        try{
            DroolsTemplateRuleUtils.buildNewObjectDroolsParam(object);
            Assertions.fail("Expected exception");
        } catch (IllegalStateException e){
            // Do nothing
        }
    }

    static class TestModelSampleNoGetters {
        @Setter
        private String fieldNoGetter;
    }
}
