package br.com.lett.crawlernode.processor.digitalcontent;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.lett.crawlernode.processor.base.DigitalContentAnalyser;

public class RulesEvaluation {

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
		for(int i = 0; i < nameRulesDesired.length(); i++) {
			JSONObject rule = nameRulesDesired.getJSONObject(i);

			JSONObject r = DigitalContentAnalyser.validateRule(originalName, rule);
			r.put("name", rule.getString("name"));

			if(rule.has("section") && !rule.isNull("section")) {
				r.put("section", rule.getString("section"));
			}
			if(rule.has("type") && !rule.isNull("type")) {
				r.put("type", rule.getString("type"));
			}
			if(rule.has("condition") && !rule.isNull("condition")) {
				r.put("condition", rule.getInt("condition"));
			}

			nameRulesResults.put(r);
		}

		return nameRulesResults;
	}

	/**
	 * Evaluates description rules results comparing the original description
	 * crawled from the web, with that on the reference digital content.
	 * 
	 * @param lettDigitalContent
	 * @param originalDescription
	 * @return
	 */
	public static JSONArray computeDescriptionRulesResults(JSONObject lettDigitalContent, String originalDescription) {
		JSONArray descriptionRulesResults = new JSONArray();

		// get reference description rules
		JSONArray descriptionRulesDesired = new JSONArray();
		if (lettDigitalContent.has("description_rules") ) {
			descriptionRulesDesired = lettDigitalContent.getJSONArray("description_rules");
		}

		// iterate through each rule and see if it gets satisfied or not
		for (int i = 0; i < descriptionRulesDesired.length(); i++) {
			JSONObject rule = descriptionRulesDesired.getJSONObject(i);

			JSONObject r = DigitalContentAnalyser.validateRule(originalDescription, rule);
			r.put("name", rule.getString("name"));

			if (rule.has("section") && !rule.isNull("section")) {
				r.put("section", rule.getString("section"));
			}
			if (rule.has("type") && !rule.isNull("type")) {
				r.put("type", rule.getString("type"));
			}
			if (rule.has("condition") && !rule.isNull("condition")) {
				r.put("condition", rule.getInt("condition"));
			}

			descriptionRulesResults.put(r);
		}

		return descriptionRulesResults;
	}

	/**
	 * Create a summary of all naming and descriptin rules
	 * 
	 * @param nameRulesResults
	 * @param descriptionRulesResults
	 * @return
	 */
	public static JSONObject sumarizeRules(JSONArray nameRulesResults, JSONArray descriptionRulesResults) {
		JSONObject rules_results = new JSONObject();
		rules_results.put("name", true); // assuming the name as OK
		rules_results.put("description", new JSONObject());

		// 	Ver se ocorreu alguma regra de nome que não foi satisfeita
		for(int i = 0; i < nameRulesResults.length(); i++) {
			if(!nameRulesResults.getJSONObject(i).getBoolean("satisfied")) {

				// Marca como não-satisfeita e sai do loop
				rules_results.put("name", false);
				break;
			}
		}


		// see if we have some description rule in some section that was not satisfied
		for(int i = 0; i < descriptionRulesResults.length(); i++) {

			// if none of the rules in the section was evaluated yet, we assume it as OK
			if(!rules_results.getJSONObject("description").has(descriptionRulesResults.getJSONObject(i).getString("section"))) {
				rules_results.getJSONObject("description").put(descriptionRulesResults.getJSONObject(i).getString("section"), true);
			}

			if(!descriptionRulesResults.getJSONObject(i).getBoolean("satisfied")) {

				// mark as not satisfied				
				rules_results.getJSONObject("description").put(descriptionRulesResults.getJSONObject(i).getString("section"), false);
			}
		}

		return rules_results;
	}

}
