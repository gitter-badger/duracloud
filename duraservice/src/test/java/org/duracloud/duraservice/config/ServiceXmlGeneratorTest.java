/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.duraservice.config;

import org.duracloud.common.util.ApplicationConfig;
import org.duracloud.serviceconfig.ServiceInfo;
import org.duracloud.serviceconfig.SystemConfig;
import org.duracloud.serviceconfig.user.UserConfig;
import org.duracloud.serviceconfig.user.UserConfigMode;
import org.duracloud.serviceconfig.user.UserConfigModeSet;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Andrew Woods
 *         Date: Jan 31, 2010
 */
public class ServiceXmlGeneratorTest {

    private static final String PROJECT_VERSION_PROP = "PROJECT_VERSION";

    @Test
    public void testBuildServiceList() {
        String ver = getVersion();

        ServiceXmlGenerator serviceXmlGenerator = new ServiceXmlGenerator(ver);
        List<ServiceInfo> serviceInfos = serviceXmlGenerator.buildServiceList();
        Assert.assertNotNull(serviceInfos);

        int NUM_SERVICES = 11;
        Assert.assertEquals(NUM_SERVICES, serviceInfos.size());

        boolean foundHello = false;
        boolean foundReplication = false;
        boolean foundImagemagick = false;
        boolean foundWebapputil = false;
        boolean foundHellowebappwrapper = false;
        boolean foundJ2k = false;
        boolean foundImageconversion = false;
        boolean foundMediaStreaming = false;
        boolean foundFixity = false;
        boolean foundFixityTools = false;
        boolean foundBulkImageConversion = false;
        boolean foundAmazonFixity = false;
        boolean foundRepOnDemand = false;

        for (ServiceInfo serviceInfo : serviceInfos) {
            String contentId = serviceInfo.getContentId();
            Assert.assertNotNull(contentId);
            if (contentId.equals("helloservice-" + ver + ".jar")) {
                foundHello = true;
                verifyHello();

            } else if (contentId.equals("replicationservice-" + ver + ".zip")) {
                foundReplication = true;
                verifyReplication(serviceInfo);

            } else if (contentId.equals("imagemagickservice-" + ver + ".zip")) {
                foundImagemagick = true;
                verifyImagemagick(serviceInfo);

            } else if (contentId.equals("webapputilservice-" + ver + ".zip")) {
                foundWebapputil = true;
                verifyWebapputil(serviceInfo);

            } else if (contentId.equals("hellowebappwrapper-" + ver + ".zip")) {
                foundHellowebappwrapper = true;
                verifyHellowebappwrapper(serviceInfo);

            } else if (contentId.equals("j2kservice-" + ver + ".zip")) {
                foundJ2k = true;
                verifyJ2k(serviceInfo);

            } else if (contentId.equals(
                "imageconversionservice-" + ver + ".zip")) {
                foundImageconversion = true;
                verifyImageconversion(serviceInfo);

            } else if (contentId.equals(
                "mediastreamingservice-" + ver + ".zip")) {
                foundMediaStreaming = true;
                verifyMediaStreaming(serviceInfo);

            } else if (contentId.equals(
                "fixityservice-" + ver + ".zip")) {
                foundFixity = true;
                verifyFixity(serviceInfo);

            } else if (contentId.equals(
                "bitintegritytoolsservice-" + ver + ".zip")) {
                foundFixityTools = true;
                verifyFixityTools(serviceInfo);

            } else if (contentId.equals(
                "bulkimageconversionservice-" + ver + ".zip")) {
                foundBulkImageConversion = true;
                verifyBulkImageconversion(serviceInfo);

            } else if (contentId.equals(
                "amazonfixityservice-" + ver + ".zip")) {
                foundAmazonFixity = true;
                verifyAmazonFixity(serviceInfo);

            } else if (contentId.equals(
                "replication-on-demand-service-" + ver + ".zip")) {
                foundRepOnDemand = true;
                verifyRepOnDemand(serviceInfo);

            } else {
                Assert.fail("unexpected contentId: " + contentId);
            }
        }

        //Assert.assertTrue(foundHello);
        Assert.assertTrue(foundReplication);
        Assert.assertTrue(foundImagemagick);
        Assert.assertTrue(foundWebapputil);
        //Assert.assertTrue(foundHellowebappwrapper);
        Assert.assertTrue(foundJ2k);
        Assert.assertTrue(foundImageconversion);
        Assert.assertTrue(foundMediaStreaming);
        Assert.assertTrue(foundFixity);
        Assert.assertTrue(foundFixityTools);
        Assert.assertTrue(foundBulkImageConversion);
        Assert.assertTrue(foundAmazonFixity);
        Assert.assertTrue(foundRepOnDemand);
    }

    private void verifyHello() {
        Assert.assertTrue("I need an implementation", true);
    }

    private void verifyReplication(ServiceInfo serviceInfo) {
        List<SystemConfig> systemConfigs = serviceInfo.getSystemConfigs();
        Assert.assertNotNull(systemConfigs);
        Assert.assertEquals(6, systemConfigs.size());

        verifyDurastoreCredential(systemConfigs);
    }

    private void verifyImagemagick(ServiceInfo serviceInfo) {
        Assert.assertTrue("I need an implementation", true);
    }

    private void verifyWebapputil(ServiceInfo serviceInfo) {
        Assert.assertTrue("I need an implementation", true);
    }

    private void verifyHellowebappwrapper(ServiceInfo serviceInfo) {
        Assert.assertTrue("I need an implementation", true);
    }

    private void verifyJ2k(ServiceInfo serviceInfo) {
        List<SystemConfig> systemConfigs = serviceInfo.getSystemConfigs();
        Assert.assertNotNull(systemConfigs);
        Assert.assertEquals(4, systemConfigs.size());

        verifyDurastoreCredential(systemConfigs);
    }

    private void verifyImageconversion(ServiceInfo serviceInfo) {
        int numUserConfigs = 6;
        int numSystemConfigs = 6;
        verifyServiceInfo(numUserConfigs, numSystemConfigs, serviceInfo);
    }

    private void verifyMediaStreaming(ServiceInfo serviceInfo) {
        int numUserConfigs = 2;
        int numSystemConfigs = 5;
        verifyServiceInfo(numUserConfigs, numSystemConfigs, serviceInfo);
    }

    private void verifyFixity(ServiceInfo serviceInfo) {
        int numUserConfigs = 0;
        int numSystemConfigs = 5;
        verifyServiceInfo(numUserConfigs, numSystemConfigs, serviceInfo);

        List<List<Integer>> setsModesConfigs = new ArrayList<List<Integer>>();
        setsModesConfigs.add(Arrays.asList(0, 1));
        verifyServiceModes(setsModesConfigs, serviceInfo);
    }

    private void verifyFixityTools(ServiceInfo serviceInfo) {
        int numUserConfigs = 0;
        int numSystemConfigs = 5;
        verifyServiceInfo(numUserConfigs, numSystemConfigs, serviceInfo);

        List<List<Integer>> setsModesConfigs = new ArrayList<List<Integer>>();
        setsModesConfigs.add(Arrays.asList(1, 2, 2));
        verifyServiceModes(setsModesConfigs, serviceInfo);
    }

    private void verifyBulkImageconversion(ServiceInfo serviceInfo) {
        int numUserConfigs = 9;
        int numSystemConfigs = 6;
        verifyServiceInfo(numUserConfigs, numSystemConfigs, serviceInfo);
    }

    private void verifyAmazonFixity(ServiceInfo serviceInfo) {
        int numUserConfigs = 5;
        int numSystemConfigs = 6;
        verifyServiceInfo(numUserConfigs, numSystemConfigs, serviceInfo);
    }

    private void verifyRepOnDemand(ServiceInfo serviceInfo) {
        int numUserConfigs = 7;
        int numSystemConfigs = 6;
        verifyServiceInfo(numUserConfigs, numSystemConfigs, serviceInfo);
    }

    private void verifyServiceInfo(int numUserConfigs,
                                   int numSystemConfigs,
                                   ServiceInfo serviceInfo) {
        List<UserConfigModeSet> userConfigModeSets = serviceInfo.getUserConfigModeSets();
        Assert.assertNotNull(userConfigModeSets);

        UserConfigModeSet userConfigModeSet = userConfigModeSets.get(0);
        if (0 == numUserConfigs) {
            Assert.assertFalse(userConfigModeSet.hasOnlyUserConfigs());
        } else {
            Assert.assertTrue(userConfigModeSet.hasOnlyUserConfigs());
            Assert.assertEquals(1, userConfigModeSets.size());
            Assert.assertEquals(numUserConfigs,
                                userConfigModeSet.wrappedUserConfigs().size());
        }

        List<SystemConfig> systemConfigs = serviceInfo.getSystemConfigs();
        Assert.assertNotNull(systemConfigs);
        Assert.assertEquals(numSystemConfigs, systemConfigs.size());

        verifyDurastoreCredential(systemConfigs);
    }

    private void verifyServiceModes(List<List<Integer>> setsModesConfigs,
                                    ServiceInfo serviceInfo) {
        List<UserConfigModeSet> modeSets = serviceInfo.getUserConfigModeSets();

        int numModeSets = setsModesConfigs.size();
        if (numModeSets > 0) {
            Assert.assertNotNull(modeSets);
            Assert.assertEquals(numModeSets, modeSets.size());
        } else {
            return;
        }

        for (int i = 0; i < numModeSets; ++i) {
            List<Integer> modesConfigsI = setsModesConfigs.get(i);
            int numModes = modesConfigsI.size();
            if (numModes > 0) {
                UserConfigModeSet modeSet = modeSets.get(i);
                Assert.assertNotNull(modeSet);

                List<UserConfigMode> modes = modeSet.getModes();
                Assert.assertNotNull(modes);
                Assert.assertEquals(numModes, modes.size());
                if (numModes > 0) {
                    for (int j = 0; j < numModes; ++j) {
                        UserConfigMode mode = modes.get(j);
                        Assert.assertNotNull(mode);

                        int numConfigsJ = modesConfigsI.get(j);
                        Assert.assertEquals(numConfigsJ,
                                            mode.getUserConfigs().size());
                    }
                }
            }
        }
    }

    private void verifyDurastoreCredential(List<SystemConfig> systemConfigs) {
        boolean foundUsername = false;
        boolean foundPassword = false;
        for (SystemConfig systemConfig : systemConfigs) {
            String name = systemConfig.getName();
            String value = systemConfig.getValue();
            Assert.assertNotNull(name);
            Assert.assertNotNull(value);

            if (name.equals("username")) {
                foundUsername = true;
                Assert.assertEquals("$DURASTORE-USERNAME", value);
            } else if (name.equals("password")) {
                foundPassword = true;
                Assert.assertEquals("$DURASTORE-PASSWORD", value);
            }
        }
        Assert.assertTrue(foundUsername);
        Assert.assertTrue(foundPassword);
    }

    @Test
    public void testGenerate() throws Exception {
        TestConfig config = new TestConfig();
        String targetDir = config.getTargetDir();
        URI targetDirUri = new URI(targetDir);
        File targetDirFile = new File(targetDirUri);

        ServiceXmlGenerator xmlGenerator = new ServiceXmlGenerator(getVersion());
        xmlGenerator.generateServiceXml(targetDirFile.getAbsolutePath());
    }

    private String getVersion() {
        String version = System.getProperty(PROJECT_VERSION_PROP);
        Assert.assertNotNull(version);
        return version;
    }

    private class TestConfig extends ApplicationConfig {
        private String propName = "test-duraservice.properties";

        private Properties getProps() throws Exception {
            return getPropsFromResource(propName);
        }

        public String getTargetDir() throws Exception {
            return getProps().getProperty("targetdir");
        }
    }
}