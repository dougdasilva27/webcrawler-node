package br.com.lett.crawlernode.aws.s3;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.amazonaws.ClientConfiguration;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
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

   private static final String AMAZON_HTML_URL = "https://s3.console.aws.amazon.com/s3/object/";
   public static final String SCREENSHOT_UPLOAD_TYPE = "screenshot";
   public static final String HTML_UPLOAD_TYPE = "html";
   public static final String MD5_HEX_METADATA_FIELD = "md5hex";
   public static final String MD5_ORIGINAL_HEX_FIELD = "originalmd5hex";
   public static final String POSITION = "position";
   public static final String LOCAL_PATH = "src/resources/";
   protected static final Logger logger = LoggerFactory.getLogger(S3Service.class);
   // Amazon images
   private static final AmazonS3 s3clientImages;
   private static final String S3_CRAWLER_SESSIONS_PREFIX = "crawler-sessions";

   // Amazon crawler-session
   private static final AmazonS3 s3clientCrawlerSessions;
   private static final String LOGS_BUCKET_NAME = GlobalConfigurations.executionParameters.getLogsBucketName();
   private static final int AWS_CONNECTION_TIMEOUT = Math.toIntExact(TimeUnit.MINUTES.toMillis(20));

   static {
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      clientConfiguration.setConnectionTimeout(AWS_CONNECTION_TIMEOUT);

      s3clientImages = AmazonS3ClientBuilder
         .standard()
         .withRegion(Regions.US_EAST_1)
         .withClientConfiguration(clientConfiguration)
         .withCredentials(new DefaultAWSCredentialsProviderChain())
         .build();

      s3clientCrawlerSessions = AmazonS3ClientBuilder
         .standard()
         .withRegion(Regions.US_EAST_1)
         .withClientConfiguration(clientConfiguration)
         .withCredentials(new DefaultAWSCredentialsProviderChain())
         .build();
   }

   /**
    * @param session
    * @param name
    * @return
    */
   public static ObjectMetadata fetchObjectMetadata(Session session, String name, String bucket) {
      try {
         return s3clientImages.getObjectMetadata(bucket, name);
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


   /**
    * Uploads the current image being processed in the session object to S3.
    *
    * @param session
    * @param newObjectMetadata the user metadata that goes into the s3object
    * @param f the local file
    * @param key the path for the image in the S3 bucket
    * @throws FileNotFoundException if the local temporary file of the downloaded image was not found
    */
   public static void uploadImage(Session session, ObjectMetadata newObjectMetadata, File f, String key, String bucket) throws FileNotFoundException {

      FileInputStream fileInputStream = new FileInputStream(f);
      PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, fileInputStream, newObjectMetadata);

      try {
         s3clientImages.putObject(putObjectRequest);
         Logging.printLogInfo(logger, session, "[BUCKET - " + bucket + "] Uploaded image " + key + " with success!");

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

         long sendS3FilesStartTime = System.currentTimeMillis();

         if (compressedFile.exists()) {
            try {
               Logging.printLogDebug(logger, session, "Uploading HTML to S3");

               String amazonFinalUrl = new StringBuilder()
                  .append(AMAZON_HTML_URL)
                  .append(LOGS_BUCKET_NAME)
                  .append("?region=")
                  .append(Regions.US_EAST_1.toString().toLowerCase().replace('_','-'))
                  .append("&prefix=")
                  .append(amazonLocation)
                  .toString();

               s3clientCrawlerSessions.putObject(new PutObjectRequest(LOGS_BUCKET_NAME, amazonLocation, compressedFile));

               JSONObject kinesisProductFlowMetadata = new JSONObject().put("aws_elapsed_time", System.currentTimeMillis() - sendS3FilesStartTime)
                     .put("aws_type", "s3")
                     .put("s3_bucket", LOGS_BUCKET_NAME);

               Logging.logInfo(logger, session, kinesisProductFlowMetadata, "AWS TIMING INFO");

               Logging.printLogDebug(logger, session, "HTML uploaded successfully!");

               Logging.printLogInfo(logger, session, "S3 HTML:\n" + amazonFinalUrl);
            } catch (AmazonServiceException ase) {
               Logging.printLogError(logger, session, " - Caught an AmazonServiceException, which " + "means your request made it "
                     + "to Amazon S3, but was rejected with an error response" + " for some reason.");
               Logging.printLogError(logger, session, "Error Message:    " + ase.getMessage());
               Logging.printLogError(logger, session, "HTTP Status Code: " + ase.getStatusCode());
               Logging.printLogError(logger, session, "AWS Error Code:   " + ase.getErrorCode());
               Logging.printLogError(logger, session, "Error Type:       " + ase.getErrorType());
               Logging.printLogError(logger, session, "Request ID:       " + ase.getRequestId());

            } catch (AmazonClientException ace) {
               Logging.printLogError(logger, session, " - Caught an AmazonClientException, which " + "means the client encountered "
                     + "an internal error while trying to " + "communicate with S3, " + "such as not being able to access the network.");
               Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ace));
            } catch (Exception ex) {
               Logging.printLogError(logger, session, "Error during html upload.");
               Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
            } finally {
               if (compressedFile != null) {
                  compressedFile.delete();
               }
            }

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

}
