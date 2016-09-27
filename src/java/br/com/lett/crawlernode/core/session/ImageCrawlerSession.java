package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawlerSession extends CrawlerSession {

	/** Internal id of the sku being processed */
	private String internalId;
	
	
	private Long processedId;
	
	/** 
	 * The number of the image, that is used to assemble the directory.
	 * Primary image always have a number=1
	 * First secondary image has number=2, and so on
	 */
	private String number;

	/** Primary image URL */
	private String primaryImage;

	/** Array of secondary images */
	private List<String> secondaryImages;

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
		
		// get the internal id
		this.setInternalId(attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue());
		
		// get processed id
		this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());

		// the primary image url is the message body, that is already set up on the superclass CrawlerSession
		this.primaryImage = super.url;

		// get the list of secondary images
		this.secondaryImages = parseSecondaryImages(message);

		// set local directories
//		localFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "_" + FilenameUtils.getName(url);  
//		localOriginalFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-original.jpg";  
//		localRegularFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-regular.jpg";  
//		localSmallFileDir = Main.tempFolder + "/" + super.market.getCity() + "/" + super.market.getName() + "/images/" + internalId + "_" + number + "-small.jpg";

		// set names
//		originalName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-original.jpg";
//		smallName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-small.jpg";
//		regularName = "product-image/" + super.market.getCity() + "/" + super.market.getName() + "/" + internalId + "/" + number + "-regular.jpg";

	}

	private ArrayList<String> parseSecondaryImages(Message message) {
		try {
			Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

			ArrayList<String> secondaryImages = new ArrayList<String>();
			String imagesString = attrMap.get("secondary").getStringValue();

			JSONArray imagesJsonArray = new JSONArray(imagesString);
			for (int i = 0; i < imagesJsonArray.length(); i++) {
				secondaryImages.add( imagesJsonArray.getString(i) );
			}

			return secondaryImages;
		}
		catch (JSONException e) {
			Logging.printLogDebug(logger, CommonMethods.getStackTraceString(e));
			return new ArrayList<String>();
		}
	}

	public String getPrimaryImage() {
		return this.primaryImage;
	}

	public List<String> getSecondaryImages() {
		return this.secondaryImages;
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

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
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

}
