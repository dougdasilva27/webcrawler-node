package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawlerSession extends CrawlerSession {

	/** Primary image URL */
	private String primaryImage;

	/** Array of secondary images */
	private List<String> secondaryImages;

	public ImageCrawlerSession(Message message, String queueName, Markets markets) {
		super(message, queueName, markets);

		// the primary image url is the message body, that is already set up on the superclass CrawlerSession
		this.primaryImage = super.url;

		// get the list of secondary images
		secondaryImages = parseSecondaryImages(message);		
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

}
