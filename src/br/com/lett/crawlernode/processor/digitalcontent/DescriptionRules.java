package br.com.lett.crawlernode.processor.digitalcontent;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.processor.base.DigitalContentAnalyser;

public class DescriptionRules {

	public static JSONArray computeDescriptionRulesResults(JSONObject lettDigitalContent, String originalDescription) {
		// 3) Avaliando regras de descrição
		JSONArray descriptionRulesResults = new JSONArray();

		// 		3.1) Lendo regras de descrição definidas no objeto lett
		JSONArray descriptionRulesDesired = new JSONArray();
		if(lettDigitalContent.has("description_rules") ) {
			descriptionRulesDesired = lettDigitalContent.getJSONArray("description_rules");
		}

		// 		3.2) Para cada regra, ver se é satisfeita ou não

		for(int i=0; i<descriptionRulesDesired.length(); i++) {
			JSONObject rule = descriptionRulesDesired.getJSONObject(i);

			JSONObject r = DigitalContentAnalyser.validateRule(originalDescription, rule);
			r.put("name", rule.getString("name"));

			if(rule.has("section") 		&& !rule.isNull("section")) 	r.put("section", rule.getString("section"));
			if(rule.has("type") 	 	&& !rule.isNull("type")) 		r.put("type", rule.getString("type"));
			if(rule.has("condition") 	&& !rule.isNull("condition")) 	r.put("condition", rule.getInt("condition"));

			descriptionRulesResults.put(r);
		}
		
		return descriptionRulesResults;
	}

}
