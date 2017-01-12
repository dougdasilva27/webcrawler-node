package br.com.lett.crawlernode.queue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
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


	// Amazon images
	private static AWSCredentials credentialsImages;
	private static AmazonS3 s3clientImages;
	private static String imagesBucketName     	= "cdn.insights.lett.com.br";
	private static String accessKeyImages       = "AKIAJ73Z3NTUDN2IF7AA";
	private static String secretKeyImages       = "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";

	static {
		credentialsImages = new BasicAWSCredentials(accessKeyImages, secretKeyImages);
		s3clientImages = new AmazonS3Client(credentialsImages);
	}

	// Amazon crawler-session
	private static AWSCredentials credentialsCrawlerSessions;
	private static AmazonS3 s3clientCrawlerSessions;
	private static String crawlerLogsBucketName  		= "lett-logs";
	private static String crawlerSessionsPrefix  		= "crawler-sessions";
	private static String accessKeyCrawlerSessions      = "AKIAIB4EBBCHAGRUFJLA";
	private static String secretKeyCrawlerSessions      = "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";

	static {
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
		AWSCredentials credentials = new BasicAWSCredentials(accessKeyImages, secretKeyImages);
		AmazonS3 s3client = new AmazonS3Client(credentials);
		try {
			return s3client.getObjectMetadata(imagesBucketName, name);
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

	public static S3Object fetchS3Object(Session session, String name) {
		AWSCredentials credentials = new BasicAWSCredentials(accessKeyImages, secretKeyImages);
		AmazonS3 s3client = new AmazonS3Client(credentials);
		try {
			return s3client.getObject(imagesBucketName, name);
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

	public static void uploadImageToAmazon(Session session, String md5) {
		int number = ((ImageCrawlerSession)session).getImageNumber();

		String md5Path = ((ImageCrawlerSession)session).getMd5AmazonPath();
		String localMd5Path = ((ImageCrawlerSession)session).getLocalMd5Path();

		File localMd5File = new File(localMd5Path);
		try {
			FileUtils.writeStringToFile(localMd5File, md5);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String originalName = ((ImageCrawlerSession)session).getOriginalName();
		String localOriginalFileDir = ((ImageCrawlerSession)session).getLocalOriginalFileDir();

		String smallName = ((ImageCrawlerSession)session).getSmallName();
		String localSmallFileDir = ((ImageCrawlerSession)session).getLocalSmallFileDir();

		String regularName = ((ImageCrawlerSession)session).getRegularName();
		String localRegularFileDir = ((ImageCrawlerSession)session).getLocalRegularFileDir();

		try {
			s3clientImages.putObject(new PutObjectRequest(imagesBucketName, md5Path, localMd5File));
			//if(number == 1) s3client.copyObject(new CopyObjectRequest(cdnBucketName, ((ImageCrawlerSession)session).getOriginalName(), cdnBucketName, originalName.replace(".jpg", "." + md5 + ".jpg")));
			Logging.printLogDebug(logger, session, "Upload md5 file OK!");

			s3clientImages.putObject(new PutObjectRequest(imagesBucketName, originalName, new File(localOriginalFileDir)));
			if(number == 1) s3clientImages.copyObject(new CopyObjectRequest(imagesBucketName, ((ImageCrawlerSession)session).getOriginalName(), imagesBucketName, originalName.replace(".jpg", "." + md5 + ".jpg")));
			Logging.printLogDebug(logger, session, "Upload original OK!");

			s3clientImages.putObject(new PutObjectRequest(imagesBucketName, smallName, new File(localSmallFileDir)));
			if(number == 1) s3clientImages.copyObject(new CopyObjectRequest(imagesBucketName, smallName, imagesBucketName, smallName.replace(".jpg", "." + md5 + ".jpg")));
			Logging.printLogDebug(logger, session, "Upload small OK!");

			s3clientImages.putObject(new PutObjectRequest(imagesBucketName, regularName, new File(localRegularFileDir)));
			if(number == 1) s3clientImages.copyObject(new CopyObjectRequest(imagesBucketName, regularName, imagesBucketName, regularName.replace(".jpg", "." + md5 + ".jpg")));
			Logging.printLogDebug(logger, session, "Upload regular OK!");

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

	public static String getAmazonImageFileMd5(S3Object s3Object) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object.getObjectContent()));
		String content = "";
		String line = null;
		try {
			while((line = reader.readLine()) != null) {
				content += line;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
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
		.append(new DateTime().millisOfDay())
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

		//if(attempt > 3) return null; 

		Logging.printLogDebug(logger, session, "Fetching image from Amazon: " + key);

		try {
			S3Object object = s3clientImages.getObject(new GetObjectRequest(imagesBucketName, key));

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

	public static String fetchMd5FromAmazon(Session session, String key) {
		return fetchMd5FromAmazon(session, key, 1);
	}

	private static String fetchMd5FromAmazon(Session session, String key, int attempt) {

		//if(attempt > 3) return null; 

		Logging.printLogDebug(logger, session, "Fetching image md5 from Amazon: " + key);

		try {
			ObjectMetadata objectMetadata = s3clientImages.getObjectMetadata(new GetObjectMetadataRequest(imagesBucketName, key));

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
