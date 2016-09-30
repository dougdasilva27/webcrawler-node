package br.com.lett.crawlernode.crawlers.extractionutils;

import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMagazineluizaCrawlerUtils {
	
	/**
	 * Get the script having a json with the sku information.
	 * In the example below the sku has 2 variations. Even if one of them
	 * is'n show on the main page of the sku, this json contains it's information.
	 * 
	 * It was observed that when the variation is unavailable, the ecommerce website
	 * doesn't display the variation as an option to be selected by the user. When the
	 * two variations are unavailable, the website doesn't display none of them. Instead,
	 * it creates a new base product that is displayed as unavailable for the user.
	 * 
	 * For instance, if we have Ar Condicionado Midea 110 Vols and Ar Condicionado Midea 220 Volts
	 * when the two are unavailable, the website only display the product Ar Condicionado Midea.
	 * If one of them is available, only the other is shown on a box selector for the user.
	 * 
	 * The crawler must use the JSON retrieved in this method, so it won't create the new
	 * "false" sku "Ar Condicionado Midea" on database. This way it continues to crawl the two variations
	 * and correctly crawl the availability as false.
	 * 
	 * eg:
	 * 
	 * "reference":"com Função Limpa Fácil",
	 *	"extendedWarranty":true,
	 *	"idSku":"0113562",
	 *	"idSkuFull":"011356201",
	 *	"salePrice":429,
	 *	"imageUrl":"http://i.mlcdn.com.br//micro-ondas-midea-liva-mtas4-30l-com-funcao-limpa-facil/v/210x210/011356201.jpg",
	 *	"fullName":"micro%20ondas%20midea%20liva%20mtas4%2030l%20-%20com%20funcao%20limpa%20facil",
	 *	"details":[
	 *		{
	 *			"color":"Branco",
	 *			"sku":"011356201",
	 *			"voltage":"110 Volts"
	 *		},
	 *		{
	 *			"color":"Branco",
	 *			"sku":"011356301",
	 *			"voltage":"220 Volts"
	 *		}
	 *	],
	 *	"title":"Micro-ondas Midea Liva MTAS4 30L",
	 *	"cashPrice":407.55,
	 *	"brand":"midea",
	 *	"stockAvailability":true
	 * 
	 * @return a json object containing all sku informations in this page.
	 */
	public static JSONObject crawlFullSKUInfo(Document document) {
		Elements scriptTags = document.getElementsByTag("script");
		JSONObject skuJsonProduct = new JSONObject();
		JSONObject skuJson = null;

		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("var digitalData = ")) {
					skuJson = new JSONObject
							(
									node.getWholeData().split(Pattern.quote("var digitalData = "))[1] +
									node.getWholeData().split(Pattern.quote("var digitalData = "))[1].split(Pattern.quote("}]};"))[0]
									);

				}
			}        
		}

		if(skuJson.has("page")){
			JSONObject jsonPage = skuJson.getJSONObject("page");
			
			if(jsonPage.has("product")){
				skuJsonProduct = jsonPage.getJSONObject("product");
			}
		}
		
		return skuJsonProduct;
	}
	
	public static boolean hasVoltageSelector(JSONArray skus) {
		for (int i = 0; i < skus.length(); i++) {
			JSONObject sku = skus.getJSONObject(i);
			if (sku.has("voltage")) {
				if (!sku.getString("voltage").equals("Bivolt")) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * If in this page we have other skus with diferrent urls for each one.
	 * This is used because the crawler must extract only the default product in this page.
	 * 
	 * @return
	 */
	public static boolean skusWithURL(Document document) {
		Element ulElement = document.select(".js-buy-option-box.container-basic-information").last();
		if(ulElement != null){
			Elements options = ulElement.select("span.js-buy-option");
			if (options.size() == 0) options = ulElement.select("li");
			for (Element option : options) {
				Element hrefElement = option.select("a[href]").first();
				if (hrefElement != null) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String selectCurrentSKUValue(Document document) {
		Element ulElement = document.select(".js-buy-option-box.container-basic-information").last();
		if(ulElement != null){
			Elements options = ulElement.select("input");
			for (Element option : options) {
				Element checkedElement = option.select("[checked]").first();
				if (checkedElement != null) {
					return option.attr("value");
				}
			}
		}
		return null;
	}
	
	public static JSONObject getSKUDetails(String selectedValue, JSONObject fullSKUInfoJson) {
		if(fullSKUInfoJson.has("details")){
			JSONArray details = fullSKUInfoJson.getJSONArray("details");
			
			String idSku = fullSKUInfoJson.getString("idSku");
			String idSkuFull = fullSKUInfoJson.getString("idSkuFull");
			
			for (int i = 0; i < details.length(); i++) {
				JSONObject detail = details.getJSONObject(i);
				if (idSku.equals(detail.getString("sku")) || idSkuFull.equals(detail.getString("sku"))) {
					return detail;
				}
			}
		}
		return null;
	}
	
	/**
	 * Analyze if the current internalId is displayed as an option on the product main page
	 * in a selection box. If it isn't, it means that this products is not being displayed because
	 * it's variations is unavailable.
	 * 
	 * @return boolean true if exists an option if this internalId or false otherwise.
	 */
	public static boolean hasOptionSelector(String internalId, Document document) {
		Element skuUl = document.select(".js-buy-option-box.container-basic-information .js-buy-option-list").first();
		if (skuUl != null) {
			Elements skuOptions = skuUl.select("li");
			for (Element option : skuOptions) {
				Element input = option.select("input").first();
				if (input != null) {
					String value = input.attr("value").trim();
					if (value.equals(internalId)) return true;
				}
			}
		}
		return false;
	}

}
