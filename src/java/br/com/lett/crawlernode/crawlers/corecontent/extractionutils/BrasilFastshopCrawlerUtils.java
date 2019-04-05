package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.List;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilFastshopCrawlerUtils {

  public static String crawlPartnerId(Session session) {
    String redirectUrl = CrawlerUtils.crawlFinalUrl(session.getOriginalURL(), session);
    String partnerId = null;

    String[] tokens = redirectUrl.split("/");
    for (String token : tokens) {
      if (token.endsWith("_PRD")) {
        partnerId = token;
        break;
      }
    }

    return partnerId;
  }

  public static String crawlPartnerId(Document doc) {
    String partnerId = null;
    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.html();
      String token = "'productId':";
      script = script.replace(" ", "");

      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf(',', x);

        partnerId = script.substring(x, y).replace("'", "").trim();
      }
    }

    return partnerId;
  }

  /**
   * 
   * [ { "catentry_id" : "4611686018425146172", "Attributes" : { "Voltagem_110V":"1" }, "ItemImage" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
   * "ItemImage467" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
   * "ItemThumbnailImage" :
   * "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_160_1.jpg"
   * ,"ItemAngleThumbnail" : { "image_1" :
   * "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_70_1.jpg",
   * "image_2" :
   * "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_3.jpg",
   * "image_3" :
   * "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_2.jpg",
   * "image_4" :
   * "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_4.jpg",
   * "image_5" :
   * "//prdresources4-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_5.jpg",
   * "image_6" :
   * "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_6.jpg",
   * "image_7" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_7.jpg",
   * "image_8" :
   * "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_8.jpg",
   * }, "ItemAngleFullImage" : { "image_1" :
   * "//prdresources10-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
   * "image_2" :
   * "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_3.jpg",
   * "image_3" :
   * "//prdresources4-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_2.jpg",
   * "image_4" :
   * "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_4.jpg",
   * "image_5" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_5.jpg",
   * "image_6" :
   * "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_6.jpg",
   * "image_7" :
   * "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_7.jpg",
   * "image_8" :
   * "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_8.jpg",
   * }, "ShippingAvailability" : "1" },
   * 
   * { "catentry_id" : "4611686018425146173", "Attributes" : { "Voltagem_220V":"1" }, "ItemImage" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
   * "ItemImage467" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_1S_Selos/UT/3T20038905/V2/3T20038905_PRD_447_1.jpg",
   * "ItemThumbnailImage" :
   * "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_160_1.jpg",
   * "ItemAngleThumbnail" : { "image_1" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_1.jpg",
   * "image_2" :
   * "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_3.jpg",
   * "image_3" :
   * "//prdresources6-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_2.jpg",
   * "image_4" :
   * "//prdresources7-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_4.jpg",
   * "image_5" :
   * "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_5.jpg",
   * "image_6" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_6.jpg",
   * "image_7" :
   * "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_7.jpg",
   * "image_8" :
   * "//prdresources10-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_70_8.jpg",
   * }, "ItemAngleFullImage" : { "image_1" :
   * "//prdresources9-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_1.jpg",
   * "image_2" :
   * "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_3.jpg",
   * "image_3" :
   * "//prdresources3-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_2.jpg",
   * "image_4" :
   * "//prdresources8-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_4.jpg",
   * "image_5" :
   * "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_5.jpg",
   * "image_6" :
   * "//prdresources4-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_6.jpg",
   * "image_7" :
   * "//prdresources5-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_7.jpg",
   * "image_8" :
   * "//prdresources1-a.akamaihd.net/wcsstore/FastShopCAS/imagens/_UT_Utilidades/3T20038905/V3/3T20038905_PRD_447_8.jpg",
   * }, "ShippingAvailability" : "1" } ]
   * 
   * @param document
   * @return
   */
  public static JSONArray crawlSkusInfo(Document document) {
    JSONArray skusInfo = new JSONArray();
    Element skusInfoElement = document.select("div.widget_product_info_viewer_position div[id^=entitledItem_]").first();
    if (skusInfoElement != null) {
      try {
        skusInfo = new JSONArray(skusInfoElement.text().trim());
      } catch (Exception e) {
      }
    }

    return skusInfo;
  }

  /**
   * { "buyable": true, "images": [ { "path":
   * "FastShopCAS\/imagens\/_VD_Video\/LG43LJ5550\/LG43LJ5550_PRD_447_1.jpg", "usage":
   * "ANGLEIMAGES_FULLIMAGE" } ], "longDescription": "<div> bla bla </div", "manuals": [ { "path":
   * "\/wcsstore\/FastShopCAS\/manuais\/VD\/LG\/LG43LJ5550.pdf", "usage": "USERMANUAL" } ],
   * "manufacturer": "LG", "partNumber": "LG43LJ5550_PRD", "priceOffer": "1613.80", "priceTag":
   * "2299.00", "productID": "4611686018427104522", "shortDescription": "Smart TV LG LED Full HD
   * 43\u201d com Time Machine Ready, webOS 3.5, Quick Access e Wi-Fi - 43LJ5550", "thumbnail":
   * "\/wcsstore\/FastShopCAS\/imagens\/_VD_Video\/LG43LJ5550\/LG43LJ5550_PRD_160_1.jpg", "voltage": [
   * { "catEntry": "4611686018427104523", "identifier": "BIVOLT", "name": "Bivolt", "partNumber":
   * "LG43LJ5550B" } ] }
   * 
   * @param partnerId
   * @param session
   * @param cookies
   * @return
   */
  public static JSONObject crawlApiJSON(String partnerId, Session session, List<Cookie> cookies, DataFetcher dataFetcher) {
    JSONObject apiJson = new JSONObject();

    if (partnerId != null) {
      String apiUrl = "https://www.fastshop.com.br/wcs/resources/v1/products/byPartNumber/" + partnerId;

      Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
      apiJson = CrawlerUtils.stringToJson(dataFetcher.get(session, request).getBody());
    }

    return apiJson;
  }

  /**
   * Json Prices { priceData":{ "catalogEntryId":"4611686018426116511",
   * "displayPriceProdInactive":"true", "offerPrice":"R$ 439,61", "offerPriceValue":"439.61",
   * "listPrice":"R$ 649,00", "installmentPrice":"3x de R$ 153,00 iguais", "interestPrice":"juros de
   * 2,19% a.m. e 29,69% a.a.", "totalPrice":"Total a prazo: R$ 459,00", "displayPriceRange":"",
   * "displayLinkWhyInterest":"" } }
   */

  public static JSONObject fetchPrices(String internalId, boolean available, Session session, Logger logger, DataFetcher dataFetcher) {
    JSONObject jsonPrice = new JSONObject();

    if (available) {
      String url = "https://www.fastshop.com.br/loja/AjaxPriceDisplayView?" + "catEntryIdentifier=" + internalId
          + "&hotsite=fastshop&fromWishList=false&" + "storeId=10151&displayPriceRange=true&displayLinkWhyInterest=true";

      Request request = RequestBuilder.create().setUrl(url).build();
      String json = dataFetcher.get(session, request).getBody();

      try {
        int x = json.indexOf("/*");
        int y = json.indexOf("*/", x + 2);

        json = json.substring(x + 2, y);

        jsonPrice = new JSONObject(json);
      } catch (Exception e) {
        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
    }
    return jsonPrice;
  }
}
