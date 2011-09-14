/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.s3task.streaming;

import com.amazonaws.services.s3.AmazonS3Client;
import org.duracloud.common.util.SerializationUtil;
import org.easymock.classextension.EasyMock;
import org.jets3t.service.CloudFrontService;
import org.jets3t.service.model.cloudfront.LoggingStatus;
import org.jets3t.service.model.cloudfront.OriginAccessIdentity;
import org.jets3t.service.model.cloudfront.S3Origin;
import org.jets3t.service.model.cloudfront.StreamingDistribution;
import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author: Bill Branan
 * Date: Jun 3, 2010
 */
public class EnableStreamingTaskRunnerTest extends StreamingTaskRunnerTestBase {

    protected EnableStreamingTaskRunner createRunner(AmazonS3Client s3Client,
                                                     CloudFrontService cfService) {
        this.s3Provider = createMockS3StorageProvider();
        this.s3Client = s3Client;
        this.cfService = cfService;
        return new EnableStreamingTaskRunner(s3Provider, s3Client, cfService);
    }    

    @Test
    public void testGetName() throws Exception {
        EnableStreamingTaskRunner runner =
            createRunner(createMockS3ClientV1(), createMockCFServiceV1());

        String name = runner.getName();
        assertEquals("enable-streaming", name);
    }

    /*
     * Testing the case where no streaming distribution exists for the given
     * bucket and no origin access id exists. Both should be created.
     */
    @Test
    public void testPerformTask1() throws Exception {
        EnableStreamingTaskRunner runner =
            createRunner(createMockS3ClientV2(), createMockCFServiceV3());

        try {
            runner.performTask(null);
            fail("Exception expected");
        } catch(Exception expected) {
            assertNotNull(expected);
        }

        String results = runner.performTask("spaceId");
        assertNotNull(results);
        testResults(results);
    }

    /*
     * For testing the case where a distribution and origin access id do not
     * exist and are created.
     * In short, these are the calls that are expected:
     *
     * createStreamingDistribution (1) - returns valid dist
     * createOriginAccessIdentity (1) - returns valid oaid
     * getOriginAccessIdentityList (1) - returns null (or empty list)
     * getOriginAccessIdentity (1) - returns valid oaid
     * listStreamingDistributions (1) - returns null
     */
    private CloudFrontService createMockCFServiceV3() throws Exception {
        CloudFrontService service =
            EasyMock.createMock(CloudFrontService.class);

        S3Origin origin = new S3Origin("origin");
        StreamingDistribution dist =
            new StreamingDistribution("id", "status", null, "domainName",
                                      origin, null, "comment", true);

        EasyMock
            .expect(service.createStreamingDistribution(
                EasyMock.isA(S3Origin.class),
                EasyMock.<String>isNull(),
                EasyMock.<String[]>isNull(),
                EasyMock.<String>isNull(),
                EasyMock.eq(true),
                EasyMock.<LoggingStatus>isNull(),
                EasyMock.eq(false),
                EasyMock.<String[]>isNull()))
            .andReturn(dist)
            .times(1);

        OriginAccessIdentity oaIdentity =
            new OriginAccessIdentity("id", "s3CanonicalUserId", "comment");

        EasyMock
            .expect(service.createOriginAccessIdentity(
                EasyMock.<String>isNull(),
                EasyMock.isA(String.class)))
            .andReturn(oaIdentity)
            .times(1);

        EasyMock
            .expect(service.getOriginAccessIdentityList())
            .andReturn(null)
            .times(1);

        EasyMock
            .expect(service.getOriginAccessIdentity(EasyMock.isA(String.class)))
            .andReturn(oaIdentity)
            .times(1);

        EasyMock
            .expect(service.listStreamingDistributions())
            .andReturn(null)
            .times(1);

        EasyMock.replay(service);
        return service;
    }

    /*
     * Testing the case where a streaming distribution exists for the given
     * bucket and an origin access id exists. Nothing should be created.
     */
    @Test
    public void testPerformTask2() throws Exception {
        EnableStreamingTaskRunner runner =
            createRunner(createMockS3ClientV2(), createMockCFServiceV2());

        String results = runner.performTask("spaceId");
        assertNotNull(results);
        testResults(results);
    }

    private void testResults(String results) {
        Map<String, String> resultMap =
            SerializationUtil.deserializeMap(results);
        assertNotNull(resultMap);
        assertEquals(resultMap.get("domain-name"), "domainName");
        assertTrue(resultMap.get("results").contains("completed"));
    }
    
}
