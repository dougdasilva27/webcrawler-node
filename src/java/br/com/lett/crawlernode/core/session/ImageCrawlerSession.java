package br.com.lett.crawlernode.core.session;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawlerSession extends CrawlerSession {
	
	public static final String PRIMARY_IMG_TYPE = "primary";
	public static final String SECONDARY_IMG_TYPE = "secondary";

	/** Internal id of the sku being processed */
	private String internalId;
	
	private Long processedId;
	
	/** 
	 * The number of the image, that is used to assemble the directory.
	 * Primary image always have a number=1
	 * First secondary image has number=2, and so on
	 */
	private int number;
	
	/** The image type: primary | secondary */
	private String type;

	private String localFileDir;  
	private String localOriginalFileDir;  
	private String localRegularFileDir;  
	private String localSmallFileDir; 

	private String originalName;
	private String smallName;
	private String regularName;

	public ImageCrawlerSession(Message message, String queueName, Markets markets) {
		super(message, queueName, markets);

		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();
		
		// get the type
		if (attrMap.containsKey("type")) {
			this.type = attrMap.get("type").getStringValue();
		} else {
			Logging.printLogError(logger, "Error: 'type' field not found on message attributes.");
		}
		
		// get the internal id
		if (attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
			this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
		} else {
			Logging.printLogError(logger, "Error: " + QueueService.INTERNAL_ID_MESSAGE_ATTR + " field not found on message attributes.");
		}
		
		// get processed id
		if (attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
			this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		} else {
			Logging.printLogError(logger, "Error: " + QueueService.PROCESSED_ID_MESSAGE_ATTR + " field not found on message attributes.");
		}
				
		// get the number
		if (attrMap.containsKey(QueueService.NUMBER_MESSAGE_ATTR)) {
			this.number = Integer.parseInt(attrMap.get(QueueService.NUMBER_MESSAGE_ATTR).getStringValue());
		} else {
			Logging.printLogError(logger, "Error: " + QueueService.NUMBER_MESSAGE_ATTR + " field not found on message attributes.");
		}
		
		// set local directories 
		this.localFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "_" + createImageBaseName();  
		this.localOriginalFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-original.jpg";  
		this.localRegularFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-regular.jpg";  
		this.localSmallFileDir = Main.executionParameters.getTmpImageFolder() + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-small.jpg";

		// set names
		this.originalName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-original.jpg";
		this.smallName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-small.jpg";
		this.regularName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-regular.jpg";

	}
	
	@Override
	public void clearSession() {
		try {
			Files.deleteIfExists(Paths.get(this.localFileDir));
			Files.deleteIfExists(Paths.get(this.localOriginalFileDir));
			Files.deleteIfExists(Paths.get(this.localSmallFileDir));
			Files.deleteIfExists(Paths.get(this.localRegularFileDir));
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
		String s = super.url + new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");
		return DigestUtils.md5Hex(s) + "." + FilenameUtils.getExtension(super.url);
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

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
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
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
