/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.storage.provider;

import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.error.StorageException;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A Storage Provider provides services which allow content to be
 * stored in and retrieved from spaces.
 *
 * @author Bill Branan
 */
public interface StorageProvider {

    public enum AccessType {OPEN, CLOSED}

    /* Space property names */
    public static final String PROPERTIES_SPACE_CREATED = "space-created";
    public static final String PROPERTIES_SPACE_COUNT = "space-count";
    public static final String PROPERTIES_SPACE_SIZE = "space-total-size";
    public static final String PROPERTIES_SPACE_ACCESS = "space-access";

    /* Content property names */
    public static final String PROPERTIES_CONTENT_MIMETYPE = "content-mimetype";
    public static final String PROPERTIES_CONTENT_SIZE = "content-size";
    public static final String PROPERTIES_CONTENT_CHECKSUM = "content-checksum";
    public static final String PROPERTIES_CONTENT_MODIFIED = "content-modified";

    /* Reserved property names */
    public static final String PROPERTIES_CONTENT_MD5 = "content-md5";

    /* Name values for property files */
    public static final String SPACE_PROPERTIES_SUFFIX = "-space-metadata";

    /* Other constants */
    public static final String DEFAULT_MIMETYPE = "application/octet-stream";
    public static final DateFormat RFC822_DATE_FORMAT =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    public static final int HTTP_NOT_FOUND = 404;

    public static final long DEFAULT_MAX_RESULTS = 1000;

    /**
     * Provides a listing of all spaces owned by a customer.
     *
     * @return Iterator listing spaceIds
     */
    public Iterator<String> getSpaces();

    /**
     * Provides access to the content files within a space. Chunking of the
     * list is handled internally. Prefix can be set to return only content
     * IDs starting with the prefix value.
     *
     * @param spaceId - ID of the space
     * @param prefix - The prefix of the content id (null for no constraints)
     * @return Iterator of contentIds
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public Iterator<String> getSpaceContents(String spaceId,
                                             String prefix);

    /**
     * Provides a listing of the content files within a space. The number of
     * items returned is limited to maxResults (default is 1000). Retrieve
     * further results by including the last content ID in the previous list
     * as the marker. Set prefix to return only content IDs starting with the
     * prefix value.
     *
     * @param spaceId - ID of the space
     * @param prefix - Only retrieve content IDs with this prefix (null for all content ids)
     * @param maxResults - The maximum number of content IDs to return in the list (0 indicates default (1000))
     * @param marker - The content ID marking the last item in the previous set (null indicates the first set of ids)
     * @return List of contentIds
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public List<String> getSpaceContentsChunked(String spaceId,
                                                String prefix,
                                                long maxResults,
                                                String marker);

    /**
     * Creates a new space.
     *
     * Depending on the storage implementation, the spaceId may be
     * changed somewhat to comply with the naming rules of the
     * underlying storage provider. The same spaceId value used
     * here can be used in all other methods, as the conversion
     * will be applied internally, however a call to getSpaces()
     * may not include a space with exactly this same name.
     *
     * @param spaceId - ID of the space
     * @throws StorageException if space with ID spaceId already exists
     */
    public void createSpace(String spaceId);

    /**
     * Deletes a space.
     *
     * @param spaceId - ID of the space
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public void deleteSpace(String spaceId);

    /**
     * Retrieves the properties associated with a space.
     *
     * @param spaceId - ID of the space
     * @return Map of space properties or null if no properties exists
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public Map<String, String> getSpaceProperties(String spaceId);

    /**
     * Sets the properties associated with a space.
     *
     * @param spaceId - ID of the space
     * @param spaceProperties - Updated space properties
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public void setSpaceProperties(String spaceId,
                                   Map<String, String> spaceProperties);

    /**
     * Gets the access setting of the space, either OPEN or CLOSED. An OPEN space is
     * available for public viewing. A CLOSED space requires authentication prior to
     * viewing any of the contents.
     *
     * @param spaceId - ID of the space
     * @return the access type of the space, OPEN or CLOSED
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public AccessType getSpaceAccess(String spaceId);

    /**
     * Sets the accessibility of a space to either OPEN or CLOSED.
     *
     * @param spaceId - ID of the space
     * @param access - New space access value
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public void setSpaceAccess(String spaceId,
                               AccessType access);

    /**
     * Adds content to a space. Computes the checksum of the
     * provided content and checks this against the checksum
     * of the uploaded content to protect against loss or
     * corruption during transfer.
     *
     * @param spaceId - ID of the space
     * @param contentId - ID of the content in the space
     * @param contentMimeType - the MIME type of the content being added
     * @param contentSize - the file size (in bytes) of the content being added
     * @param contentChecksum - the MD5 checksum of the content being added (null if no checksum is known)
     * @param content - content to add
     * @return The checksum of the provided content
     * @throws NotFoundException if space with ID spaceId does not exist
     * @throws StorageException if errors occur
     */
    public String addContent(String spaceId,
                             String contentId,
                             String contentMimeType,
                             long contentSize,
                             String contentChecksum,
                             InputStream content);

    /**
     * This method copies the content item found in source-space with the id of
     * source-content-id into the dest-space, naming it to dest-content-id.
     *
     * @param sourceSpaceId   of content to copy
     * @param sourceContentId of content to copy
     * @param destSpaceId     where copied content will end up
     * @param destContentId   given to copied content
     * @return MD5 checksum of destination content item
     */
    public String copyContent(String sourceSpaceId,
                              String sourceContentId,
                              String destSpaceId,
                              String destContentId);

    /**
     * Gets content from a space.
     *
     * @param spaceId - ID of the space
     * @param contentId - ID of the content in the space
     * @return the content stream
     * @throws NotFoundException if space with ID spaceId does not exist or the
     *                           content item with ID contentId does not exist
     * @throws StorageException if errors occur
     */
    public InputStream getContent(String spaceId,
                                  String contentId);

    /**
     * Removes content from a space.
     *
     * @param spaceId - ID of the space
     * @param contentId - ID of the content in the space
     * @throws NotFoundException if space with ID spaceId does not exist or the
     *                           content item with ID contentId does not exist
     * @throws StorageException if errors occur
     */
    public void deleteContent(String spaceId,
                              String contentId);

    /**
     * Sets the properties associated with content. This effectively
     * removes all of the current content properties and adds a new
     * set of properties. Some properties, such as system properties
     * provided by the underlying storage system, cannot be updated
     * or removed.
     *
     * Some of the values which cannot be updated or removed:
     * Content-MD5
     * ETag
     * Last-Modified
     *
     * Content-Type cannot be removed, but it can be updated
     *
     * @param spaceId - ID of the space
     * @param contentId - ID of the content in the space
     * @param contentProperties - new content properties
     * @throws NotFoundException if space with ID spaceId does not exist or the
     *                           content item with ID contentId does not exist
     * @throws StorageException if errors occur
     */
    public void setContentProperties(String spaceId,
                                     String contentId,
                                     Map<String, String> contentProperties);

    /**
     * Retrieves the properties associated with content. This includes
     * both properties generated by the underlying storage system as
     * well as custom properties.
     *
     * Use the PROPERTIES_CONTENT_* constants to retrieve standard
     * properties values.
     *
     * @param spaceId - ID of the space
     * @param contentId - ID of the content in the space
     * @return content properties
     * @throws NotFoundException if space with ID spaceId does not exist or the
     *                           content item with ID contentId does not exist
     * @throws StorageException if errors occur
     */
    public Map<String, String> getContentProperties(String spaceId,
                                                    String contentId);

}
