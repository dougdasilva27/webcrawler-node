package br.com.lett.crawlernode.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import br.com.lett.crawlernode.core.task.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * @author Samir Leao
 *
 */
public class S3Service {
	
	protected static final Logger logger = LoggerFactory.getLogger(S3Service.class);
	
	private static final String SESSION_BUCKET  = "crawler-sessions";
	
	public static final String SCREENSHOT_UPLOAD_TYPE = "screenshot";
	public static final String HTML_UPLOAD_TYPE = "html";
	
	private static AWSCredentials credentials;
	private static AmazonS3 s3client;
	
	
	private static String cdnBucketName     = "cdn.insights.lett.com.br";
	private static String accessKey        	= "AKIAJ73Z3NTUDN2IF7AA";
	private static String secretKey        	= "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";
	
	
	static {
		credentials = new BasicAWSCredentials(accessKey, secretKey);
		s3client = new AmazonS3Client(credentials);
	}
	
	/**
	 * Uploads a file to the Amazon session bucket.
	 * @param session
	 * @param file
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
	 */
	public static void uploadContentToAmazon(CrawlerSession session, String requestHash, String html) {		
		String amazonLocation = session.getSessionId() + "/" + requestHash + ".html";
		File htmlFile = null;
	
		try {
			Logging.printLogDebug(logger, session, "Uploading content to Amazon");
			htmlFile = new File(requestHash + ".html");			
			FileUtils.writeStringToFile(htmlFile, html);
			s3client.putObject(new PutObjectRequest(SESSION_BUCKET, amazonLocation, htmlFile));
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
	
	public static File fetchImageFromAmazon(CrawlerSession session, String key) {
		return fetchImageFromAmazon(session, key, 1); 
	}
	
	private static File fetchImageFromAmazon(CrawlerSession session, String key, int attempt) {
		
		//if(attempt > 3) return null; 
		
		Logging.printLogDebug(logger, session, "Fetching image from Amazon: " + key);
		
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3 s3client = new AmazonS3Client(credentials);

        try {
            S3Object object = s3client.getObject(new GetObjectRequest(cdnBucketName, key));
            
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
	
	public static String fetchMd5FromAmazon(CrawlerSession session, String key) {
		return fetchMd5FromAmazon(session, key, 1);
	}
	
	private static String fetchMd5FromAmazon(CrawlerSession session, String key, int attempt) {
		
		//if(attempt > 3) return null; 
		
		Logging.printLogDebug(logger, session, "Fetching image md5 from Amazon: " + key);
		
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3 s3client = new AmazonS3Client(credentials);

        try {
            ObjectMetadata objectMetadata = s3client.getObjectMetadata(new GetObjectMetadataRequest(cdnBucketName, key));
            
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
