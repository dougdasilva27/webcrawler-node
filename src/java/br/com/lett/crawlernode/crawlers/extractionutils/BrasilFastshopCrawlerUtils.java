package br.com.lett.crawlernode.crawlers.extractionutils;

import java.util.Scanner;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilFastshopCrawlerUtils {
	
	/**
	 * Get the data layer json as a String and remove some unused fields
	 * that would cause JSON parsing errors. The fields that are removed
	 * on the example from the getDataLayerScript() method:
	 * 
	 * 'visitorId': getUserInfo(4),
	 * 'visitorState': getUserInfo(1), 
	 * 'visitorCity': getUserInfo(0),   
	 * 'visitorGender': getGTMGenderLabel(getUserInfo(2)),  
	 * 'visitorAge': getUserInfo(5),
	 * 'visitorSource': '',
	 * 'visitorCampaign': getCookie("partner"),
	 * 
	 * The final json object will be:
	 * 
	 * {
	 * 	"productDepartment":"Tratamento de Ar",
	 * 	"isBundle":"false",
	 * 	"mktPlacePartner":"Multi-Ar",
	 * 	"productId":"4395_PRD",
	 * 	"productBrand":"Midea",
	 * 	"channel":"wcs",
	 * 	"available":"true",
	 * 	"productName":"Ar Condicionado Split Hi-Wall Elite Midea com 30.000 BTUs Quente e Frio Branco - 42MLQC30M5/38KQJ30M5",
	 * 	"productCategory":[{"category":"OMNIPLACE"},{"category":"O2 - TRATAMENTO DE AR"},{"category":"MULTIAR"}],
	 * 	"installmentNumber":"12",
	 * 	"productSalePrice":"3469.00",
	 * 	"productQuantity":"1",
	 * 	"pageType":"C",
	 * 	"productThumbnail":"//prdresources10-a.akamaihd.net/wcsstore/FastShopCAS/Marketplace/_LA_LinhadeAr/4395/4395_PRD_160_1.jpg",
	 * 	"productPrice":"3469.00",
	 * 	"installmentValue":"289.08",
	 * 	"installmentTotalValue":""
	 * }
	 *
	 * 
	 * @param document
	 * @return an instance of the json object containing all necessary sku data
	 */
	public static JSONObject crawlFullSKUInfo(Document document) {
		String dataLayer = getDataLayerScript(document);
		String processedDataLayer = "";
		
		Scanner scanner = new Scanner(dataLayer);

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if ( !line.contains("visitorId") 		&& 
					!line.contains("visitorState") 	&& 
					!line.contains("visitorCity") 	&&
					!line.contains("visitorGender") && 
					!line.contains("visitorAge") 	&&
					!line.contains("visitorSource")	&&
					!line.contains("visitorCampaign")) {
				processedDataLayer = processedDataLayer + line.substring(0, line.length()) + "\n";
			}
		}
		
		if (scanner != null) {
			scanner.close();
		}
		
		return new JSONObject(processedDataLayer);
	}
	
	/**
	 * Selects from the html of the sku page, the element with tag "script"
	 * that contains the dataLayer json object that is injected in Javascript.
	 * 
	 *  e.g:
	 *  
	 * dataLayer = [{
	 * 'pageType': 'C',
	 * 'visitorId': getUserInfo(4),
	 * 'visitorState': getUserInfo(1), 
	 * 'visitorCity': getUserInfo(0),   
	 * 'visitorGender': getGTMGenderLabel(getUserInfo(2)),  
	 * 'visitorAge': getUserInfo(5),
	 * 'visitorSource': '',
	 * 'visitorCampaign': getCookie("partner"),
	 * 'productId': '4395_PRD',
	 * 'productName': 'Ar Condicionado Split Hi-Wall Elite Midea com 30.000 BTUs Quente e Frio Branco - 42MLQC30M5/38KQJ30M5', 
	 * 'productCategory': [{category:'OMNIPLACE'},{category:'O2 - TRATAMENTO DE AR'},{category:'MULTIAR'}], 
	 * 'productDepartment': 'Tratamento de Ar', 
	 * 'productBrand': 'Midea', 
	 * 'productPrice': '3469.00', 
	 * 'productSalePrice': '3469.00',       
	 * 'productQuantity': '1', 
	 * 'productThumbnail': '//prdresources10-a.akamaihd.net/wcsstore/FastShopCAS/Marketplace/_LA_LinhadeAr/4395/4395_PRD_160_1.jpg',
	 * 'channel': 'wcs',
	 * 'isBundle': 'false',
	 * 'available': 'true',
	 * 'installmentValue': '289.08',
	 * 'installmentNumber': '12',
	 * 'installmentTotalValue': '',
	 * 'mktPlacePartner':'Multi-Ar'
	 *	}];  
	 *  
	 * @param document
	 * @return
	 */
	private static String getDataLayerScript(Document document) {
		Elements scriptTags = document.getElementsByTag("script");

		for (Element tag : scriptTags){                
			for (DataNode node : tag.dataNodes()) {
				if(tag.html().trim().startsWith("dataLayer = [")) {
					return node.getWholeData().split(Pattern.quote("dataLayer = ["))[1].split(Pattern.quote("];"))[0];
				}
			}        
		}
		
		return null;
	}
	
	
}
