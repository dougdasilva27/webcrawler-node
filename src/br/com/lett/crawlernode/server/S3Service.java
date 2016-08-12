package br.com.lett.crawlernode.server;

import java.io.File;

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
import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * @author Samir Leao
 *
 */
public class S3Service {
	
	protected static final Logger logger = LoggerFactory.getLogger(S3Service.class);
	
	private static final String AWS_ACCESS_KEY = "AKIAITCJTG6OHBYGUWLA";
	private static final String SECRET_KEY = "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";
	private static final String SESSION_BUCKET  = "crawler-sessions";
	
	private static final String SCREENSHOT_FOLDER = "screenshot";
	private static final String HTML_FOLDER = "html";
	
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
	public static void uploadFileToAmazon(CrawlerSession session, File file, String type) {
		
		String amazonLocation = null;
		if (type.equals(SCREENSHOT_UPLOAD_TYPE)) {
			amazonLocation = session.getSessionId() + "/" + SCREENSHOT_FOLDER + "/" + session.getSessionId() + ".png";
		}
		else if (type.equals(HTML_UPLOAD_TYPE)) {
			amazonLocation = session.getSessionId() + "/" + HTML_FOLDER + "/" + session.getSessionId() + ".html";
		}
		else {
			Logging.printLogError(logger, "Upload type not recognized...aborting file upload...");
			return;
		}

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
			Logging.printLogError(logger, session, "Error Message: " + ace.getMessage());
		}
	}

}
