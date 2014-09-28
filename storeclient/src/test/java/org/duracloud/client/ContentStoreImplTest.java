/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.duracloud.common.model.AclType;
import org.duracloud.common.retry.Retriable;
import org.duracloud.common.util.SerializationUtil;
import org.duracloud.common.web.RestHttpHelper;
import org.duracloud.domain.Content;
import org.duracloud.domain.Space;
import org.duracloud.error.ContentStoreException;
import org.duracloud.error.InvalidIdException;
import org.duracloud.manifest.ManifestGenerator.FORMAT;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrew Woods
 *         Date: 8/23/11
 */
public class ContentStoreImplTest {

    private static final String HEADER_PREFIX = "x-dura-meta-";
    private static final String ACL_PREFIX = StorageProvider.PROPERTIES_SPACE_ACL;

    private ContentStore contentStore;

    private final String baseURL = "url";
    private final StorageProviderType type = StorageProviderType.AMAZON_S3;
    private final String storeId = "1";
    private RestHttpHelper restHelper;
    private RestHttpHelper.HttpResponse response;

    private String spaceId = "myspace";
    private String contentId = "mycontent";

    @Before
    public void setUp() throws Exception {
        restHelper = EasyMock.createMock("RestHttpHelper",
                                         RestHttpHelper.class);
        response = EasyMock.createMock("HttpResponse",
                                       RestHttpHelper.HttpResponse.class);

        contentStore = new ContentStoreImpl(baseURL, type, storeId, restHelper);
    }

    @After
    public void tearDown() throws Exception {
        EasyMock.verify(restHelper, response);
    }

    private void replayMocks() {
        EasyMock.replay(restHelper, response);
    }

    @Test
    public void testRetry() throws Exception {
        int attempts = doWork(1);
        Assert.assertEquals(1, attempts);

        attempts = doWork(2);
        Assert.assertEquals(2, attempts);

        attempts = doWork(3);
        Assert.assertEquals(3, attempts);

        try {
            attempts = doWork(3);
        } catch(ContentStoreException e) {
            Assert.assertNotNull(e);
        }

        replayMocks();
    }

    private class TestRetriable implements Retriable {
        private int expectedFailures = 0;
        private int attempts = 0;

        public TestRetriable(int expectedFailures) {
            this.expectedFailures = expectedFailures;
        }

        @Override
        public Integer retry() throws ContentStoreException {
            attempts++;
            if(attempts >= expectedFailures) {
                return attempts;
            } else {
                throw new ContentStoreException("Expected Failure");
            }
        }
    }

    private Integer doWork(int expectedFailures) throws ContentStoreException {
        ContentStoreImpl fakeStore = new ContentStoreImpl(null, null,null, null);
        return fakeStore.execute(new TestRetriable(expectedFailures));
    }

    @Test
    public void testGetSpaces() throws Exception {
        String xml = "<spaces><space id=\"space1\" />" +
                     "<space id=\"space2\" /></spaces>";
        String fullURL = baseURL + "/spaces" + "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(response.getResponseBody()).andReturn(xml);
        EasyMock.expect(restHelper.get(fullURL)).andReturn(response);

        replayMocks();

        List<String> spaces = contentStore.getSpaces();
        Assert.assertEquals(2, spaces.size());
    }

    @Test
    public void testGetSpaceContents() throws Exception {
        String xml = "<space id=\"space1\">" +
                     "<item>Image 1</item><item>Image 2</item></space>";
        String fullURL = baseURL + "/" + spaceId +
                         "?maxResults=1000&storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(response.getResponseBody()).andReturn(xml);
        EasyMock.expect(restHelper.get(fullURL)).andReturn(response);
        EasyMock.expect(response.getResponseHeaders()).andReturn(new Header[0]);

        replayMocks();

        Iterator<String> spaceContents = contentStore.getSpaceContents(spaceId);
        Assert.assertNotNull(spaceContents);
        Assert.assertEquals("Image 1", spaceContents.next());
        Assert.assertEquals("Image 2", spaceContents.next());
    }

    @Test
    public void testGetSpace() throws Exception {
        String xml = "<space id=\"space1\">" +
                     "<item>Image 1</item><item>Image 2</item></space>";
        String fullURL = baseURL + "/" + spaceId +
                         "?maxResults=10&storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(response.getResponseBody()).andReturn(xml);
        EasyMock.expect(response.getResponseHeaders()).andReturn(new Header[0]);

        EasyMock.expect(restHelper.get(fullURL)).andReturn(response);

        replayMocks();

        Space space = contentStore.getSpace(spaceId, null, 10, null);
        Assert.assertNotNull(space);
        List<String> spaceContents = space.getContentIds();
        Assert.assertEquals(2, spaceContents.size());
    }
    
    @Test
    public void testGetManifest() throws Exception {
        String tsv = "manifest stream";
        String fullURL = baseURL + "/manifest/" + spaceId +
                         "?storeID=" + storeId + "&format=TSV";
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(response.getResponseStream())
                .andReturn(new ByteArrayInputStream(tsv.getBytes()));

        EasyMock.expect(restHelper.get(fullURL)).andReturn(response);

        replayMocks();

        InputStream is = contentStore.getManifest(spaceId,FORMAT.TSV);
        Assert.assertNotNull(is);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Assert.assertEquals(tsv, reader.readLine());
    }

    @Test
    public void testCreateSpace() throws Exception {
        String fullURL = baseURL + "/" + spaceId + "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(201);
        EasyMock.expect(restHelper.put(fullURL, null, null)).andReturn(response);

        replayMocks();

        contentStore.createSpace(spaceId);
    }

    @Test
    public void testDeleteSpace() throws Exception {
        String fullURL = baseURL + "/" + spaceId + "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(restHelper.delete(fullURL)).andReturn(response);

        replayMocks();

        contentStore.deleteSpace(spaceId);
    }

    @Test
    public void testGetSpaceProperties() throws Exception {
        Header[] headers =
            new Header[]{new Header("x-dura-meta-space-count", "65"),
                         new Header("x-dura-meta-custom-property", "custom")};
        String fullURL = baseURL + "/" + spaceId + "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(restHelper.head(fullURL)).andReturn(response);
        EasyMock.expect(response.getResponseHeaders()).andReturn(headers);

        replayMocks();

        Map<String, String> spaceProps =
            contentStore.getSpaceProperties(spaceId);
        Assert.assertEquals("65", spaceProps.get("space-count"));
        Assert.assertEquals("custom", spaceProps.get("custom-property"));
    }

    @Test
    public void testSpaceExists() throws Exception {
        String xml = "<spaces><space id=\"space1\" />" +
                     "<space id=\"space2\" /></spaces>";
        String fullURL = baseURL + "/spaces" + "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200).times(2);
        EasyMock.expect(response.getResponseBody()).andReturn(xml).times(2);
        EasyMock.expect(restHelper.get(fullURL)).andReturn(response).times(2);

        replayMocks();

        Assert.assertTrue(contentStore.spaceExists("space1"));
        Assert.assertFalse(contentStore.spaceExists("spaceX"));
    }

    @Test
    public void testAddContent() throws Exception {
        InputStream content = IOUtils.toInputStream("content");
        String checksum = "checksum";
        String mime = "text/plain";

        Capture<Map<String, String>> headersCapture = new Capture<>();
        String fullURL = baseURL + "/" + spaceId + "/" + contentId +
                         "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(201);
        EasyMock.expect(response.getResponseHeader(HttpHeaders.CONTENT_MD5))
                .andReturn(new Header(HttpHeaders.CONTENT_MD5, checksum));
        EasyMock.expect(restHelper.put(EasyMock.eq(fullURL),
                                       EasyMock.eq(content),
                                       EasyMock.eq("7"),
                                       EasyMock.eq(mime),
                                       EasyMock.capture(headersCapture)))
                .andReturn(response);

        replayMocks();

        contentStore.addContent(spaceId, contentId, content, 7,
                                mime, checksum, null);
        Map<String, String> headers = headersCapture.getValue();
        Assert.assertEquals(mime, headers.get("x-dura-meta-" +
                                              ContentStore.CONTENT_MIMETYPE));
        Assert.assertEquals(checksum, headers.get(HttpHeaders.CONTENT_MD5));
    }

    @Test
    public void testGetContent() throws Exception {
        InputStream stream = IOUtils.toInputStream("content");

        String fullURL = baseURL + "/" + spaceId + "/" + contentId +
                         "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(response.getResponseHeaders())
                .andReturn(new Header[0]).times(2);
        EasyMock.expect(response.getResponseStream()).andReturn(stream);
        EasyMock.expect(restHelper.get(fullURL)).andReturn(response);

        replayMocks();

        Content content = contentStore.getContent(spaceId, contentId);
        Assert.assertNotNull(content);
        Assert.assertEquals(contentId, content.getId());
        Assert.assertEquals(stream, content.getStream());
    }

    @Test
    public void testDeleteContent() throws Exception {
        String fullURL = baseURL + "/" + spaceId + "/" + contentId +
                         "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(restHelper.delete(fullURL)).andReturn(response);

        replayMocks();

        contentStore.deleteContent(spaceId, contentId);
    }

    @Test
    public void testSetContentProperties() throws Exception {
        Capture<Map<String, String>> headersCapture = new Capture<>();
        String fullURL = baseURL + "/" + spaceId + "/" + contentId +
                         "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(restHelper.post(EasyMock.eq(fullURL),
                                        EasyMock.<String>isNull(),
                                        EasyMock.capture(headersCapture)))
                .andReturn(response);

        replayMocks();

        Map<String, String> props = new HashMap<>();
        props.put("key1", "value1");
        props.put("key2", "value2");
        contentStore.setContentProperties(spaceId, contentId, props);

        Map<String, String> headers = headersCapture.getValue();
        Assert.assertEquals("value1", headers.get("x-dura-meta-key1"));
        Assert.assertEquals("value2", headers.get("x-dura-meta-key2"));
    }

    @Test
    public void testGetContentProperties() throws Exception {
        String fullURL = baseURL + "/" + spaceId + "/" + contentId +
                         "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);

        Header[] headers =
            new Header[]{new Header("Content-Type", "text/xml"),
                         new Header("x-dura-meta-custom-property", "custom")};
        EasyMock.expect(response.getResponseHeaders())
                .andReturn(headers).times(2);
        EasyMock.expect(restHelper.head(fullURL)).andReturn(response);

        replayMocks();

        Map<String, String> props =
            contentStore.getContentProperties(spaceId, contentId);
        Assert.assertEquals("text/xml", props.get(
            StorageProvider.PROPERTIES_CONTENT_MIMETYPE));
        Assert.assertEquals("custom", props.get("custom-property"));
    }

    @Test
    public void testContentExists() throws Exception {
        String fullURL = baseURL + "/" + spaceId + "/" + contentId +
                         "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode())
                .andReturn(HttpStatus.SC_OK);
        EasyMock.expect(response.getStatusCode())
                .andReturn(HttpStatus.SC_NOT_FOUND);

        Header[] headers = new Header[]{};
        EasyMock.expect(response.getResponseHeaders())
                .andReturn(headers).times(2);
        EasyMock.expect(restHelper.head(fullURL)).andReturn(response).times(2);
        EasyMock.expect(response.getResponseBody()).andReturn("");

        replayMocks();

        Assert.assertTrue(contentStore.contentExists(spaceId, contentId));
        Assert.assertFalse(contentStore.contentExists(spaceId, contentId));
    }

    @Test
    public void testCopyContentWithDefaultStoreId() throws Exception {
        doTestCopyContent(storeId);
    }

    @Test
    public void testCopyContentWithAlternateStoreId() throws Exception {
        doTestCopyContent("1");
    }

    private void doTestCopyContent(String destStoreId) throws Exception {
        String srcSpaceId = "src-space-id";
        String srcContentId = "src-content-id";
        String destSpaceId = "dest-space-id";
        String destContentId = "dest-content-id";

        String expectedMd5 = "md5";
        int expectedStatus = 201;
        Capture<Map<String, String>> capturedHeaders = createCopyContentMocks(
            destStoreId,
            destSpaceId,
            destContentId,
            expectedMd5,
            expectedStatus);
        replayMocks();

        
        String md5;
        if(storeId.equals(destStoreId)){
            md5 = contentStore.copyContent(
                srcSpaceId,
                srcContentId,
                destSpaceId,
                destContentId);
            
        }else{
            md5 = contentStore.copyContent(
                srcSpaceId,
                srcContentId,
                destStoreId,
                destSpaceId,
                destContentId);
        }
        Assert.assertNotNull(md5);
        Assert.assertEquals(expectedMd5, md5);

        Assert.assertNotNull(capturedHeaders);
        Map<String, String> headers = capturedHeaders.getValue();
        Assert.assertNotNull(headers);
        Assert.assertEquals(2, headers.size());
        Assert.assertEquals(srcSpaceId + "/" + srcContentId,
                            headers.get(HEADER_PREFIX
                                + StorageProvider.PROPERTIES_COPY_SOURCE));

        if(!destStoreId.equals(this.storeId)){
            Assert.assertEquals(destStoreId,
                                headers.get(HEADER_PREFIX
                                    + StorageProvider.PROPERTIES_COPY_SOURCE_STORE));
        }
    }

    @Test
    public void testCopyContentErrorWithDefaultStore() throws Exception {
        int retries = 2;
        contentStore =
            new ContentStoreImpl(baseURL, type, storeId, restHelper, retries);
        doTestCopyContentError(storeId, retries);
    }

    @Test
    public void testCopyContentErrorWithAlternateStore() throws Exception {
        int retries = 4;
        contentStore =
            new ContentStoreImpl(baseURL, type, storeId, restHelper, retries);
        doTestCopyContentError("1", retries);
    }

    private void doTestCopyContentError(String destStoreId, int retries) throws Exception {
        String srcSpaceId = "src-space-id";
        String srcContentId = "src-content-id";
        String destSpaceId = "dest-space-id";
        String destContentId = "dest-content-id";

        String expectedMd5 = "md5";
        int status = 400;
        createCopyContentMocksError(destStoreId,
                                    destSpaceId,
                                    destContentId,
                                    expectedMd5,
                                    status,
                                    retries + 1);
        replayMocks();

        try {
            
            if(storeId.equals(destStoreId)){
                contentStore.copyContent(srcSpaceId,
                                         srcContentId,
                                         destSpaceId,
                                         destContentId);
            }else{
                contentStore.copyContent(srcSpaceId,
                                         srcContentId,
                                         destStoreId,
                                         destSpaceId,
                                         destContentId);
            }
            Assert.fail("exception expected");

        } catch (Exception e) {
            Assert.assertTrue(e instanceof InvalidIdException);
        }

    }

    private void createCopyContentMocksError(String destStoreId,
                                             String destSpaceId,
                                             String destContentId,
                                             String md5,
                                             int status,
                                             int expectedAttempts) throws Exception {
        EasyMock.expect(response.getStatusCode())
                .andReturn(status).times(expectedAttempts);

        Header header = EasyMock.createMock(Header.class);
        EasyMock.expect(header.getValue())
                .andReturn(md5);
        EasyMock.expect(response.getResponseBody())
                .andReturn("body")
                .times(expectedAttempts);
        EasyMock.replay(header);

        String fullURL =
            baseURL + "/" + destSpaceId + "/" + destContentId + "?storeID=" +
                destStoreId;
        Capture<Map<String, String>> capturedHeaders = new Capture<>();
        EasyMock.expect(restHelper.put(EasyMock.eq(fullURL),
                                       EasyMock.<String>isNull(),
                                       EasyMock.capture(capturedHeaders)))
                .andReturn(response)
                .times(expectedAttempts);
    }

    private Capture<Map<String, String>> createCopyContentMocks(String destStoreId,
                                                                String destSpaceId,
                                                                String destContentId,
                                                                String md5,
                                                                int status)
        throws Exception {
        EasyMock.expect(response.getStatusCode()).andReturn(status);

        Header header = EasyMock.createMock(Header.class);
        EasyMock.expect(header.getValue()).andReturn(md5);
        EasyMock.expect(response.getResponseHeader(HttpHeaders.CONTENT_MD5))
            .andReturn(header);
        EasyMock.replay(header);

        String fullURL =
            baseURL + "/" + destSpaceId + "/" + destContentId + "?storeID=" +
                destStoreId;
        Capture<Map<String, String>> capturedHeaders =
            new Capture<Map<String, String>>();
        EasyMock.expect(restHelper.put(EasyMock.eq(fullURL),
                                       EasyMock.<String>isNull(),
                                       EasyMock.capture(capturedHeaders)))
            .andReturn(response);

        return capturedHeaders;
    }

    @Test
    public void testMoveContentWithDefaultStore() throws Exception {
        doTestMoveContent(storeId);
    }

    @Test
    public void testMoveContentWithAlternateStore() throws Exception {
        doTestMoveContent("1");
    }

    private void doTestMoveContent(String destStoreId) throws Exception {
        String srcSpaceId = "src-space-id";
        String srcContentId = "src-content-id";
        String destSpaceId = "dest-space-id";
        String destContentId = "dest-content-id";

        String expectedMd5 = "md5";
        int expectedStatus = 201;
        Capture<Map<String, String>> capturedHeaders = createCopyContentMocks(
            destStoreId,
            destSpaceId,
            destContentId,
            expectedMd5,
            expectedStatus);

        EasyMock.expect(response.getStatusCode()).andReturn(HttpStatus.SC_OK);
        EasyMock.expect(restHelper.delete(EasyMock.<String>notNull()))
            .andReturn(response);
        replayMocks();

        String md5;
        if(destStoreId.equals(storeId)){
            md5 = contentStore.moveContent(srcSpaceId,
                                           srcContentId,
                                           destSpaceId,
                                           destContentId);
        }else{
            md5 = contentStore.moveContent(srcSpaceId,
                                           srcContentId,
                                           destStoreId,
                                           destSpaceId,
                                           destContentId);
        }
        Assert.assertNotNull(md5);
        Assert.assertEquals(expectedMd5, md5);

        Assert.assertNotNull(capturedHeaders);
        Map<String, String> headers = capturedHeaders.getValue();
        Assert.assertNotNull(headers);
        Assert.assertEquals(2, headers.size());
        Assert.assertEquals(srcSpaceId + "/" + srcContentId, headers.get(
            HEADER_PREFIX + StorageProvider.PROPERTIES_COPY_SOURCE));
        if (!destStoreId.equals(this.storeId)) {
            Assert.assertEquals(destStoreId,
                                headers.get(HEADER_PREFIX
                                    + StorageProvider.PROPERTIES_COPY_SOURCE_STORE));

        }
    }

    @Test
    public void testSetSpaceACLs() throws Exception {
        String name = "name1";
        Map<String, AclType> acls = createACLsForTest(name);

        String spaceId = "space-id";
        Capture<Map<String, String>> capturedHeaders = createSetSpaceACLsMocks(
            spaceId,
            storeId,
            200);

        // call being tested
        contentStore.setSpaceACLs(spaceId, acls);

        // Note; All headers are included, even if they are not ACL headers.
        Assert.assertNotNull(capturedHeaders);
        Map<String, String> headers = capturedHeaders.getValue();
        Assert.assertNotNull(headers);

        Assert.assertEquals(acls.size(), headers.size());

        String prefix = HEADER_PREFIX + ACL_PREFIX;

        Set<String> headerKeys = headers.keySet();
        for (String acl : acls.keySet()) {
            Assert.assertTrue(headerKeys.contains(prefix + acl));

            String aclProp = headers.get(prefix + acl);
            Assert.assertEquals(acls.get(acl), AclType.valueOf(aclProp));
        }
    }

    private Map<String, AclType> createACLsForTest(String name) {
        String prefix = HEADER_PREFIX + ACL_PREFIX;
        String name0 = prefix + "name0";
        String name1 = name;
        String name2 = prefix + "name2";
        String name3 = prefix + "name3";

        AclType value0 = AclType.READ;
        AclType value1 = AclType.READ;
        AclType value2 = AclType.WRITE;
        AclType value3 = AclType.WRITE;

        Map<String, AclType> acls = new HashMap<String, AclType>();
        acls.put(name0, value0);
        acls.put(name1, value1);
        acls.put(name2, value2);
        acls.put(name3, value3);
        return acls;
    }

    private Capture<Map<String, String>> createSetSpaceACLsMocks(String spaceId,
                                                                 String storeId,
                                                                 int status)
        throws Exception {
        EasyMock.expect(response.getStatusCode()).andReturn(status);

        Header header = EasyMock.createMock(Header.class);
        String fullURL = baseURL + "/acl/" + spaceId + "?storeID=" + storeId;
        Capture<Map<String, String>> capturedHeaders =
            new Capture<Map<String, String>>();
        EasyMock.expect(restHelper.post(EasyMock.eq(fullURL),
                                        EasyMock.<String>isNull(),
                                        EasyMock.capture(capturedHeaders)))
                .andReturn(response);

        EasyMock.replay(header);
        replayMocks();

        return capturedHeaders;
    }

    @Test
    public void testGetSpaceACLs() throws Exception {
        String name = "name1";
        Map<String, AclType> acls = createACLsForTest(name);

        String spaceId = "space-id";
        createGetSpaceACLsMocks(acls, spaceId, storeId, 200);

        // call being tested
        Map<String, AclType> spaceACLs = contentStore.getSpaceACLs(spaceId);
        Assert.assertNotNull(spaceACLs);

        // header without the proper x-dura-meta- prefix is omitted.
        Assert.assertEquals(acls.size() - 1, spaceACLs.size());
        Assert.assertTrue(!spaceACLs.containsKey(name));
        Assert.assertNotNull(acls.remove(name));

        Set<String> spaceACLKeys = spaceACLs.keySet();
        String aclHeaderPrefix = HEADER_PREFIX + ACL_PREFIX;
        for (String acl : acls.keySet()) {
            String aclNoPrefix = acl.substring(aclHeaderPrefix.length());
            Assert.assertTrue(spaceACLKeys.contains(aclNoPrefix));
            Assert.assertEquals(acls.get(acl), spaceACLs.get(aclNoPrefix));
        }
    }

    private void createGetSpaceACLsMocks(Map<String, AclType> acls,
                                         String spaceId,
                                         String storeId,
                                         int status) throws Exception {
        Header header = EasyMock.createMock(Header.class);
        Header[] headers = new Header[acls.size()];
        int i = 0;
        for (String acl : acls.keySet()) {
            headers[i++] = new Header(acl, acls.get(acl).name());
        }

        EasyMock.expect(response.getStatusCode()).andReturn(status);
        EasyMock.expect(response.getResponseHeaders()).andReturn(headers);

        String fullURL = baseURL + "/acl/" + spaceId + "?storeID=" + storeId;
        EasyMock.expect(restHelper.head(EasyMock.eq(fullURL))).andReturn(
            response);

        EasyMock.replay(header);
        replayMocks();
    }

    @Test
    public void testGetSupportedTasks() throws Exception {
        List<String> supportedtaskList = new ArrayList<>();
        supportedtaskList.add("task1");
        String xml = SerializationUtil.serializeList(supportedtaskList);

        String fullURL = baseURL + "/task" + "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(response.getResponseBody()).andReturn(xml);
        EasyMock.expect(restHelper.get(fullURL)).andReturn(response);

        replayMocks();

        List<String> taskList = contentStore.getSupportedTasks();
        Assert.assertEquals(supportedtaskList, taskList);
    }

    @Test
    public void testPerformTask() throws Exception {
        String taskName = "task1";
        String taskParams = "params";

        String fullURL = baseURL + "/task/" + taskName + "?storeID=" + storeId;
        EasyMock.expect(response.getStatusCode()).andReturn(200);
        EasyMock.expect(response.getResponseBody()).andReturn("success");
        EasyMock.expect(restHelper.post(fullURL, taskParams, null))
                .andReturn(response);

        replayMocks();

        String result = contentStore.performTask(taskName, taskParams);
        Assert.assertEquals("success", result);
    }

}
