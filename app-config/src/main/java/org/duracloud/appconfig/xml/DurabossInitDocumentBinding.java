/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.appconfig.xml;

import org.duracloud.appconfig.domain.DurabossConfig;
import org.duracloud.appconfig.domain.NotificationConfig;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.util.EncryptionUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Bill Branan
 * Date: 5/12/11
 */
public class DurabossInitDocumentBinding {

    private static final Logger log = LoggerFactory
        .getLogger(DurabossInitDocumentBinding.class);

    private static EncryptionUtil encryptionUtil;

    static {
        try {
            encryptionUtil = new EncryptionUtil();
        } catch (Exception e) {
            throw new DuraCloudRuntimeException(e);
        }
    }


    /**
     * This method deserializes the provided xml into a duraboss config object.
     *
     * @param xml
     * @return
     */
    public static DurabossConfig createDurabossConfigFrom(InputStream xml) {
        DurabossConfig config = new DurabossConfig();

        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(xml);
            Element root = doc.getRootElement();

            config.setReporterEnabled(
                Boolean.valueOf(root.getChildText("reporterEnabled")));
            config.setExecutorEnabled(
                Boolean.valueOf(root.getChildText("executorEnabled")));
            config.setAuditorEnabled(
                Boolean.valueOf(root.getChildText("auditorEnabled")));

            config.setDurastoreHost(root.getChildText("durastoreHost"));
            config.setDurastorePort(root.getChildText("durastorePort"));
            config.setDurastoreContext(root.getChildText("durastoreContext"));
            config.setDuraserviceHost(root.getChildText("duraserviceHost"));
            config.setDuraservicePort(root.getChildText("duraservicePort"));
            config.setDuraserviceContext(root.getChildText("duraserviceContext"));

            List<Element> notifyElements = root.getChildren("notificationConfig");
            if(null != notifyElements) {
                Map<String, NotificationConfig> configMap =
                    new HashMap<String, NotificationConfig>();
                for(int i=0; i<notifyElements.size(); i++) {
                    Element notifyElement = notifyElements.get(i);
                    NotificationConfig notifyConfig = new NotificationConfig();
                    notifyConfig.setType(notifyElement.getChildText("type"));
                    String encUsername = notifyElement.getChildText("username");
                    notifyConfig.setUsername(decrypt(encUsername));
                    String encPassword = notifyElement.getChildText("password");
                    notifyConfig.setPassword(decrypt(encPassword));
                    notifyConfig.setOriginator(
                        notifyElement.getChildText("originator"));
                    configMap.put(String.valueOf(i), notifyConfig);
                }
                config.setNotificationConfigs(configMap);
            }
        } catch (Exception e) {
            String error = "Error encountered attempting to parse " +
                "Duraboss configuration xml: " + e.getMessage();
            log.error(error);
            throw new DuraCloudRuntimeException(error, e);
        }

        return config;
    }

    /**
     * This method serializes the provide duraboss configuration into xml.
     *
     * @param durabossConfig
     * @return
     */
    public static String createDocumentFrom(DurabossConfig durabossConfig) {
        StringBuilder xml = new StringBuilder();

        if (null != durabossConfig) {
            String durastoreHost = durabossConfig.getDurastoreHost();
            String durastorePort = durabossConfig.getDurastorePort();
            String durastoreContext = durabossConfig.getDurastoreContext();
            String duraserviceHost = durabossConfig.getDuraserviceHost();
            String duraservicePort = durabossConfig.getDuraservicePort();
            String duraserviceContext = durabossConfig.getDuraserviceContext();
            boolean reporterEnabled = durabossConfig.isReporterEnabled();
            boolean executorEnabled = durabossConfig.isExecutorEnabled();
            boolean auditorEnabled = durabossConfig.isAuditorEnabled();

            xml.append("<durabossConfig>");
            xml.append("  <reporterEnabled>" + reporterEnabled);
            xml.append("</reporterEnabled>");
            xml.append("  <executorEnabled>" + executorEnabled);
            xml.append("</executorEnabled>");
            xml.append("  <auditorEnabled>" + auditorEnabled);
            xml.append("</auditorEnabled>");
            xml.append("  <durastoreHost>" + durastoreHost);
            xml.append("</durastoreHost>");
            xml.append("  <durastorePort>" + durastorePort);
            xml.append("</durastorePort>");
            xml.append("  <durastoreContext>" + durastoreContext);
            xml.append("</durastoreContext>");
            xml.append("  <duraserviceHost>" + duraserviceHost);
            xml.append("</duraserviceHost>");
            xml.append("  <duraservicePort>" + duraservicePort);
            xml.append("</duraservicePort>");
            xml.append("  <duraserviceContext>" + duraserviceContext);
            xml.append("</duraserviceContext>");

            Collection<NotificationConfig> notificationConfigs =
                durabossConfig.getNotificationConfigs();
            if(null != notificationConfigs) {
                for(NotificationConfig config : notificationConfigs) {
                    String encUsername = encrypt(config.getUsername());
                    String encPassword = encrypt(config.getPassword());

                    xml.append("<notificationConfig>");
                    xml.append("  <type>" + config.getType() + "</type>");
                    xml.append("  <username>" + encUsername +
                                 "</username>");
                    xml.append("  <password>" + encPassword +
                                 "</password>");
                    xml.append("  <originator>" + config.getOriginator() +
                                 "</originator>");
                    xml.append("</notificationConfig>");
                }
            }

            xml.append("</durabossConfig>");
        }
        return xml.toString();
    }

    private static String encrypt(String text) {
        try {
            return encryptionUtil.encrypt(text);
        } catch (Exception e) {
            throw new DuraCloudRuntimeException(e);
        }
    }

    private static String decrypt(String text) {
        try {
            return encryptionUtil.decrypt(text);
        } catch (Exception e) {
            throw new DuraCloudRuntimeException(e);
        }
    }

}
