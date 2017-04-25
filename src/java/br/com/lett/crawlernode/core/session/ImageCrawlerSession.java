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
	
	public static final String PRIMARY_IMG_TYPE = "primary";
	public static final String SECONDARY_IMG_TYPE = "secondary";
	public static final String DEFAULT_EXTENSION = "jpg";

	/** Internal id of the sku being processed */
	private String internalId;
	
	private Long processedId;
	
	/** 
	 * The number of the image, that is used to assemble the directory.
	 * Primary image always have a number=1
	 * First secondary image has number=2, and so on
	 */
	private int imageNumber;
	
	/** The image type: primary | secondary */
	private String imageType;

	private String localFileDir;  
	private String localOriginalFileDir;  

	private String imageKeyOnBucket;

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
		
		// set local directories 
		localFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + "_" + createImageBaseName();
		localOriginalFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + "-original.jpg";  
		
		imageKeyOnBucket = "product-image/" + processedId + "/" + imageNumber + ".jpg";
	}
	
	@Override
	public void clearSession() {
		try {
			Files.deleteIfExists(Paths.get(localFileDir));
			Files.deleteIfExists(Paths.get(localOriginalFileDir));
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

	public String getLocalOriginalFileDir() {
		return localOriginalFileDir;
	}

	public void setLocalOriginalFileDir(String localOriginalFileDir) {
		this.localOriginalFileDir = localOriginalFileDir;
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
