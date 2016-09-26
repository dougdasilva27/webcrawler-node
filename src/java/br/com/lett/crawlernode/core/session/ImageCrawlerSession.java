package br.com.lett.crawlernode.core.session;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Markets;

public class ImageCrawlerSession extends CrawlerSession {
	
	/** Primary image URL */
	private String primaryImage;
	
	/** Array of secondary images */
	private List<String> secondaryImages;
	
	public ImageCrawlerSession(Message message, String queueName, Markets markets) {
		super(message, queueName, markets);
		
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();
		
		// the primary image url is the message body, that is already set up on the superclass CrawlerSession
		this.primaryImage = super.url;
		
		// get the list of secondary images
		String secondaryImagesString = attrMap.get("secondary").getStringValue();
		
		
		
	}
	
	public String getPrimaryImage() {
		return super.url;
	}
	
	private void parseSecondaryImagesURL(String secondaryImagesString) {
		String[] urls = secondaryImagesString.replace("[", "").replaceAll("]", "").split(",");
	}
	
	
	
	
	
}
