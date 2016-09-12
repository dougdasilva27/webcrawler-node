package br.com.lett.crawlernode.processor.digitalcontent;

import org.json.JSONException;
import org.json.JSONObject;

public class Pic {
	
	public static final String NO_IMAGE = "no-image";
	public static final String MATCH = "match";
	public static final String NOT_VERIFIED = "not-verified";
	public static final String NO_REFERENCE = "no-reference";
	public static final String COMPLETE = "complete";
	public static final String INCOMPLETE = "incomplete";

	public static void setPicSecondary(JSONObject lettDigitalContent, JSONObject pic) {
		JSONObject pic_secondary = new JSONObject();
		
		if(pic.has("secondary") ) {
			pic_secondary = pic.getJSONObject("secondary");
		}

		Integer secondary_reference_count = 0;

		try {
			secondary_reference_count = lettDigitalContent.getJSONObject("pic").getInt("secondary");
		} catch (JSONException e) { 

		}
		if(secondary_reference_count == 0) { // no-reference tem precedência sobre no-image
			pic_secondary.put("status", NO_REFERENCE);
		} 
		else if (pic.getInt("count") <= 1) {
			pic_secondary.put("status", NO_IMAGE);
		}
		else if(pic.getInt("count")-1 >= secondary_reference_count) {
			pic_secondary.put("status", COMPLETE);
		} 
		else {
			pic_secondary.put("status", INCOMPLETE);
		}

		pic.put("secondary", pic_secondary);
	}

}
