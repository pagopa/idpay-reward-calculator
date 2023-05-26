package it.gov.pagopa.common.drools.utils;

import it.gov.pagopa.common.drools.model.DroolsRuleTemplateParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities to build Drools rules template to build
 */
@Slf4j
public final class DroolsTemplateRuleUtils {
    private DroolsTemplateRuleUtils() {
    }

    public static final DroolsRuleTemplateParam NULL_TEMPLATE_PARAM = new DroolsRuleTemplateParam("null");

    /**
     * To build a String containing the logic to instantiate a new LocalDate copy of the input
     */
    public static DroolsRuleTemplateParam toTemplateParam(Object o) {
        if (o == null) {
            return NULL_TEMPLATE_PARAM;
        } else if (o instanceof DroolsRuleTemplateParam droolsRuleTemplateParam) {
            return droolsRuleTemplateParam;
        }  else if (o instanceof String string) {
            return buildStringDroolsParam(string);
        }  else if (o instanceof Enum) {
            return buildValueOfStringDroolsParam(o.getClass(), o);
        } else if (o instanceof Number) {
            return buildNewObjectFromStringDroolsParam(o.getClass(), o);
        } else if (o instanceof Collection) {
            return buildCollectionDroolsParam((Collection<?>)o);
        } else if (o instanceof LocalDate localDate) {
            return buildLocalDateDroolsParam(localDate);
        } else if (o instanceof LocalTime localTime) {
            return buildLocalTimeDroolsParam(localTime);
        } else if (o instanceof LocalDateTime localDateTime) {
            return buildLocalDateTimeDroolsParam(localDateTime);
        } else if (o instanceof ZoneOffset zoneOffset) {
            return buildZoneOffsetDroolsParam(zoneOffset);
        } else if (o instanceof ZoneId zoneId) {
            return buildZoneIdDroolsParam(zoneId);
        } else if (o instanceof ZonedDateTime zonedDateTime) {
            return buildZonedDateTimeDroolsParam(zonedDateTime);
        } else if (o instanceof OffsetDateTime offsetDateTime) {
            return buildOffsetDateTimeDroolsParam(offsetDateTime);
        } else if (o instanceof Boolean booleanObject) {
            return buildBooleanDroolsParam(booleanObject);
        }  else {
            return buildNewObjectDroolsParam(o);
        }
    }

    public static DroolsRuleTemplateParam buildBooleanDroolsParam(Boolean o) {
        return new DroolsRuleTemplateParam(o.toString());
    }

    /**
     * To build a String containing the logic to instantiate a new String copy of the input
     */
    public static DroolsRuleTemplateParam buildStringDroolsParam(String string) {
        if (string == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("\"%s\"", StringEscapeUtils.escapeJava(string)));
    }

    /**
     * To build a String containing the logic to instantiate a new LocalDate copy of the input
     */
    public static DroolsRuleTemplateParam buildLocalDateDroolsParam(LocalDate localDate) {
        if (localDate == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("%s.of(%d,%d,%d)", LocalDate.class.getName(), localDate.getYear(), localDate.getMonth().getValue(), localDate.getDayOfMonth()));
    }

    /**
     * To build a String containing the logic to instantiate a new LocalTime copy of the input
     */
    public static DroolsRuleTemplateParam buildLocalTimeDroolsParam(LocalTime localTime) {
        if (localTime == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("%s.of(%d,%d,%d,%d)", LocalTime.class.getName(), localTime.getHour(), localTime.getMinute(), localTime.getSecond(), localTime.getNano()));
    }

    /**
     * To build a String containing the logic to instantiate a new ZoneId copy of the input
     */
    public static DroolsRuleTemplateParam buildZoneIdDroolsParam(ZoneId zoneId) {
        if (zoneId == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("%s.of(%s)", ZoneId.class.getName(), buildStringDroolsParam(zoneId.getId())));
    }

    /**
     * To build a String containing the logic to instantiate a new ZoneOffset copy of the input
     */
    public static DroolsRuleTemplateParam buildZoneOffsetDroolsParam(ZoneOffset zoneOffset) {
        if (zoneOffset == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("%s.of(%s)", ZoneOffset.class.getName(), buildStringDroolsParam(zoneOffset.getId())));
    }

    /**
     * To build a String containing the logic to instantiate a new LocalDateTime copy of the input
     */
    public static DroolsRuleTemplateParam buildLocalDateTimeDroolsParam(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("%s.of(%s, %s)", LocalDateTime.class.getName(), buildLocalDateDroolsParam(localDateTime.toLocalDate()).getParam(), buildLocalTimeDroolsParam(localDateTime.toLocalTime()).getParam()));
    }

    /**
     * To build a String containing the logic to instantiate a new ZonedDateTime copy of the input
     */
    public static DroolsRuleTemplateParam buildZonedDateTimeDroolsParam(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("%s.of(%s, %s, %s)", ZonedDateTime.class.getName(), buildLocalDateDroolsParam(zonedDateTime.toLocalDate()).getParam(), buildLocalTimeDroolsParam(zonedDateTime.toLocalTime()).getParam(), buildZoneIdDroolsParam(zonedDateTime.getZone()).getParam()));
    }

    /**
     * To build a String containing the logic to instantiate a new OffsetDateTime copy of the input
     */
    public static DroolsRuleTemplateParam buildOffsetDateTimeDroolsParam(OffsetDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return NULL_TEMPLATE_PARAM;
        }
        return new DroolsRuleTemplateParam(String.format("%s.of(%s, %s, %s)", OffsetDateTime.class.getName(), buildLocalDateDroolsParam(zonedDateTime.toLocalDate()).getParam(), buildLocalTimeDroolsParam(zonedDateTime.toLocalTime()).getParam(), buildZoneOffsetDroolsParam(zonedDateTime.getOffset()).getParam()));
    }

    /**
     * To build a String containing the logic to instantiate a new List copy of the input
     */
    public static DroolsRuleTemplateParam buildCollectionDroolsParam(Collection<?> list) {
        if (list == null) {
            return NULL_TEMPLATE_PARAM;
        }
        Class<?> listClazz = list.getClass();
        if(list instanceof Set){
            listClazz = HashSet.class;
        } else if(list instanceof List){
            listClazz = ArrayList.class;
        }
        return new DroolsRuleTemplateParam(String.format("new %s(java.util.Arrays.asList(%s))", listClazz.getName(),
                list.stream()
                        .map(DroolsTemplateRuleUtils::toTemplateParam)
                        .map(DroolsRuleTemplateParam::getParam)
                        .collect(Collectors.joining(","))));
    }

    /**
     * To build a String containing the logic to instantiate a new List copy of the input. the input object must have a no args constructor and must expose a setter/getter for each value
     */
    public static DroolsRuleTemplateParam buildNewObjectDroolsParam(Object o) {
        if (o == null) {
            return NULL_TEMPLATE_PARAM;
        }

        String varName = String.format("var%s", o.getClass().getSimpleName());

        List<String> setters = new ArrayList<>();

        Class<?> clazz = o.getClass();
        for(Method setter : clazz.getMethods()){
            try{
                if(setter.getName().startsWith("set") && setter.getParameterTypes().length==1){
                    Method getter = retrieveGetter(clazz, setter);

                    String value = toTemplateParam(getter.invoke(o)).toString();
                    if(setter.getParameterTypes()[0].isAssignableFrom(getter.getReturnType())) {
                        setters.add(String.format("%s.%s(%s);", varName, setter.getName(), value));
                    }
                }
            } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
                String errMessage = String.format("Cannot build object having class %s:%s", o.getClass(), o);
                log.error(errMessage);
                throw new IllegalStateException(errMessage, e);
            }
        }

        String clazzString = o.getClass().getName().replace("$", ".");
        return new DroolsRuleTemplateParam(String.format("((%s)(new java.util.function.Supplier<%s>(){public %s get(){%s %s = new %s();%sreturn %s;}}).get())", clazzString, clazzString, clazzString, clazzString, varName, clazzString, String.join("", setters), varName));
    }

    private static Method retrieveGetter(Class<?> clazz, Method setter) throws NoSuchMethodException {
        Method getter;
        try{
            getter = clazz.getMethod(setter.getName().replaceFirst("set", "get"));
        } catch (NoSuchMethodException e){
            getter = clazz.getMethod(setter.getName().replaceFirst("set", "is"));
        }
        return getter;
    }

    private static DroolsRuleTemplateParam buildValueOfStringDroolsParam(Class<?> clazz, Object value) {
        return new DroolsRuleTemplateParam(String.format("%s.valueOf(%s)", clazz.getName(), toTemplateParam(value.toString())));
    }

    private static DroolsRuleTemplateParam buildNewObjectFromStringDroolsParam(Class<?> clazz, Object value) {
        return new DroolsRuleTemplateParam(String.format("new %s(%s)", clazz.getName(), toTemplateParam(value.toString())));
    }
}
