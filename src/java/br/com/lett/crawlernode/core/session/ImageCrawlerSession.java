package br.com.lett.crawlernode.core.session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
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
	private String localRegularFileDir;  
	private String localSmallFileDir; 

	private String originalName;
	private String smallName;
	private String regularName;
	
	private String md5AmazonPath;
	private String localMd5AmazonPath;

	public ImageCrawlerSession(Request request, String queueName, Markets markets) {		
		super(request, queueName, markets);
				
		ImageCrawlerRequest imageCrawlerRequest = (ImageCrawlerRequest)request;
		
		// get the type
		if (imageCrawlerRequest.getImageType() != null) {
			this.imageType = imageCrawlerRequest.getImageType();
		} else {
			Logging.printLogError(logger, "Error: 'type' field not found on message attributes.");
		}
		
		// get the internal id
		if (imageCrawlerRequest.getInternalId() != null) {
			this.internalId = request.getInternalId();
		} else {
			Logging.printLogError(logger, "Error: " + QueueService.INTERNAL_ID_MESSAGE_ATTR + " field not found on message attributes.");
		}
		
		// get processed id
		if (imageCrawlerRequest.getProcessedId() != null) {
			this.processedId = request.getProcessedId();
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
		this.localFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + "_" + createImageBaseName();  
				
		this.localOriginalFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + "-original.jpg";  
		this.localRegularFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + "-regular.jpg";  
		this.localSmallFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + "-small.jpg";

		// set names
		this.originalName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + imageNumber + "-original.jpg";
		this.smallName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + imageNumber + "-small.jpg";
		this.regularName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + imageNumber + "-regular.jpg";
		
		// set Amazon path name
		this.md5AmazonPath = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + ".rev-" + imageNumber + "-md5.txt";
		this.localMd5AmazonPath = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + imageNumber + ".rev-" + imageNumber + "-md5.txt";
	}
	
	@Override
	public void clearSession() {
		try {
			Files.deleteIfExists(Paths.get(localFileDir));
			Files.deleteIfExists(Paths.get(localOriginalFileDir));
			Files.deleteIfExists(Paths.get(localSmallFileDir));
			Files.deleteIfExists(Paths.get(localRegularFileDir));
			Files.deleteIfExists(Paths.get(localMd5AmazonPath));
		} catch (IOException e) {
			Logging.printLogError(logger, this, CommonMethods.getStackTraceString(e));
		}
	}
	
	public String getLocalMd5Path() {
		return this.localMd5AmazonPath;
	}
	
	/**
	 * Create a base name for the image.
	 * 
	 * @return a String representing the name of the image.
	 */
	private String createImageBaseName() {
		String s = super.originalURL + new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");
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

	public String getLocalSmallFileDir() {
		return localSmallFileDir;
	}

	public void setLocalSmallFileDir(String localSmallFileDir) {
		this.localSmallFileDir = localSmallFileDir;
	}

	public String getLocalRegularFileDir() {
		return localRegularFileDir;
	}

	public void setLocalRegularFileDir(String localRegularFileDir) {
		this.localRegularFileDir = localRegularFileDir;
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

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getSmallName() {
		return smallName;
	}

	public void setSmallName(String smallName) {
		this.smallName = smallName;
	}

	public String getRegularName() {
		return regularName;
	}

	public void setRegularName(String regularName) {
		this.regularName = regularName;
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

	public String getMd5AmazonPath() {
		return md5AmazonPath;
	}

	public void setMd5AmazonPath(String md5AmazonPath) {
		this.md5AmazonPath = md5AmazonPath;
	}

}
