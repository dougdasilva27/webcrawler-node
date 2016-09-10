package br.com.lett.crawlernode.processor.digitalcontent;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.processor.base.DigitalContentAnalyser;

public class NameRules {
	
	/**
	 * Compute name rules results comparing the original name with
	 * the rules defined on the reference digital content.
	 * 
	 * @param lettDigitalContent
	 * @param originalName
	 * @return a json array containing the result for each rule
	 */
	public static JSONArray computeNameRulesResults(JSONObject lettDigitalContent, String originalName) {
		JSONArray nameRulesResults = new JSONArray();

		// get the reference rules
		JSONArray nameRulesDesired = new JSONArray();
		if(lettDigitalContent.has("name_rules") ) {
			nameRulesDesired = lettDigitalContent.getJSONArray("name_rules");
		}

		// iterate through each rule and see if it gets satisfied
		for(int i=0; i<nameRulesDesired.length(); i++) {
			JSONObject rule = nameRulesDesired.getJSONObject(i);

			JSONObject r = DigitalContentAnalyser.validateRule(originalName, rule);
			r.put("name", rule.getString("name"));

			if(rule.has("section") 		&& !rule.isNull("section")) 	r.put("section", rule.getString("section"));
			if(rule.has("type") 	 	&& !rule.isNull("type")) 		r.put("type", rule.getString("type"));
			if(rule.has("condition") 	&& !rule.isNull("condition")) 	r.put("condition", rule.getInt("condition"));

			nameRulesResults.put(r);
		}
		
		return nameRulesResults;
	}

}
