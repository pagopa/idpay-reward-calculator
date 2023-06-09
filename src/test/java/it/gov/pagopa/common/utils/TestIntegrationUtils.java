package it.gov.pagopa.common.utils;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.TimeZone;

/** Utilities when performing Spring integration tests */
public class TestIntegrationUtils {
    private TestIntegrationUtils() {}

    public static void setDefaultTimeZoneAndUnregisterCommonMBean() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TimeZone.setDefault(TimeZone.getTimeZone(CommonConstants.ZONEID));

        unregisterMBean("kafka.*:*");
        unregisterMBean("org.springframework.*:*");
    }

    /** To unregister MBean */
    public static void unregisterMBean(String objectName) throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        ObjectName mbeanName = new ObjectName(objectName);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectInstance mBean : mBeanServer.queryMBeans(mbeanName, null)) {
            mBeanServer.unregisterMBean(mBean.getObjectName());
        }
    }
}
