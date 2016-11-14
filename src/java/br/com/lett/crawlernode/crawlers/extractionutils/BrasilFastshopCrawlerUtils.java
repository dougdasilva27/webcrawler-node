package br.com.lett.crawlernode.crawlers.extractionutils;

import org.json.JSONArray;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BrasilFastshopCrawlerUtils {
	
	/**
	 * 
		[ {
			"catentry_id" : "4611686018425146172",
			"Attributes" :	{
							"Voltagem_110V":"1"
						},
						"ItemImage" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
						"ItemImage467" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
						"ItemThumbnailImage" : "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_160_1.jpg"
						,"ItemAngleThumbnail" : {
							"image_1" : "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_70_1.jpg",
							"image_2" : "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_3.jpg",
							"image_3" : "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_2.jpg",
							"image_4" : "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_4.jpg",
							"image_5" : "//prdresources4-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_5.jpg",
							"image_6" : "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_6.jpg",
							"image_7" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_7.jpg",
							"image_8" : "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_8.jpg",
						},
						"ItemAngleFullImage" : {
							"image_1" : "//prdresources10-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
							"image_2" : "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_3.jpg",
							"image_3" : "//prdresources4-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_2.jpg",
							"image_4" : "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_4.jpg",
							"image_5" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_5.jpg",
							"image_6" : "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_6.jpg",
							"image_7" : "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_7.jpg",
							"image_8" : "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_8.jpg",
						},
						"ShippingAvailability" : "1"
					},

					{
			"catentry_id" : "4611686018425146173",
			"Attributes" :	{
							"Voltagem_220V":"1"
							},
				"ItemImage" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
				"ItemImage467" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
				"ItemThumbnailImage" : "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_160_1.jpg",
					"ItemAngleThumbnail" : {
							"image_1" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_1.jpg",
							"image_2" : "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_3.jpg",
							"image_3" : "//prdresources6-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_2.jpg",
							"image_4" : "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_4.jpg",
							"image_5" : "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_5.jpg",
							"image_6" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_6.jpg",
							"image_7" : "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_7.jpg",
							"image_8" : "//prdresources10-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_8.jpg",
					},
					"ItemAngleFullImage" : {
							"image_1" : "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_1.jpg",
							"image_2" : "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_3.jpg",
							"image_3" : "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_2.jpg",
							"image_4" : "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_4.jpg",
							"image_5" : "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_5.jpg",
							"image_6" : "//prdresources4-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_6.jpg",
							"image_7" : "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_7.jpg",
							"image_8" : "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_8.jpg",
						},
					"ShippingAvailability" : "1"
			}
		]

	 * @param document
	 * @return
	 */
	public static JSONArray crawlSkusInfo(Document document) {
		JSONArray skusInfo = null;
		Element skusInfoElement = document.select("div.widget_product_info_viewer_position div[id^=entitledItem_]").first();
		if (skusInfoElement != null) {
			try {
				skusInfo = new JSONArray(skusInfoElement.text().trim());
			} catch (Exception e) {
				skusInfo = new JSONArray();
			}
		}
		if (skusInfo == null) skusInfo = new JSONArray();
		
		return skusInfo;
	}	
	
}
