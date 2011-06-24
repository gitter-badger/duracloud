/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.servicemonitor;

import org.duracloud.serviceconfig.ServiceInfo;
import org.duracloud.serviceconfig.ServiceSummary;
import org.duracloud.servicemonitor.error.ServiceSummaryException;

import java.util.List;

/**
 * @author: Bill Branan
 * Date: 6/23/11
 */
public interface ServiceSummarizer {

    public List<ServiceSummary> summarizeServices(List<ServiceInfo> services);

    public List<ServiceSummary> summarizeService(ServiceInfo service);

    public ServiceSummary summarizeService(int serviceId, int deploymentId)
        throws ServiceSummaryException;

}