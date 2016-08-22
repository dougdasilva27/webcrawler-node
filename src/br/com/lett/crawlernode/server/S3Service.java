package br.com.lett.crawlernode.server;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * @author Samir Leao
 *
 */
public class S3Service {
	
	protected static final Logger logger = LoggerFactory.getLogger(S3Service.class);
	
	private static final String AWS_ACCESS_KEY = "AKIAJ73Z3NTUDN2IF7AA";
	private static final String SECRET_KEY = "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";
	
	private static final String SESSION_BUCKET  = "crawler-sessions";
	
	public static final String SCREENSHOT_UPLOAD_TYPE = "screenshot";
	public static final String HTML_UPLOAD_TYPE = "html";
	
	private static AWSCredentials credentials;
	private static AmazonS3 s3client;
	
	static {
		credentials = new BasicAWSCredentials(AWS_ACCESS_KEY, SECRET_KEY);
		s3client = new AmazonS3Client(credentials);
	}
	
	/**
	 * Uploads a file to the Amazon session bucket.
	 * @param session
	 * @param file
	 * @param type
	 */
	public static void uploadFileToAmazon(CrawlerSession session, File file) {
		
		String amazonLocation = session.getSessionId() + "/" + session.getSessionId() + ".png";

		try {
			Logging.printLogDebug(logger, session, "Uploading file to Amazon");
			s3client.putObject(new PutObjectRequest(SESSION_BUCKET, amazonLocation, file));
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
	 * @param type
	 */
	public static void uploadHtmlToAmazon(CrawlerSession session, String html) {		
		String amazonLocation = session.getSessionId() + "/" + session.getSessionId() + ".html";
	
		try {
			Logging.printLogDebug(logger, session, "Uploading file to Amazon");
			File htmlFile = new File(session.getSessionId() + ".html");
			FileUtils.writeStringToFile(htmlFile, html);
			s3client.putObject(new PutObjectRequest(SESSION_BUCKET, amazonLocation, htmlFile));
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
			
		} catch (IOException ex) {
			Logging.printLogError(logger, session, "Error writing String to file during html upload.");
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(ex));
		}
	}

}
