package org.auscope.portal.server.web.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.auscope.portal.server.cloud.CloudFileInformation;
import org.auscope.portal.server.vegl.VEGLJob;
import org.springframework.stereotype.Service;


public interface IStorageStrategy {

    /**
     * Gets an input stream to the file specified by key
     *
     * @param job
     *            The job that owns the files
     * @param key
     *            The key of the file to download
     * @return
     * @throws ServiceException
     */
    public InputStream getJobFileData(VEGLJob job, String key)
            throws CloudStorageException;

    /**
     * Gets information about all output files for a given job
     *
     * @param job
     *            The job to examine
     * @return
     * @throws S3ServiceException
     */
    public CloudFileInformation[] getOutputFileDetails(VEGLJob job)
            throws CloudStorageException;

    /**
     * Uploads the specified files to the job's input storage area
     *
     * @param job
     *            The job who will 'own' these input files
     * @param files
     *            The input files to be uploaded.
     * @throws S3ServiceException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public void uploadInputJobFiles(VEGLJob job, File[] files)
            throws CloudStorageException, NoSuchAlgorithmException, IOException;
}