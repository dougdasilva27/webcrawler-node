package br.com.lett.crawlernode.processor.digitalcontent;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.lett.crawlernode.processor.base.PicStatus;

public class Pic {

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
			pic_secondary.put("status", PicStatus.NO_REFERENCE);
		} 
		else if (pic.getInt("count") <= 1) {
			pic_secondary.put("status", PicStatus.NO_IMAGE);
		}
		else if(pic.getInt("count")-1 >= secondary_reference_count) {
			pic_secondary.put("status", PicStatus.COMPLETE);
		} 
		else {
			pic_secondary.put("status", PicStatus.INCOMPLETE);
		}

		pic.put("secondary", pic_secondary);
	}

}
