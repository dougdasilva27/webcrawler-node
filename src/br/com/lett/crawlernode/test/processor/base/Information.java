package br.com.lett.crawlernode.test.processor.base;

import java.awt.Dimension;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Authenticator;
import java.net.URL;

import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import br.com.lett.crawlernode.kernel.fetcher.Proxy;
import br.com.lett.crawlernode.kernel.fetcher.ProxyAuthenticator;
import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;


/**
 * Classe contém atributos com consultas e apontamentos para arquivos importantes
 * @author doug
 *
 */
public class Information {
	
	private static final Logger logger = LoggerFactory.getLogger(Information.class);
	
	// Arquivo CVS com mapa de substituição
	public static String unitsReplaceMapCSV = "https://docs.google.com/spreadsheets/d/14rCz0hVvHf69-qxb8IQwFJzg6n3gX1O2jXg3uqYv0rQ/export?gid=765964741&format=csv";
	public static String recipientsReplaceMapCSV = "https://docs.google.com/spreadsheets/d/14rCz0hVvHf69-qxb8IQwFJzg6n3gX1O2jXg3uqYv0rQ/export?gid=1453155877&format=csv";
	
	// Arquivo CVS com mapa de identificação
	public static String blacklistRemoveListCSV = "https://docs.google.com/spreadsheets/d/14rCz0hVvHf69-qxb8IQwFJzg6n3gX1O2jXg3uqYv0rQ/export?gid=2006769503&format=csv";
	public static String recipientsListCSV = "https://docs.google.com/spreadsheets/d/14rCz0hVvHf69-qxb8IQwFJzg6n3gX1O2jXg3uqYv0rQ/export?gid=1329938183&format=csv";
	public static String unitsListCSV = "https://docs.google.com/spreadsheets/d/14rCz0hVvHf69-qxb8IQwFJzg6n3gX1O2jXg3uqYv0rQ/export?gid=1653255262&format=csv";
		
	// Querys para consultas
	public static final String queryForProcessedProducts = "SELECT * FROM processed WHERE lmt > ";

//	public static final String queryForDistinctProducts = "SELECT DISTINCT ON (market, internal_id) * FROM crawler ORDER BY market, internal_id, date DESC";

//	public static final String queryForDistinctProductsByCity1 = "SELECT DISTINCT ON (internal_id) * FROM crawler WHERE market =";
//	public static final String queryForDistinctProductsByCity2 = " ORDER BY internal_id, date DESC limit 10";
	
//	public static final String queryForDistinctProductsByCity1Test = "SELECT DISTINCT ON (internal_id) * FROM crawler WHERE market =";
//	public static final String queryForDistinctProductsByCity2Test = " ORDER BY internal_id, date DESC LIMIT 1000";
	
	public static final String queryForLettClassProducts = "SELECT denomination, mistake, extra FROM lett_class LEFT JOIN lett_class_mistake ON (lett_class.id = id_lett_class);";
	
	public static final String queryForLettBrandProducts = "SELECT denomination, supplier as lett_supplier, mistake, ignored FROM lett_brand LEFT JOIN lett_brand_mistake ON (lett_brand.id = id_brand);";
		
	public static final String queryMarkets = "SELECT * FROM market";	
	
	public static final String queryForSelectProcessedProduct_part1 = "SELECT * FROM processed WHERE internal_id LIKE '";
	public static final String queryForSelectProcessedProduct_part2 = "' AND market =";
	

	private static String cdnBucketName     = "cdn.insights.lett.com.br";
	private static String accessKey        	= "AKIAJ73Z3NTUDN2IF7AA";
	private static String secretKey        	= "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";
	
	
	public static Dimension fetchImageDimensionFromWeb(CrawlerSession session, URL url, Proxy proxy) throws IOException {
 		
 		System.setProperty("http.proxyHost", proxy.getHost());
        System.setProperty("http.proxyPort", Integer.toString(proxy.getPort()));
        System.setProperty("https.proxyHost", proxy.getHost());
        System.setProperty("https.proxyPort", Integer.toString(proxy.getPort()));

        Authenticator.setDefault(new ProxyAuthenticator(proxy.getUser(), proxy.getPass()));
        
        Logging.printLogDebug(logger, session, "Fetching image with proxy: " + proxy.getHost());
        
		try(ImageInputStream in = ImageIO.createImageInputStream(url.openStream())){
		    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
		    if (readers.hasNext()) {
		        ImageReader reader = readers.next();
		        try {
		            reader.setInput(in);
		            return new Dimension(reader.getWidth(0), reader.getHeight(0));
		        } finally {
		            reader.dispose();
		        }
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null; 	
	}
	
	public static File fetchImageFromAmazon(CrawlerSession session, String key) {
		return fetchImageFromAmazon(session, key, 1); 
	}
	
	private static File fetchImageFromAmazon(CrawlerSession session, String key, int attempt) {
		
		if(attempt > 3) return null; 
		
		Logging.printLogDebug(logger, session, "[ATTEMPT " + attempt + "] Fetching image from Amazon: " + key);
		
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
        		return fetchImageFromAmazon(session, key, (attempt+1));
        	}
        }
        catch (Exception e) {
        	Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
    		return fetchImageFromAmazon(session, key, (attempt+1));
		}
		
	}
	
	public static String fetchMd5FromAmazon(CrawlerSession session, String key) {
		return fetchMd5FromAmazon(session, key, 1);
	}
	
	private static String fetchMd5FromAmazon(CrawlerSession session, String key, int attempt) {
		
		if(attempt > 3) return null; 
		
		Logging.printLogDebug(logger, session, "[ATTEMPT " + attempt + "] Fetching image md5 from Amazon: " + key);
		
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
        		return fetchMd5FromAmazon(session, key, (attempt+1));
        	}
        } 
        catch (Exception e) {
        	Logging.printLogWarn(logger, CommonMethods.getStackTrace(e));
        	return fetchMd5FromAmazon(session, key, (attempt+1));
		}
		
	}
	
}
