package br.com.lett.crawlernode.util;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class CrawlerUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerUtils.class);

  /**
   * Crawl skuJson from html in VTEX Sites
   * 
   * @param document
   * @param session
   * @return
   */
  public static JSONObject crawlSkuJsonVTEX(Document document, Session session) {
    Elements scriptTags = document.getElementsByTag("script");
    String scriptVariableName = "var skuJson_0 = ";
    JSONObject skuJson = new JSONObject();
    String skuJsonString = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith(scriptVariableName)) {
          skuJsonString = node.getWholeData().split(Pattern.quote(scriptVariableName))[1]
              + node.getWholeData().split(Pattern.quote(scriptVariableName))[1].split(Pattern.quote("};"))[0];
          break;
        }
      }
    }

    if (skuJsonString != null) {
      try {
        skuJson = new JSONObject(skuJsonString);

      } catch (JSONException e) {
        Logging.printLogError(LOGGER, session, "Error creating JSONObject from var skuJson_0");
        Logging.printLogError(LOGGER, session, CommonMethods.getStackTraceString(e));
      }
    }

    return skuJson;
  }

  public static JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex)
      throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
    return selectJsonFromHtml(doc, cssElement, token, finalIndex, true);
  }

  /**
   * Crawl json inside element html
   *
   * e.g: vtxctx = { skus:"825484", searchTerm:"", categoryId:"38", categoryName:"Leite infantil",
   * departmentyId:"4", departmentName:"Infantil", url:"www.araujo.com.br" };
   *
   * token = "vtxctx=" finalIndex = ";"
   * 
   * @param doc
   * @param cssElement selector used to get the desired json element
   * @param token whithout spaces
   * @param finalIndex if final index is null or is'nt in html, substring will use only the token
   * @return JSONObject
   * 
   * @throws JSONException
   * @throws ArrayIndexOutOfBoundsException if finalIndex doesn't exists or there is a duplicate
   * @throws IllegalArgumentException if doc is null
   */
  public static JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex, boolean withoutSpaces)
      throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {

    if (doc == null)
      throw new IllegalArgumentException("Argument doc cannot be null");

    JSONObject object = new JSONObject();

    Elements scripts = doc.select(cssElement);

    for (Element e : scripts) {
      String script = e.html();

      script = withoutSpaces ? script.replace(" ", "") : script;

      if (script.contains(token)) {
        int x = script.indexOf(token) + token.length();

        String json;

        if (script.contains(finalIndex)) {
          int y = script.indexOf(finalIndex, x);
          json = script.substring(x, y).trim();
        } else {
          json = script.substring(x).trim();
        }

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            object = new JSONObject(json);
          } catch (Exception e1) {
            Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }


    return object;
  }

  /**
   * Crawl description of stores with flix media
   * 
   * @param storeId -> you will find this id in product html, may be close of description
   * @param ean -> product Ean, in vtex stores you find in a javascript script
   * @param session -> session of tasks
   * @return
   */
  public static String crawlDescriptionFromFlixMedia(String storeId, String ean, Session session) {
    StringBuilder description = new StringBuilder();

    String url =
        "https://media.flixcar.com/delivery/js/inpage/" + storeId + "/br/ean/" + ean + "?&=" + storeId + "&=br&ean=" + ean + "&ssl=1&ext=.js";

    String script = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null);
    final String token = "$(\"#flixinpage_\"+i).inPage";

    JSONObject productInfo = new JSONObject();

    if (script.contains(token)) {
      int x = script.indexOf(token + " (") + token.length() + 2;
      int y = script.indexOf(");", x);

      String json = script.substring(x, y);

      try {
        productInfo = new JSONObject(json);
      } catch (JSONException e) {
        Logging.printLogError(LOGGER, session, CommonMethods.getStackTrace(e));
      }
    }

    if (productInfo.has("product")) {
      String id = productInfo.getString("product");

      String urlDesc = "https://media.flixcar.com/delivery/inpage/show/" + storeId + "/br/" + id + "/json?c=jsonpcar" + storeId + "r" + id
          + "&complimentary=0&type=.html";
      String scriptDesc = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, urlDesc, null, null);

      if (scriptDesc.contains("({")) {
        int x = scriptDesc.indexOf("({") + 1;
        int y = scriptDesc.lastIndexOf("})") + 1;

        String json = scriptDesc.substring(x, y);

        try {
          JSONObject jsonInfo = new JSONObject(json);

          if (jsonInfo.has("html")) {
            if (jsonInfo.has("css")) {
              description.append("<link href=\"" + jsonInfo.getString("css") + "\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\">");
            }

            description.append(jsonInfo.get("html").toString().replace("//media", "https://media"));
          }
        } catch (JSONException e) {
          Logging.printLogError(LOGGER, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    return description.toString();
  }

  /**
   * AssembleMarketplaceFromMap
   * 
   * Return object Marketplaces only with sellers of the market
   * 
   * @param marketplaceMap -> map of sellerName - Prices
   * @param sellerNameLowerList -> list of principal sellers
   * @param session -> session of crawler
   * 
   * @return Marketplace
   */
  public static Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap, List<String> sellerNameLowerList, Session session) {
    Marketplace marketplace = new Marketplace();

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerNameLowerList.contains(sellerName)) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Prices prices = marketplaceMap.get(sellerName);

        if (prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          // Pegando o preço de uma vez no cartão
          Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = price.floatValue();

          sellerJSON.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
        }

        sellerJSON.put("prices", marketplaceMap.get(sellerName).toJSON());

        try {
          Seller seller = new Seller(sellerJSON);
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(LOGGER, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }
}
