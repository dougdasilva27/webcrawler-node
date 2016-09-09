package br.com.lett.crawlernode.processor.digitalcontent;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.processor.base.DigitalContentAnalyser;

public class NameRules {
	
	public static JSONArray computeNameRulesResults(JSONObject lettDigitalContent, String originalName) {
		JSONArray nameRulesResults = new JSONArray();

		// 		2.1) Lendo regras de nomeclatura definidas no objeto lett
		JSONArray nameRulesDesired = new JSONArray();
		if(lettDigitalContent.has("name_rules") ) {
			nameRulesDesired = lettDigitalContent.getJSONArray("name_rules");
		}

		// 		2.2) Para cada regra, ver se é satisfeita ou não

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
