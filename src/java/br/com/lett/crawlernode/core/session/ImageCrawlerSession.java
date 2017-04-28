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
	private int imageNumber;					// 1 is the main image, any value greater than one is a secondary image
	private String imageType;					// primary | secondary
	private String localOriginalFileDir; 		// downloaded image temporary file
	private String localTransformedFileDir;		// directory of the original image after transformations
	private String originalImageKeyOnBucket;	// original image s3object path on S3 bucket
	private String transformedImageKeyOnBucket;	// transformed image s3object path on S3 bucket

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
		
		// local tmp directories
		localOriginalFileDir = createLocalOriginalImageFileDir();
		localTransformedFileDir = createLocalTransformedImageFileDir();
		
		// amazon bucket keys
		originalImageKeyOnBucket = createOriginalImageKeyOnBucket();
		transformedImageKeyOnBucket = createTransformedImageKeyOnBucket();
	}
	
	@Override
	public void clearSession() {
		try {
			Files.deleteIfExists(Paths.get(localOriginalFileDir));
			Files.deleteIfExists(Paths.get(localTransformedFileDir));
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
	
	private String createOriginalImageKeyOnBucket() {
		return new StringBuilder()
				.append("market")
				.append("/")
				.append("product-image")
				.append("/")
				.append(processedId)
				.append("/")
				.append(imageNumber)
				.append("_original")
				.append(".jpg")
				.toString();
	}
	
	private String createTransformedImageKeyOnBucket() {
		return new StringBuilder()
				.append("market")
				.append("/")
				.append("product-image")
				.append("/")
				.append(processedId)
				.append("/")
				.append(imageNumber)
				.append(".jpg")
				.toString();
	}
	
	private String createLocalOriginalImageFileDir() {
		return new StringBuilder()
				.append(Main.executionParameters.getTmpImageFolder())
				.append("/")
				.append(super.market.getCity())
				.append("/")
				.append(super.market.getName())
				.append("/")
				.append("images")
				.append("/")
				.append(internalId)
				.append("_")
				.append(imageNumber)
				.append("_")
				.append(createImageBaseName())
				.toString();	
	}
	
	private String createLocalTransformedImageFileDir() {
		return new StringBuilder()
				.append(Main.executionParameters.getTmpImageFolder())
				.append("/")
				.append(super.market.getCity())
				.append("/")
				.append(super.market.getName())
				.append("/")
				.append("images")
				.append(internalId)
				.append("_")
				.append("imageNumber_transformed")
				.append("_")
				.append(createImageBaseName())
				.toString();
	}

	public String getLocalOriginalFileDir() {
		return localOriginalFileDir;
	}

	public void setLocalOriginalFileDir(String localFileDir) {
		this.localOriginalFileDir = localFileDir;
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

	public String getOriginalImageKeyOnBucket() {
		return originalImageKeyOnBucket;
	}

	public void setOriginalImageKeyOnBucket(String originalImageKeyOnBucket) {
		this.originalImageKeyOnBucket = originalImageKeyOnBucket;
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

	public String getLocalTransformedFileDir() {
		return localTransformedFileDir;
	}

	public void setLocalTransformedFileDir(String localTransformedFileDir) {
		this.localTransformedFileDir = localTransformedFileDir;
	}

	public String getTransformedImageKeyOnBucket() {
		return transformedImageKeyOnBucket;
	}

	public void setTransformedImageKeyOnBucket(String transformedImageKeyOnBucket) {
		this.transformedImageKeyOnBucket = transformedImageKeyOnBucket;
	}

}
