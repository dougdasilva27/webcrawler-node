package br.com.lett.crawlernode.aws.s3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

import br.com.lett.crawlernode.aws.ec2.TransferOverSFTP;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.FileCompression;
import br.com.lett.crawlernode.util.Logging;

/**
 * Utility class to export some methods more user-friendly to our needs to operate on S3.
 *
 * @author Samir Leao
 */
public class S3Service {

    protected static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    public static final String SCREENSHOT_UPLOAD_TYPE = "screenshot";
    public static final String HTML_UPLOAD_TYPE = "html";
    public static final String MD5_HEX_METADATA_FIELD = "md5hex";
    public static final String MD5_ORIGINAL_HEX_FIELD = "originalmd5hex";

    public static final String LOCAL_PATH = "src/resources/";

    // Amazon images
    private static final AmazonS3 s3clientImages;
    private static final String IMAGES_BUCKET_NAME = GlobalConfigurations.executionParameters.getImagesBucketName();

    // Amazon crawler-session
    private static final AmazonS3 s3clientCrawlerSessions;
    private static final String LOGS_BUCKET_NAME = GlobalConfigurations.executionParameters.getLogsBucketName();
    private static final String S3_BATCH_USER = GlobalConfigurations.executionParameters.getS3BatchUser();
    private static final String S3_BATCH_HOST = GlobalConfigurations.executionParameters.getS3BatchHost();
    private static final String S3_BATCH_KEY = GlobalConfigurations.executionParameters.getS3BatchKey();
    private static final String S3_BATCH_REMOTE_LOCATION = GlobalConfigurations.executionParameters.getS3BatchRemoteLocaion();
    private static final String SSH_KEYS_BUCKET = GlobalConfigurations.executionParameters.getSshKeysBucket();
    private static final String crawlerSessionsPrefix = "crawler-sessions";

    static {
        s3clientImages =
                AmazonS3ClientBuilder
                        .standard()
                        .withRegion(Regions.US_EAST_1)
                        .withCredentials(new DefaultAWSCredentialsProviderChain())
                        .build();

        s3clientCrawlerSessions =
                AmazonS3ClientBuilder
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
            return s3clientImages.getObjectMetadata(IMAGES_BUCKET_NAME, name);
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
            return s3clientImages.getObject(IMAGES_BUCKET_NAME, name);
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
    public static void uploadImage(Session session, ObjectMetadata newObjectMetadata, File f, String key) throws FileNotFoundException {

        ImageCrawlerSession s = (ImageCrawlerSession) session;

        FileInputStream fileInputStream = new FileInputStream(f);

        PutObjectRequest putObjectRequest = new PutObjectRequest(IMAGES_BUCKET_NAME, key, fileInputStream, newObjectMetadata);

        try {
            s3clientImages.putObject(putObjectRequest);
            Logging.printLogDebug(logger, session, "Uploaded image #" + s.getImageNumber() + " with success!");

        } catch (AmazonClientException ace) {
            Logging.printLogError(logger, session, "Error Message:    " + ace.getMessage());
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
                    .append(crawlerSessionsPrefix)
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

            File compressedFile = null;

            try {
                Logging.printLogDebug(logger, session, "Uploading HTML to S3 Batch service");
                compressedFile = new File(tarGzPath);

                TransferOverSFTP sftp = new TransferOverSFTP(S3_BATCH_USER, S3_BATCH_HOST, SSH_KEYS_BUCKET, S3_BATCH_KEY);
                String localFile = compressedFile.getAbsolutePath();
                String remoteFile = Paths.get(S3_BATCH_REMOTE_LOCATION, LOGS_BUCKET_NAME, amazonLocation).toString();
                sftp.connect();
                sftp.sendFile(localFile, remoteFile);
                sftp.disconnect();

                Logging.printLogDebug(logger, session, "HTML uploaded successfully!");
            } catch (Exception ex) {
                Logging.printLogError(logger, session, "Error during HTML upload.");
                Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
            } finally {
                if (compressedFile != null) {
                    compressedFile.delete();
                }
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

    public static File fetchImageFromAmazon(Session session, String key) {
        Logging.printLogDebug(logger, session, "Fetching image from Amazon: " + key);

        try {
            S3Object object = s3clientImages.getObject(new GetObjectRequest(IMAGES_BUCKET_NAME, key));

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
            ObjectMetadata objectMetadata = s3clientImages.getObjectMetadata(new GetObjectMetadataRequest(IMAGES_BUCKET_NAME, key));

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
            ObjectMetadata objectMetadata = s3clientImages.getObjectMetadata(new GetObjectMetadataRequest(IMAGES_BUCKET_NAME, key));

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
