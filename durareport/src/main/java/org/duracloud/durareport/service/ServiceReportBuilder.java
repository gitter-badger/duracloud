/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.durareport.service;

import org.duracloud.common.util.IOUtil;
import org.duracloud.common.util.SerializationUtil;
import org.duracloud.durareport.error.ReportBuilderException;
import org.duracloud.durareport.storage.StorageReportScheduler;
import org.duracloud.serviceapi.ServicesManager;
import org.duracloud.serviceapi.error.ServicesException;
import org.duracloud.serviceconfig.ServiceInfo;
import org.duracloud.serviceconfig.ServiceSummariesDocument;
import org.duracloud.serviceconfig.ServiceSummary;
import org.duracloud.servicemonitor.ServiceSummarizer;
import org.duracloud.servicemonitor.ServiceSummaryDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Builds service reports.
 *
 * @author: Bill Branan
 * Date: 6/22/11
 */
public class ServiceReportBuilder {

    private final Logger log =
        LoggerFactory.getLogger(StorageReportScheduler.class);

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 1000;

    private ServicesManager servicesMgr;
    private ServiceSummarizer serviceSummarizer;
    private ServiceSummaryDirectory summaryDirectory;

    public ServiceReportBuilder(ServicesManager servicesMgr,
                                ServiceSummarizer serviceSummarizer,
                                ServiceSummaryDirectory summaryDirectory) {
        this.servicesMgr = servicesMgr;
        this.serviceSummarizer = serviceSummarizer;
        this.summaryDirectory = summaryDirectory;
    }

    /**
     * Gets a listing of service summaries which includes all of the
     * currently deployed services.
     *
     * @return stream of XML with a summary of each deployed service
     */
    public InputStream getDeployedServicesReport() {
        List<ServiceSummary> summaries =  collectDeployedServices();
        return convertSummariesToStream(summaries);
    }

    protected List<ServiceSummary> collectDeployedServices()
        throws ReportBuilderException {
        try {
            List<ServiceInfo> deployedServices =
                servicesMgr.getDeployedServices();
            return serviceSummarizer.summarizeServices(deployedServices);
        } catch(ServicesException e) {
            String error = "Unable to collect information about deployed " +
                           "services due to: " + e.getMessage();
            throw new ReportBuilderException(error, e);
        }
    }

    private InputStream convertSummariesToStream(List<ServiceSummary> services) {
        String xml =
            ServiceSummariesDocument.getServiceSummaryListAsXML(services);
        return convertXmlToStream(xml);
    }

    private InputStream convertXmlToStream(String xml) {
        try {
            return IOUtil.writeStringToStream(xml);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a listing of the most recent completed services. The number of
     * services included in the list is defined by the limit parameter.
     * The default value is 20, the max value is 1000.
     *
     * @param limit the maximum number of services to include in the report
     * @return stream of XML with a summary of the most recent completed services
     */
    public InputStream getCompletedServicesReport(int limit) {
        if(limit < 1 || limit > MAX_LIMIT) {
            limit = DEFAULT_LIMIT;
        }

        List<ServiceSummary> completedSums = collectCompletedServices(limit);
        return convertSummariesToStream(completedSums);
    }

    protected List<ServiceSummary> collectCompletedServices(int limit) {
        List<ServiceSummary> completedSums = new LinkedList<ServiceSummary>();

        // Start with current services
        List<ServiceSummary> summaries =
            summaryDirectory.getCurrentServiceSummaries();
        addSummaries(summaries, completedSums, limit);

        // Include services from previous summary lists as necessary
        if(completedSums.size() < limit) {
            List<String> summaryIds = summaryDirectory.getServiceSummaryIds();
            Collections.sort(summaryIds);
            Collections.reverse(summaryIds);

            Iterator<String> summaryIdIterator = summaryIds.iterator();
            while(summaryIdIterator.hasNext() && completedSums.size() < limit) {
                String summaryId = summaryIdIterator.next();
                List<ServiceSummary> sumList =
                    summaryDirectory.getServiceSummariesById(summaryId);
                addSummaries(sumList, completedSums, limit);
            }
        }

        return completedSums;
    }

    private void addSummaries(List<ServiceSummary> sourceList,
                              List<ServiceSummary> compilationList,
                              int limit) {
        // TODO: Order source list based on completion date
        Iterator<ServiceSummary> summaryIterator = sourceList.iterator();
        while(summaryIterator.hasNext() && compilationList.size() < limit) {
            compilationList.add(summaryIterator.next());
        }
    }

    /**
     * Gets the list of the IDS of all pre-created completed services reports.
     *
     * @return stream of XML with all available reportIDs
     */
    public InputStream getCompletedServicesReportIds() {
        List<String> reportIds = summaryDirectory.getServiceSummaryIds();
        // TODO: Serialize with a schema binding
        String xml = SerializationUtil.serializeList(reportIds);
        return convertXmlToStream(xml);
    }

    /**
     * Gets a specific completed services report based on ID.
     *
     * @param reportId the ID of the report to retrieve
     * @return stream of XML with all service summaries included in this report
     */
    public InputStream getCompletedServicesReport(String reportId) {
        return summaryDirectory.getServiceSummariesStreamById(reportId);
    }

}