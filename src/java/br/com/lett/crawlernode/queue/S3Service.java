package br.com.lett.crawlernode.queue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.Logging;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * 
 * @author Samir Leao
 *
 */
public class S3Service {

	protected static final Logger logger = LoggerFactory.getLogger(S3Service.class);

	public static final String SCREENSHOT_UPLOAD_TYPE = "screenshot";
	public static final String HTML_UPLOAD_TYPE = "html";
	public static final String MD5_HEX_METADATA_FIELD = "md5Hex";

	// Amazon images
	private static AWSCredentials credentialsImages;
	private static AmazonS3 s3clientImages;
	private static final String IMAGES_BUCKET_NAME   = "lett-media";
	private static final String ACCES_KEY_IMAGES     = "AKIAJEDMKADEFVS4ISQQ";
	private static final String SECRET_KEY_IMAGES    = "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";

	// Amazon crawler-session
	private static AWSCredentials credentialsCrawlerSessions;
	private static AmazonS3 s3clientCrawlerSessions;
	private static String crawlerLogsBucketName  		= "lett-logs";
	private static String crawlerSessionsPrefix  		= "crawler-sessions";
	private static String accessKeyCrawlerSessions      = "AKIAIB4EBBCHAGRUFJLA";
	private static String secretKeyCrawlerSessions      = "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";

	static {
		credentialsImages = new BasicAWSCredentials(ACCES_KEY_IMAGES, SECRET_KEY_IMAGES);
		s3clientImages = new AmazonS3Client(credentialsImages);

		credentialsCrawlerSessions = new BasicAWSCredentials(accessKeyCrawlerSessions, secretKeyCrawlerSessions);
		s3clientCrawlerSessions = new AmazonS3Client(credentialsCrawlerSessions);
	}

	/**
	 * 
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
			}
			else {
				Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(s3Exception));
				return null;
			}
		} 
		catch (Exception e) {
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
		} 
		catch (Exception e) {
			Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
			return null;
		}
	}

	public static void uploadImage(Session session, ObjectMetadata newObjectMetadata) throws FileNotFoundException {

		ImageCrawlerSession s = (ImageCrawlerSession)session;

		File f = new File(s.getLocalOriginalFileDir());

		FileInputStream fileInputStream = new FileInputStream(f);

		PutObjectRequest putObjectRequest = new PutObjectRequest(
				IMAGES_BUCKET_NAME, 
				s.getImageKeyOnBucket(), 
				fileInputStream, 
				newObjectMetadata);

		try {
			s3clientImages.putObject(putObjectRequest);
			Logging.printLogDebug(logger, session, "Uploaded image #" + s.getImageNumber() + " with success!");
			
		} catch (AmazonClientException ace) {
			Logging.printLogError(logger, session, "Error Message:    " + ace.getMessage());
		}
	}

	public static String getAmazonImageFileMd5(S3Object s3Object) {
		return s3Object.getObjectMetadata().getUserMetaDataOf(MD5_HEX_METADATA_FIELD);
	}

	/**
	 * Uploads a file to the Amazon session bucket.
	 * @param session
	 * @param file
	 */
	public static void uploadCrawlerSessionScreenshotToAmazon(Session session, File file) {

		String amazonLocation = new StringBuilder()
				.append(crawlerSessionsPrefix)
				.append("/")
				.append(session.getSessionId())
				.append("/")
				.append("screenshot-")
				.append(new DateTime(DateConstants.timeZone).millisOfDay())
				.append(".png")
				.toString();

		try {
			Logging.printLogDebug(logger, session, "Uploading file to Amazon");
			s3clientCrawlerSessions.putObject(new PutObjectRequest(crawlerLogsBucketName, amazonLocation, file));
			Logging.printLogDebug(logger, session, "Screenshot uploaded with success!");

		} catch (AmazonServiceException ase) {
			Logging.printLogError(logger, session, " - Caught an AmazonServiceException, which " +
					"means your request made it " +
					"to Amazon S3, but was rejected with an error response" +
					" for some reason.");
			Logging.printLogError(logger, session, "Error Message:    " + ase.getMessage());
			Logging.printLogError(logger, session, "HTTP Status Code: " + ase.getStatusCode());
			Logging.printLogError(logger, session, "AWS Error Code:   " + ase.getErrorCode());
			Logging.printLogError(logger, session, "Error Type:       " + ase.getErrorType());
			Logging.printLogError(logger, session, "Request ID:       " + ase.getRequestId());

		} catch (AmazonClientException ace) {
			Logging.printLogError(logger, session, " - Caught an AmazonClientException, which " +
					"means the client encountered " +
					"an internal error while trying to " +
					"communicate with S3, " +
					"such as not being able to access the network.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ace));
		}
	}

	/**
	 * Uploads a String as file to Amazon.
	 * @param session
	 * @param file
	 */
	public static void uploadCrawlerSessionContentToAmazon(Session session, String requestHash, String html) {		

		String amazonLocation = new StringBuilder()
				.append(crawlerSessionsPrefix)
				.append("/")
				.append(session.getSessionId())
				.append("/")
				.append(requestHash)
				.append(".html")
				.toString();

		File htmlFile = null;

		try {
			Logging.printLogDebug(logger, session, "Uploading content to Amazon");
			htmlFile = new File(requestHash + ".html");			
			FileUtils.writeStringToFile(htmlFile, html);
			s3clientCrawlerSessions.putObject(new PutObjectRequest(crawlerLogsBucketName, amazonLocation, htmlFile));
			Logging.printLogDebug(logger, session, "Content uploaded with success!");

		} catch (AmazonServiceException ase) {
			Logging.printLogError(logger, session, " - Caught an AmazonServiceException, which " +
					"means your request made it " +
					"to Amazon S3, but was rejected with an error response" +
					" for some reason.");
			Logging.printLogError(logger, session, "Error Message:    " + ase.getMessage());
			Logging.printLogError(logger, session, "HTTP Status Code: " + ase.getStatusCode());
			Logging.printLogError(logger, session, "AWS Error Code:   " + ase.getErrorCode());
			Logging.printLogError(logger, session, "Error Type:       " + ase.getErrorType());
			Logging.printLogError(logger, session, "Request ID:       " + ase.getRequestId());

		} catch (AmazonClientException ace) {
			Logging.printLogError(logger, session, " - Caught an AmazonClientException, which " +
					"means the client encountered " +
					"an internal error while trying to " +
					"communicate with S3, " +
					"such as not being able to access the network.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ace));

		} catch (IOException ex) {
			Logging.printLogError(logger, session, "Error writing String to file during html upload.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
		} finally {
			if (htmlFile != null) {
				htmlFile.delete();
			}
		}
	}

	public static File fetchImageFromAmazon(Session session, String key) {
		return fetchImageFromAmazon(session, key, 1); 
	}

	private static File fetchImageFromAmazon(Session session, String key, int attempt) {
		Logging.printLogDebug(logger, session, "Fetching image from Amazon: " + key);

		try {
			S3Object object = s3clientImages.getObject(new GetObjectRequest(IMAGES_BUCKET_NAME, key));

			InputStream reader = new BufferedInputStream(object.getObjectContent());

			File file = File.createTempFile("fetchImageFromAmazon", ".jpg");     
			OutputStream writer;

			writer = new BufferedOutputStream(new FileOutputStream(file));

			int read = -1;

			while ( ( read = reader.read() ) != -1 ) {
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
		}
		catch (Exception e) {
			Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
			return null;
		}

	}

	public static String fetchEtagFromAmazon(Session session, String key) {
		return fetchEtagFromAmazon(session, key, 1);
	}

	private static String fetchEtagFromAmazon(Session session, String key, int attempt) {

		Logging.printLogDebug(logger, session, "Fetching image md5 from Amazon: " + key);

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
		} 
		catch (Exception e) {
			Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
			return null;
		}

	}

}
