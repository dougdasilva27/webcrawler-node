package br.com.lett.crawlernode.core.session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawlerSession extends Session {
	
	private static final String DEFAULT_EXTENSION = "jpg";

	private String internalId;
	private Long processedId;
	private int imageNumber;			// 1 is the main image, any value greater than one is a secondary image
	private String imageType;			// primary | secondary
	private String localFileDir; 		// downloaded image temporary file
	private String imageKeyOnBucket;	// image s3object path on S3 bucket

	public ImageCrawlerSession(Request request, String queueName, Markets markets) {		
		super(request, queueName, markets);
				
		ImageCrawlerRequest imageCrawlerRequest = (ImageCrawlerRequest)request;
		
		// get the type
		if (imageCrawlerRequest.getImageType() != null) {
			imageType = imageCrawlerRequest.getImageType();
		} else {
			Logging.printLogError(logger, "Error: 'type' field not found on message attributes.");
		}
		
		// get the internal id
		if (imageCrawlerRequest.getInternalId() != null) {
			internalId = request.getInternalId();
		} else {
			Logging.printLogError(logger, "Error: " + QueueService.INTERNAL_ID_MESSAGE_ATTR + " field not found on message attributes.");
		}
		
		// get processed id
		if (imageCrawlerRequest.getProcessedId() != null) {
			processedId = request.getProcessedId();
		} else {
			Logging.printLogError(logger, "Error: " + QueueService.PROCESSED_ID_MESSAGE_ATTR + " field not found on message attributes.");
		}
				
		// get the number
		if (imageCrawlerRequest.getImageNumber() != null) {
			imageNumber = imageCrawlerRequest.getImageNumber();
		} else {
			Logging.printLogError(logger, "Error: " + QueueService.NUMBER_MESSAGE_ATTR + " field not found on message attributes.");
		}
		
		localFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + "_" + createImageBaseName();
		imageKeyOnBucket = "market" + "/" + "product-image" + "/" + processedId + "/" + imageNumber + ".jpg";
	}
	
	@Override
	public void clearSession() {
		try {
			Files.deleteIfExists(Paths.get(localFileDir));
		} catch (IOException e) {
			Logging.printLogError(logger, this, CommonMethods.getStackTraceString(e));
		}
	}
	
	/**
	 * Create a base name for the image.
	 * 
	 * @return a String representing the name of the image.
	 */
	private String createImageBaseName() {
		String s = super.originalURL + new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");
		String extension = FilenameUtils.getExtension(super.originalURL);
		if (extension == null || extension.isEmpty()) {
			extension = DEFAULT_EXTENSION;
		}
		return DigestUtils.md5Hex(s) + "." + extension;
	}

	public String getLocalFileDir() {
		return localFileDir;
	}

	public void setLocalFileDir(String localFileDir) {
		this.localFileDir = localFileDir;
	}

	public int getImageNumber() {
		return imageNumber;
	}

	public void setImageNumber(int number) {
		this.imageNumber = number;
	}

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getImageKeyOnBucket() {
		return imageKeyOnBucket;
	}

	public void setImageKeyOnBucket(String imageKeyOnBucket) {
		this.imageKeyOnBucket = imageKeyOnBucket;
	}

	public Long getProcessedId() {
		return processedId;
	}

	public void setProcessedId(Long processedId) {
		this.processedId = processedId;
	}

	public String getType() {
		return imageType;
	}

	public void setType(String type) {
		this.imageType = type;
	}

}
