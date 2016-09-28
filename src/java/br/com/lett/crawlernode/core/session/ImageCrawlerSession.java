package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.server.QueueService;

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
		this.type = attrMap.get("type").getStringValue();
		
		// get the internal id
		this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
		
		// get processed id
		this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		
		// get the number
		this.number = Integer.parseInt(attrMap.get("Number").getStringValue());

		// set local directories
//		this.localFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "_" + FilenameUtils.getName(super.url);  
//		localOriginalFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-original.jpg";  
//		localRegularFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-regular.jpg";  
//		localSmallFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-small.jpg";

		// set names
//		originalName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-original.jpg";
//		smallName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-small.jpg";
//		regularName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-regular.jpg";

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
