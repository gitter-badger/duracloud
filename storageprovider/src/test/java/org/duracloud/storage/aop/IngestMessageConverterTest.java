/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.storage.aop;

import org.duracloud.storage.aop.ContentMessage;
import org.duracloud.storage.aop.IngestMessage;
import org.duracloud.storage.aop.IngestMessageConverter;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.support.converter.MessageConversionException;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class IngestMessageConverterTest {
    private IngestMessageConverter ingestMessageConverter;

    @Before
    public void setUp() throws Exception {
        ingestMessageConverter = new IngestMessageConverter();
    }

    @Test
    public void testFromConversionException() throws JMSException {
        try{
            Message msg = EasyMock.createMock("Message",
                                              Message.class);

            ingestMessageConverter.fromMessage(msg);
            assertTrue(false);

        } catch(MessageConversionException mce) {
            assertTrue(true);
        }
    }

    @Test
    public void testToConversionException() throws JMSException {
        try{
            ingestMessageConverter.toMessage((Object) "", null);
            assertTrue(false);

        } catch(MessageConversionException mce) {
            assertTrue(true);
        }
    }

    @Test
    public void testFromMessage() throws JMSException {
        String storeId = "storeId";
        String spaceId = "spaceId";
        String contentId = "contentId";
        String mimeType = "mimeType";
        String username = "username";
        long contentSize = 1234;
        String contentMd5 = "contentMd5";
        String action = ContentMessage.ACTION.INGEST.name();

        MapMessage msg = EasyMock.createMock("MapMessage",
                                             MapMessage.class);

        msg.getStringProperty(IngestMessageConverter.STORE_ID);
        EasyMock.expectLastCall().andReturn(storeId);

        msg.getString(IngestMessageConverter.SPACE_ID);
        EasyMock.expectLastCall().andReturn(spaceId);

        msg.getString(IngestMessageConverter.CONTENT_ID);
        EasyMock.expectLastCall().andReturn(contentId);

        msg.getString(IngestMessageConverter.MIMETYPE);
        EasyMock.expectLastCall().andReturn(mimeType);

        msg.getString(IngestMessageConverter.USERNAME);
        EasyMock.expectLastCall().andReturn(username);

        msg.getString(IngestMessageConverter.CONTENT_MD5);
        EasyMock.expectLastCall().andReturn(contentMd5);

        msg.getLong(IngestMessageConverter.CONTENT_SIZE);
        EasyMock.expectLastCall().andReturn(contentSize);

        msg.getString(IngestMessageConverter.ACTION);
        EasyMock.expectLastCall().andReturn(action);

        EasyMock.replay(msg);
        Object obj = ingestMessageConverter.fromMessage(msg);
        EasyMock.verify(msg);

        assertNotNull(obj);
        assertTrue(obj instanceof IngestMessage);

        IngestMessage ingestMessage = (IngestMessage) obj;
        assertEquals(storeId, ingestMessage.getStoreId());
        assertEquals(spaceId, ingestMessage.getSpaceId());
        assertEquals(contentId, ingestMessage.getContentId());
        assertEquals(mimeType, ingestMessage.getContentMimeType());
        assertEquals(username, ingestMessage.getUsername());
        assertEquals(contentMd5, ingestMessage.getContentMd5());
        assertEquals(contentSize, ingestMessage.getContentSize());
        assertEquals(action, ingestMessage.getAction());
    }

    @Test
    public void testToMessage() throws JMSException {
        String storeId = "storeId";
        String spaceId = "spaceId";
        String contentId = "contentId";
        String mimeType = "mimeType";
        String username = "username";
        long contentSize = 1234;
        String contentMd5 = "contentMd5";
        String action = ContentMessage.ACTION.INGEST.name();

        MapMessage mapMsg = EasyMock.createMock("MapMessage",
                                                MapMessage.class);
        Session session = EasyMock.createMock("Session",
                                              Session.class);

        session.createMapMessage();
        EasyMock.expectLastCall().andReturn(mapMsg);

        mapMsg.setStringProperty(IngestMessageConverter.STORE_ID, storeId);
        EasyMock.expectLastCall().once();

        mapMsg.setStringProperty(IngestMessageConverter.SPACE_ID, spaceId);
        EasyMock.expectLastCall().once();

        mapMsg.setString(IngestMessageConverter.SPACE_ID, spaceId);
        EasyMock.expectLastCall().once();

        mapMsg.setString(IngestMessageConverter.CONTENT_ID, contentId);
        EasyMock.expectLastCall().once();

        mapMsg.setString(IngestMessageConverter.MIMETYPE, mimeType);
        EasyMock.expectLastCall().once();

        mapMsg.setString(IngestMessageConverter.USERNAME, username);
        EasyMock.expectLastCall().once();

        mapMsg.setString(IngestMessageConverter.CONTENT_MD5, contentMd5);
        EasyMock.expectLastCall().once();

        mapMsg.setLong(IngestMessageConverter.CONTENT_SIZE, contentSize);
        EasyMock.expectLastCall().once();

        mapMsg.setString(IngestMessageConverter.ACTION, action);
        EasyMock.expectLastCall().once();

        IngestMessage ingestMessage = new IngestMessage();
        ingestMessage.setStoreId(storeId);
        ingestMessage.setSpaceId(spaceId);
        ingestMessage.setContentId(contentId);
        ingestMessage.setContentMimeType(mimeType);
        ingestMessage.setUsername(username);
        ingestMessage.setContentSize(contentSize);
        ingestMessage.setContentMd5(contentMd5);
        ingestMessage.setAction(action);

        EasyMock.replay(mapMsg);
        EasyMock.replay(session);
        Message msg = ingestMessageConverter.toMessage((Object)ingestMessage,
                                                      session);
        EasyMock.verify(mapMsg);
        EasyMock.verify(session);

        assertNotNull(msg);
        assertTrue(msg instanceof MapMessage);
    }
}
