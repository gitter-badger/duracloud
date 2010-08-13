/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.fixity.results;

import org.duracloud.client.ContentStore;
import org.duracloud.error.ContentStoreException;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Andrew Woods
 *         Date: Aug 9, 2010
 */
public class ServiceResultProcessorTest {

    private ServiceResultProcessor processor;
    private String outputSpaceId = "output-space-id";
    private String outputContentId = "output-content-id";
    private String mime = "text/csv";

    private final String SPACE_PREFIX = "test-space-id-";
    private final String CONTENT_PREFIX = "test-content-id-";
    private final String HASH_PREFIX = "test-hash-";

    private File workDir = new File("target/test-result-processor");
    BufferedReader reader;

    @Before
    public void setUp() throws Exception {
        if (!workDir.exists()) {
            Assert.assertTrue(workDir.mkdir());
        }

        ContentStore contentStore = createContentStore();
        processor = new ServiceResultProcessor(contentStore,
                                               outputSpaceId,
                                               outputContentId,
                                               "test-hashing",
                                               "previous status blah",
                                               workDir);
    }

    @After
    public void tearDown() throws Exception {
        processor = null;
        if (reader != null) {
            reader.close();
        }
    }

    @Test
    public void testSetProcessingComplete() throws Exception {
        String status = processor.getProcessingStatus();
        Assert.assertNotNull(status);

        ServiceResultListener.StatusMsg msg = new ServiceResultListener.StatusMsg(
            status);
        Assert.assertEquals(ServiceResultListener.State.IN_PROGRESS,
                            msg.getState());

        processor.setProcessingState(ServiceResultListener.State.COMPLETE);
        status = processor.getProcessingStatus();
        Assert.assertNotNull(status);

        msg = new ServiceResultListener.StatusMsg(status);
        Assert.assertEquals(ServiceResultListener.State.COMPLETE,
                            msg.getState());
    }

    @Test
    public void testProcessServiceResult() throws Exception {
        ServiceResultListener.State inProgress = ServiceResultListener.State.IN_PROGRESS;

        boolean success = true;
        int index = 0;
        processResult(success, index++);
        verifyStatus(inProgress.name(), "1", "0", "?");

        success = true;
        processResult(success, index++);
        verifyStatus(inProgress.name(), "2", "0", "?");

        success = false;
        processResult(success, index++);
        verifyStatus(inProgress.name(), "3", "1", "?");

        processor.setTotalWorkItems(5);
        verifyStatus(inProgress.name(), "3", "1", "5");

        success = true;
        processResult(success, index++);
        verifyStatus(inProgress.name(), "4", "1", "5");

        success = false;
        processResult(success, index++);
        verifyStatus(inProgress.name(), "5", "2", "5");

        verifyLocalOutputFile();
    }

    private void processResult(boolean success, int index) {
        ServiceResult result = new HashFinderResult(success,
                                                    SPACE_PREFIX + index,
                                                    CONTENT_PREFIX + index,
                                                    HASH_PREFIX + index);

        processor.processServiceResult(result);
    }

    private void verifyStatus(String status,
                              String numProcessed,
                              String numFailure,
                              String totalWorkItems) {
        String text = processor.getProcessingStatus();
        Assert.assertNotNull(text);
        System.out.println("s: '" + text + "'");
        ServiceResultListener.StatusMsg msg = new ServiceResultListener.StatusMsg(
            text);

        Assert.assertEquals(status, msg.getState().name());
        Assert.assertEquals(Long.parseLong(numFailure), msg.getFailed());
        Assert.assertEquals(Long.parseLong(numProcessed),
                            msg.getPassed() + msg.getFailed());

        long totalExpected = -1;
        if (!totalWorkItems.equals("?")) {
            totalExpected = Long.parseLong(totalWorkItems);
        }
        Assert.assertEquals(totalExpected, msg.getTotal());


    }

    private void verifyLocalOutputFile() throws IOException {
        HashFinderResult result = new HashFinderResult(true, "", "", "");
        File file = new File(workDir, outputContentId);
        Assert.assertTrue(file.exists());

        reader = new BufferedReader(new FileReader(file));
        int i = -1;
        String line;
        while ((line = reader.readLine()) != null) {
            if (i == -1) {
                Assert.assertEquals(result.getHeader(), line);
            } else {
                StringBuilder expected = new StringBuilder(SPACE_PREFIX);
                expected.append(i);
                expected.append(",");
                expected.append(CONTENT_PREFIX);
                expected.append(i);
                expected.append(",");
                expected.append(HASH_PREFIX);
                expected.append(i);
                Assert.assertEquals(expected.toString(), line);
            }
            System.out.println(line);
            i++;
        }
        Assert.assertEquals(5, i);
    }

    private ContentStore createContentStore() throws ContentStoreException {
        ContentStore store = EasyMock.createMock("ContentStore",
                                                 ContentStore.class);

        EasyMock.expect(store.addContent(EasyMock.eq(outputSpaceId),
                                         EasyMock.eq(outputContentId),
                                         EasyMock.<InputStream>anyObject(),
                                         EasyMock.anyLong(),
                                         EasyMock.eq(mime),
                                         EasyMock.<String>anyObject(),
                                         EasyMock.<Map<String, String>>anyObject()))
            .andReturn("checksum")
            .anyTimes();
        EasyMock.replay(store);

        return store;
    }

}