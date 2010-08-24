/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.hadoop.imageconversion;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.duracloud.services.hadoop.base.ProcessFileMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Mapper used to perform image conversion.
 *
 * @author: Bill Branan
 * Date: Aug 13, 2010
 */
public class ImageConversionMapper extends ProcessFileMapper {

    public static final String DATE = "date";
    public static final String PROC_TIME = "processing-time";
    public static final String SRC_SIZE = "source-file-bytes";

    /**
     * Converts an image file.
     *
     * @param file the file to be converted
     * @return the converted file
     */
    @Override
    protected File processFile(File file) throws IOException {
        resultInfo.put(SRC_SIZE, String.valueOf(file.length()));

        String destFormat = jobConf.get(ICInitParamParser.DEST_FORMAT);
        String colorSpace = jobConf.get(ICInitParamParser.COLOR_SPACE);

        File workDir = file.getParentFile();
        File script = createScript(workDir, colorSpace);

        long startProcTime = System.currentTimeMillis();
        File resultFile = convertImage(script.getAbsolutePath(),
                                       file,
                                       destFormat,
                                       workDir);
        long processingTime = System.currentTimeMillis() - startProcTime;
        resultInfo.put(PROC_TIME, String.valueOf(processingTime));

        return resultFile;
    }

    @Override
    protected String collectResult() throws IOException {                
        String result = super.collectResult();
        result += ", " + PROC_TIME + "=" + resultInfo.get(PROC_TIME);
        result += ", " + SRC_SIZE + "=" +  resultInfo.get(SRC_SIZE);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String now = format.format(new Date(System.currentTimeMillis()));
        result += ", " + DATE + "=" + now;

        return result;
    }

    /*
    * Converts a local image to a given format using ImageMagick.
    * Returns the name of the converted image.
    */
    private File convertImage(String convertScript,
                              File sourceFile,
                              String toFormat,
                              File workDir)
        throws IOException {
        String fileName = sourceFile.getName();

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

    /*
     * Creates the script used to perform conversions
     */
    protected File createScript(File workDir, String colorSpace)
        throws IOException {
        String fileName = "convert.sh";
        List<String> scriptLines = new ArrayList<String>();
        scriptLines.add("#!/bin/bash");
        if(colorSpace != null && colorSpace.equals("sRGB")) {
            String csFileName = "sRGB.icm";
            copyFileToWork(workDir, csFileName);
            scriptLines.add("sudo mogrify -profile "+csFileName+" $2");
        }
        scriptLines.add("sudo mogrify -format $1 $2");

        File scriptFile = new File(workDir, fileName);
        FileUtils.writeLines(scriptFile, scriptLines);
        scriptFile.setExecutable(true);
        return scriptFile;
    }

    private void copyFileToWork(File workDir, String fileName)
        throws IOException {
        InputStream inStream = ImageConversionMapper.class.getClassLoader().
                               getResourceAsStream(fileName);
        FileOutputStream outStream =
            new FileOutputStream(new File(workDir, fileName));
        IOUtils.copy(inStream, outStream);
    }

}