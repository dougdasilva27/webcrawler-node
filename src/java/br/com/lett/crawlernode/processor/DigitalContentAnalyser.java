package br.com.lett.crawlernode.processor;

import java.awt.image.BufferedImage;

import java.io.File;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

import models.Processed;


public class DigitalContentAnalyser {

	public static final String RULE_SATISFIED = "satisfied";

	public static final String RULE_TYPE_KEYWORDS_MIN = "keywords_min";
	public static final String RULE_TYPE_KEYWORDS_EXACT = "keywords_exact";
	public static final String RULE_TYPE_KEYWORDS_ALL = "keywords_all";
	public static final String RULE_TYPE_KEYWORDS_NONE = "keywords_none";

	/**
	 * 
	 * @param pm
	 * @return
	 */
	public static int imageCount(Processed pm) {
		Integer picCount = 0;

		if(pm.getPic() != null && !pm.getPic().isEmpty()) {
			picCount++;
		}

		try {
			JSONArray secondaryPics = new JSONArray(pm.getSecondaryImages());

			return picCount = picCount + secondaryPics.length();
		} catch (Exception e) { 
			return picCount;	
		}

	}

	/**
	 * 
	 * @param image
	 * @return
	 */
	public static JSONObject imageDimensions(File image) {
		try {

			BufferedImage bimg = ImageIO.read(image);

			JSONObject pic_dimensions = new JSONObject();
			pic_dimensions.put("width", bimg.getWidth());
			pic_dimensions.put("height", bimg.getHeight());

			return pic_dimensions;

		} catch (Exception e) {
			return null;
		}

	}

}
