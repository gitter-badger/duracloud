/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.image;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.duracloud.client.ContentStore;
import org.duracloud.domain.Content;
import org.duracloud.error.ContentStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

/**
 * @author: Bill Branan
 * Date: Apr 22, 2010
 */
public class ConversionWorker implements Runnable {

    private final Logger log = LoggerFactory.getLogger(ConversionWorker.class);

    private ContentStore contentStore;
    private String sourceSpaceId;
    private String destSpaceId;
    private String contentId;
    private File workDir;
    private String toFormat;
    private Map<String, String> extMimeMap;
    private String convertScript;
    private ConversionResultListener resultListener;

    public ConversionWorker(ContentStore contentStore,
                            String sourceSpaceId,
                            String destSpaceId,
                            String contentId,
                            File workDir,
                            String toFormat,
                            Map<String, String> extMimeMap,
                            String convertScript,
                            ConversionResultListener resultListener) {
        this.contentStore = contentStore;
        this.sourceSpaceId = sourceSpaceId;
        this.destSpaceId = destSpaceId;
        this.contentId = contentId;
        this.workDir = workDir;
        this.toFormat = toFormat;
        this.extMimeMap = extMimeMap;
        this.convertScript = convertScript;
        this.resultListener = resultListener;
    }

    public void run() {
        performConversion();
    }

    private void performConversion() {
        long sourceSize = 0;
        long startTime = System.currentTimeMillis();
        long conversionTime = 0;
        try {
            // Stream the content item down to the work directory
            Content sourceContent =
                contentStore.getContent(sourceSpaceId, contentId);

            InputStream sourceStream = sourceContent.getStream();

            File sourceFile = null;
            File convertedFile = null;
            try {
                String sourceFileName = getSourceFileName(contentId);
                sourceFile = writeSourceToFile(sourceStream, sourceFileName);
                sourceSize = sourceFile.length();

                // Perform conversion
                long startConTime = System.currentTimeMillis();
                convertedFile = convertImage(sourceFile);
                conversionTime = System.currentTimeMillis() - startConTime;

                String destContentId =
                    getDestContentId(contentId, convertedFile);

                // Store the converted file in the destination space
                storeConvertedContent(convertedFile,
                                      destContentId,
                                      sourceContent.getProperties());
            } finally {
                // Delete source and converted files from work directory
                if(sourceFile != null) {
                    if (!sourceFile.delete()) {
                        sourceFile.deleteOnExit();
                    }
                }

                if(convertedFile != null) {
                    if (!convertedFile.delete()) {
                        convertedFile.deleteOnExit();
                    }
                }
            }
        } catch(Exception e) {
            sendResult(false, e.getMessage(), startTime, conversionTime,
                       sourceSize);
            return;
        }
        sendResult(true, null, startTime, conversionTime, sourceSize);
    }

    private void sendResult(boolean success,
                            String errMessage,
                            long startTime,
                            long totalConversionTime,
                            long sourceSize) {
        long now = System.currentTimeMillis();
        long totalTime = now - startTime;
        ConversionResult result =
            new ConversionResult(new Date(now), sourceSpaceId, destSpaceId,
                                 contentId, success, errMessage,
                                 totalConversionTime, totalTime, sourceSize);
        resultListener.processConversionResult(result);
    }

    protected File writeSourceToFile(InputStream sourceStream,
                                     String fileName) throws IOException {
        File sourceFile = new File(workDir, fileName);
        if(sourceFile.exists()) {
            sourceFile.delete();
        }
        sourceFile.createNewFile();
        FileOutputStream sourceOut = new FileOutputStream(sourceFile);

        try {
            long sizeCopied = IOUtils.copyLarge(sourceStream, sourceOut);
            if(sizeCopied <= 0) {
                throw new IOException("Unable to copy any bytes from file " +
                    fileName);
            }
        } finally {
            sourceStream.close();
            sourceOut.close();
        }
        return sourceFile;
    }

    protected String getSourceFileName(String contentId) {
        return contentId
            .replace("/", "_")
            .replace("\\", "_")
            .replace(" ", "_");
    }

    /*
     * Converts a local image to a given format using ImageMagick.
     * Returns the name of the converted image.
     */
    private File convertImage(File sourceFile) throws IOException {
        String fileName = sourceFile.getName();
        log("Converting " + fileName + " to " + toFormat);

        ProcessBuilder pb =
            new ProcessBuilder(convertScript, toFormat, fileName);
        pb.directory(workDir);
        Process p = pb.start();

        try {
            p.waitFor();  // Wait for the conversion to complete
        } catch (InterruptedException e) {
            throw new IOException("Conversion process interruped for " +
                fileName, e);
        }

        String convertedFileName = FilenameUtils.getBaseName(fileName);
        convertedFileName += "." + toFormat;
        File convertedFile = new File(workDir, convertedFileName);
        if(convertedFile.exists()) {
            return convertedFile;
        } else {
            throw new IOException("Could not find converted file: " +
                convertedFileName);
        }
    }

    protected String getDestContentId(String sourceContentId,
                                      File destFile) {
        String destExt = FilenameUtils.getExtension(destFile.getName());
        return FilenameUtils.removeExtension(sourceContentId) + "." + destExt;
    }

    private void storeConvertedContent(File convertedFile,
                                       String destContentId,
                                       Map<String, String> properties)
        throws IOException, ContentStoreException {
        ContentStoreException exception = null;

        boolean success = false;
        int maxLoops = 4;
        for (int loops = 0; !success && loops < maxLoops; loops++) {
            FileInputStream convertedFileStream =
                new FileInputStream(convertedFile);
            String mimetype = extMimeMap.get(toFormat);
            if(mimetype == null) {
                mimetype = extMimeMap.get("default");
            }

            try {
                contentStore.addContent(destSpaceId,
                                        destContentId,
                                        convertedFileStream,
                                        convertedFile.length(),
                                        mimetype,
                                        null,
                                        properties);
                success = true;
            } catch (ContentStoreException e) {
                exception = e;
                success = false;
            } finally {
                convertedFileStream.close();
            }

            if (!success) {
                throw exception;
            }
        }
    }

    private void log(String logMsg) {
        log.info(logMsg);
    }

}