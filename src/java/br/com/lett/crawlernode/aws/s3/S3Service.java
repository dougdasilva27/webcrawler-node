package br.com.lett.crawlernode.aws.s3;

import br.com.lett.crawlernode.aws.ec2.TransferOverFTPS;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.FileCompression;
import br.com.lett.crawlernode.util.Logging;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;

/**
 * Utility class to export some methods more user-friendly to our needs to operate on S3.
 *
 * @author Samir Leao
 */
public class S3Service {

    public static final String SCREENSHOT_UPLOAD_TYPE = "screenshot";
    public static final String HTML_UPLOAD_TYPE = "html";
    public static final String MD5_HEX_METADATA_FIELD = "md5hex";
    public static final String MD5_ORIGINAL_HEX_FIELD = "originalmd5hex";
    public static final String POSITION = "position";
    public static final String LOCAL_PATH = "src/resources/";
    protected static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    // Amazon images
    private static final AmazonS3 s3clientImages;
    private static final String LOGS_BUCKET_NAME = GlobalConfigurations.executionParameters.getLogsBucketName();
    private static final String S3_CRAWLER_SESSIONS_PREFIX = "crawler-sessions";
    private static final String S3_BATCH_USER = GlobalConfigurations.executionParameters.getS3BatchUser();
    private static final String S3_BATCH_PASS = GlobalConfigurations.executionParameters.getS3BatchPass();
    private static final String S3_BATCH_HOST = GlobalConfigurations.executionParameters.getS3BatchHost();
    private static final String S3_BATCH_REMOTE_LOCATION = GlobalConfigurations.executionParameters.getS3BatchRemoteLocation();
    private static final String READ_IMAGES_BUCKET_NAME = GlobalConfigurations.executionParameters.getImagesBucketName();
    // Amazon crawler-session
    private static final AmazonS3 s3clientCrawlerSessions;

    static {
        s3clientImages = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();

        s3clientCrawlerSessions = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    /**
     * @param session
     * @param name
     * @return
     */
    public static ObjectMetadata fetchObjectMetadata(Session session, String name) {
        try {
            return s3clientImages.getObjectMetadata(READ_IMAGES_BUCKET_NAME, name);
        } catch (AmazonS3Exception s3Exception) {
            if (s3Exception.getStatusCode() == 404) {
                Logging.printLogWarn(logger, session, "S3 status code: 404 [object metadata not found]");
                return null;
            } else {
                Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(s3Exception));
                return null;
            }
        } catch (Exception e) {
            Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
            return null;
        }
    }

    public static S3Object fetchS3Object(Session session, String name) {
        try {
            return s3clientImages.getObject(READ_IMAGES_BUCKET_NAME, name);
        } catch (AmazonS3Exception s3Exception) {
            if (s3Exception.getStatusCode() == 404) {
                Logging.printLogWarn(logger, session, "S3 status code: 404 [object metadata not found]");
                return null;
            } else {
                Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(s3Exception));
                return null;
            }
        } catch (Exception e) {
            Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
            return null;
        }
    }

    /**
     * Uploads the current image being processed in the session object to S3.
     *
     * @param session
     * @param newObjectMetadata the user metadata that goes into the s3object
     * @param f                 the local file
     * @param key               the path for the image in the S3 bucket
     * @throws FileNotFoundException if the local temporary file of the downloaded image was not found
     */
    public static void uploadImage(Session session, ObjectMetadata newObjectMetadata, File f, String key, String bucket) throws FileNotFoundException {

        ImageCrawlerSession s = (ImageCrawlerSession) session;
        FileInputStream fileInputStream = new FileInputStream(f);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, fileInputStream, newObjectMetadata);

        try {
            s3clientImages.putObject(putObjectRequest);
            Logging.printLogDebug(logger, session, "[BUCKET - " + bucket + "] Uploaded image #" + s.getImageNumber() + " with success!");

        } catch (AmazonClientException ace) {
            Logging.printLogError(logger, session, "[BUCKET - " + bucket + "] Error Message:    " + ace.getMessage());
        }
    }

    public static String getImageMd5(S3Object s3Object) {
        return s3Object.getObjectMetadata().getUserMetaDataOf(MD5_HEX_METADATA_FIELD);
    }

    /**
     * Uploads a String as file to Amazon.
     *
     * @param session
     * @param file
     */
    public static void uploadCrawlerSessionContentToAmazon(Session session) {
        try {
            String tarPath = new StringBuilder()
                    .append(LOCAL_PATH)
                    .append(session.getSessionId())
                    .append(".tar")
                    .toString();

            String tarGzPath = new StringBuilder()
                    .append(LOCAL_PATH)
                    .append(session.getSessionId())
                    .append(".tar.gz")
                    .toString();

            FileCompression.compressToTar(tarPath, session.getResponseBodiesPath());
            FileCompression.compressFileToGZIP(tarPath, tarGzPath, true, session);

            DateTime time = new DateTime();

            String amazonLocation = new StringBuilder()
                    .append(S3_CRAWLER_SESSIONS_PREFIX)
                    .append("/")
                    .append(session.getClass().getSimpleName())
                    .append("/year=")
                    .append(time.getYear())
                    .append("/month=")
                    .append(time.getMonthOfYear())
                    .append("/day=")
                    .append(time.getDayOfMonth())
                    .append("/")
                    .append(session.getSessionId())
                    .append(".tar.gz")
                    .toString();

            File compressedFile = new File(tarGzPath);

            if (compressedFile.exists()) {
                try {
                    Logging.printLogDebug(logger, session, "Uploading HTML to S3 Batch service");

                    String localFile = compressedFile.getAbsolutePath();
                    String remoteFile = Paths.get(S3_BATCH_REMOTE_LOCATION, LOGS_BUCKET_NAME, amazonLocation).toString();

                    TransferOverFTPS sftp = new TransferOverFTPS(S3_BATCH_USER, S3_BATCH_PASS, S3_BATCH_HOST);
                    sftp.sendFileAsyncAndCloseConnection(localFile, remoteFile);

                    Logging.printLogDebug(logger, session, "HTML uploaded successfully!");
                } catch (Exception ex) {
                    Logging.printLogError(logger, session, "Error during HTML upload.");
                    Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
                }

//                // TODO: remove S3 endpoint after validating batch in production VVVVVVV
//                try {
//                    Logging.printLogDebug(logger, session, "Uploading content to S3");
//                    s3clientCrawlerSessions.putObject(new PutObjectRequest(LOGS_BUCKET_NAME, amazonLocation, compressedFile));
//                    Logging.printLogDebug(logger, session, "Content uploaded to S3 successfully!");
//                } catch (AmazonServiceException ase) {
//                    Logging.printLogError(logger, session, " - Caught an AmazonServiceException, which " + "means your request made it "
//                            + "to Amazon S3, but was rejected with an error response" + " for some reason.");
//                    Logging.printLogError(logger, session, "Error Message:    " + ase.getMessage());
//                    Logging.printLogError(logger, session, "HTTP Status Code: " + ase.getStatusCode());
//                    Logging.printLogError(logger, session, "AWS Error Code:   " + ase.getErrorCode());
//                    Logging.printLogError(logger, session, "Error Type:       " + ase.getErrorType());
//                    Logging.printLogError(logger, session, "Request ID:       " + ase.getRequestId());
//
//                    ase.printStackTrace();
//                } catch (AmazonClientException ace) {
//                    Logging.printLogError(logger, session, " - Caught an AmazonClientException, which " + "means the client encountered "
//                            + "an internal error while trying to " + "communicate with S3, " + "such as not being able to access the network.");
//                    Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ace));
//                } catch (Exception ex) {
//                    Logging.printLogError(logger, session, "Error during html upload.");
//                    Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
//                }
//                // TODO: remove S3 endpoint after validating batch in production ^^^^^^^^^^^^^^^^^^

                compressedFile.delete();
            } else {
                Logging.printLogDebug(logger, session, "No files to upload!");
            }
        } catch (IOException e) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
        }
    }

    /**
     * Uploads a String as file to Amazon.
     *
     * @param session
     * @param file
     */
    public static void saveResponseContent(Session session, String requestHash, String html) {

        if (!(session instanceof TestCrawlerSession)) {
            String path = new StringBuilder().append(LOCAL_PATH).append(requestHash).append(".html").toString();
            session.addResponseBodyPath(path);

            try {
                Logging.printLogDebug(logger, session, "Save content to upload to Amazon");
                File htmlFile = new File(path);
                FileUtils.writeStringToFile(htmlFile, html);

                BufferedWriter out = new BufferedWriter(new FileWriter(htmlFile));

                out.write(html);
                out.close();
            } catch (IOException ex) {
                Logging.printLogError(logger, session, "Error writing String to file during html upload.");
                Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
            }
        }
    }

    public static File fetchImageFromAmazon(Session session, String key) {
        Logging.printLogDebug(logger, session, "Fetching image from Amazon: " + key);

        try {
            S3Object object = s3clientImages.getObject(new GetObjectRequest(READ_IMAGES_BUCKET_NAME, key));

            InputStream reader = new BufferedInputStream(object.getObjectContent());

            File file = File.createTempFile("fetchImageFromAmazon", ".jpg");
            OutputStream writer;

            writer = new BufferedOutputStream(new FileOutputStream(file));

            int read = -1;

            while ((read = reader.read()) != -1) {
                writer.write(read);
            }

            writer.flush();
            writer.close();
            reader.close();

            Logging.printLogDebug(logger, session, "Fetched at: " + file.getAbsolutePath());

            return file;

        } catch (AmazonS3Exception s3Exception) {
            if (s3Exception.getStatusCode() == 404) {
                Logging.printLogWarn(logger, session, "S3 status code: 404 [image not found]");
                return null;
            } else {
                Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(s3Exception));
                return null;
            }
        } catch (Exception e) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            return null;
        }

    }

    /**
     * Fetch the image md5 from the user metadata field on the s3object. The key passed as parameter
     * must be the key of an image file. <br>
     * Only the images in the bucket have the md5 as an user metadata.
     *
     * @param session
     * @param key     the image path on S3 bucket
     * @return <br>
     * the image md5 <br>
     * null if the object wasn't found or if it isn't an image
     */
    public static String fetchOriginalImageMd5(Session session, String key) {
        try {
            ObjectMetadata objectMetadata = s3clientImages.getObjectMetadata(new GetObjectMetadataRequest(READ_IMAGES_BUCKET_NAME, key));

            if (objectMetadata != null) {
                return objectMetadata.getUserMetaDataOf(MD5_ORIGINAL_HEX_FIELD);
            }

            return null;

        } catch (AmazonS3Exception s3Exception) {
            if (s3Exception.getStatusCode() == 404) {
                Logging.printLogWarn(logger, session, "S3 status code: 404 [md5 not found]");
                return null;
            } else if (s3Exception.getStatusCode() == 501) {
                Logging.printLogWarn(logger, session, "S3 status code: 501 [forbidden - md5 not found]");
                return null;
            } else {
                return null;
            }
        } catch (Exception e) {
            Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
            return null;
        }
    }

    /**
     * Fetch the etag metadata from a s3object.
     *
     * @param session
     * @param key     the path to the file in s3 bucket
     * @return <br>
     * String containing the etag <br>
     * null if the key lead to no s3 object
     */
    public static String fetchEtagFromAmazon(Session session, String key) {
        try {
            ObjectMetadata objectMetadata = s3clientImages.getObjectMetadata(new GetObjectMetadataRequest(READ_IMAGES_BUCKET_NAME, key));

            String md5 = objectMetadata.getETag();

            Logging.printLogDebug(logger, session, "Fetched md5: " + md5);

            return md5;

        } catch (AmazonS3Exception s3Exception) {
            if (s3Exception.getStatusCode() == 404) {
                Logging.printLogWarn(logger, session, "S3 status code: 404 [md5 not found]");
                return null;
            } else {
                Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(s3Exception));
                return null;
            }
        } catch (Exception e) {
            Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
            return null;
        }
    }

}
