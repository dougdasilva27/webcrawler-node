package br.com.lett.crawlernode.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.session.Session;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class CrawlerUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerUtils.class);
  public static final String CSS_SELECTOR_IGNORE_FIRST_CHILD = ":not(:first-child)";

  /**
   * 
   * This function get all sellers from marketplace map considered own market
   * 
   * @param marketplaceMap
   * @param sellerList
   * @return
   */
  public static List<String> getMainSellers(Map<String, Prices> marketplaceMap, List<String> sellerList) {
    List<String> mainSellersList = new ArrayList<>();

    for (String seller : sellerList) {
      for (Entry<String, Prices> entry : marketplaceMap.entrySet()) {
        if (entry.getKey().toLowerCase().startsWith(seller.toLowerCase())) {
          mainSellersList.add(entry.getKey());
        }
      }
    }

    return mainSellersList;
  }

  /**
   * Get availability from marketplaceMap
   * 
   * @param marketplaceMap
   * @param List<String> sellers
   * @return
   */
  public static boolean getAvailabilityFromMarketplaceMap(Map<String, Prices> marketplaceMap, List<String> sellerList) {
    boolean availability = false;

    for (String seller : sellerList) {
      availability = marketplaceMap.containsKey(seller);

      if (availability) {
        break;
      }
    }

    return availability;

  }

  /**
   * Get Prices From marketplace Map
   * 
   * @param marketplaceMap
   * @param List<String> sellers
   * @return
   */
  public static Prices getPrices(Map<String, Prices> marketplaceMap, List<String> sellerList) {
    Prices prices = new Prices();

    for (String seller : sellerList) {
      if (marketplaceMap.containsKey(seller)) {
        prices = marketplaceMap.get(seller);
        break;
      }
    }

    return prices;
  }

  /**
   * Scrap simple string from html
   * 
   * @param doc
   * @param cssSelector
   * @param ownText - if must use element.ownText(), if false will be used element.text()
   * @return
   */
  public static String scrapStringSimpleInfo(Element doc, String cssSelector, boolean ownText) {
    String info = null;

    Element infoElement = doc.selectFirst(cssSelector);
    if (infoElement != null) {
      info = ownText ? infoElement.ownText().trim() : infoElement.text().trim();
    }

    return info;
  }

  /**
   * Scrap simple string from html
   * 
   * @param doc
   * @param cssSelector
   * @param ownText - if must use element.ownText(), if false will be used element.text()
   * @return
   */
  public static String scrapStringSimpleInfoByAttribute(Element doc, String cssSelector, String att) {
    String info = null;

    Element infoElement = doc.selectFirst(cssSelector);
    if (infoElement != null) {
      info = infoElement.hasAttr(att) ? infoElement.attr(att) : null;
    }

    return info;
  }


  /**
   * Scrap simple price from html
   * 
   * @param document
   * @param cssSelector
   * @param ownText
   * @return Float
   */
  public static Float scrapSimplePriceFloat(Element document, String cssSelector, boolean ownText) {
    Float price = null;

    Element priceElement = document.selectFirst(cssSelector);
    if (priceElement != null) {
      price = MathUtils.parseFloatWithComma(ownText ? priceElement.ownText().trim() : priceElement.text().trim());
    }

    return price;
  }

  /**
   * Scrap simple price from html
   * 
   * @param document
   * @param cssSelector
   * @param ownText
   * @return Float
   */
  public static Float scrapSimplePriceFloatWithDots(Document document, String cssSelector, boolean ownText) {
    Float price = null;

    Element priceElement = document.selectFirst(cssSelector);
    if (priceElement != null) {
      price = MathUtils.parseFloatWithDots(ownText ? priceElement.ownText().trim() : priceElement.text().trim());
    }

    return price;
  }

  /**
   * Scrap simple price from html
   * 
   * @param document
   * @param cssSelector
   * @param ownText
   * @return Double
   */
  public static Double scrapSimplePriceDouble(Element document, String cssSelector, boolean ownText) {
    Double price = null;

    Element priceElement = document.selectFirst(cssSelector);
    if (priceElement != null) {
      price = MathUtils.parseDoubleWithComma(ownText ? priceElement.ownText().trim() : priceElement.text().trim());
    }

    return price;
  }

  /**
   * Scrap simple price from html
   * 
   * @param document
   * @param cssSelector
   * @param ownText
   * @return Double
   */
  public static Double scrapSimplePriceDoubleWithDots(Document document, String cssSelector, boolean ownText) {
    Double price = null;

    Element priceElement = document.selectFirst(cssSelector);
    if (priceElement != null) {
      price = MathUtils.parseDoubleWithDot(ownText ? priceElement.ownText().trim() : priceElement.text().trim());
    }

    return price;
  }

  /**
   * Scrap simple description from html
   * 
   * @param doc
   * @param selectors - description css selectors list
   * @return
   */
  public static String scrapSimpleDescription(Document doc, List<String> selectors) {
    StringBuilder description = new StringBuilder();

    for (String selector : selectors) {
      Element e = doc.selectFirst(selector);

      if (e != null) {
        description.append(e.outerHtml());
      }
    }

    return description.toString();
  }

  /**
   * Scrap simple description from html
   * 
   * @param doc
   * @param selectors - description css selectors list
   * @return
   */
  public static String scrapElementsDescription(Document doc, List<String> selectors) {
    StringBuilder description = new StringBuilder();

    for (String selector : selectors) {
      Elements elements = doc.select(selector);

      for (Element e : elements) {
        description.append(e.outerHtml());
      }
    }

    return description.toString();
  }

  /**
   * 
   * @param doc
   * @param cssSelector
   * @param attributes - attributes list for get image
   * @param protocol - https: or https:// or http: or http://
   * @param host - www.hostname.com.br
   * @return
   */
  public static String scrapSimplePrimaryImage(Document doc, String cssSelector, List<String> attributes, String protocol, String host) {
    String image = null;

    Element elementPrimaryImage = doc.selectFirst(cssSelector);
    if (elementPrimaryImage != null) {
      image = sanitizeUrl(elementPrimaryImage, attributes, protocol, host);
    }

    return image;
  }

  /**
   * 
   * @param doc
   * @param cssSelector
   * @param attributes - attributes list for get image
   * @param protocol - https: or https:// or http: or http://
   * @param host - www.hostname.com.br
   * @param primaryImage - if null, all images will be in secondary images
   * @return
   */
  public static String scrapSimpleSecondaryImages(Document doc, String cssSelector, List<String> attributes, String protocol, String host,
      String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(cssSelector);
    for (Element e : images) {
      String image = sanitizeUrl(e, attributes, protocol, host);

      if ((primaryImage == null || !primaryImage.equals(image)) && image != null) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }


  /**
   * Append host and protocol if url needs Scroll through all the attributes in the list sent in
   * sequence to find a url
   * 
   * @param element - code block that contains url (Jsoup Element)
   * @param attributes - ex: "href", "src"
   * @param protocol - ex: https: or https:// or http: or http://
   * @param host - send host in this format: "www.hostname.com.br"
   * @return Url with protocol and host
   */
  public static String sanitizeUrl(Element element, List<String> attributes, String protocol, String host) {
    String sanitizedUrl = null;

    for (String att : attributes) {
      String url = element.attr(att).trim();

      if (!url.isEmpty()) {
        sanitizedUrl = completeUrl(url, protocol, host);
        break;
      }
    }

    return sanitizedUrl;
  }

  /**
   * Complete url with host and protocol if necessary
   * 
   * @param url
   * @param protocol
   * @param host
   * @return
   */
  public static String completeUrl(String url, String protocol, String host) {
    StringBuilder sanitizedUrl = new StringBuilder();

    if (!url.startsWith("http") && url.contains(host)) {
      sanitizedUrl.append(protocol).append(url);
    } else if (!url.contains(host) && !url.startsWith("http")) {
      sanitizedUrl.append(protocol.endsWith("//") ? protocol : protocol + "//").append(host).append(url);
    } else {
      sanitizedUrl.append(url);
    }

    return sanitizedUrl.toString();
  }

  /**
   * Append host and protocol if url needs Scroll through all the attributes in the list sent in
   * sequence to find a url
   * 
   * @param element - code block that contains url (Jsoup Element)
   * @param attribute - ex: "href"
   * @param protocol - ex: https: or https:// or http: or http://
   * @param host - send host in this format: "www.hostname.com.br"
   * @return Url with protocol and host
   */
  public static String sanitizeUrl(Element element, String att, String protocol, String host) {
    StringBuilder sanitizedUrl = new StringBuilder();

    String url = element.attr(att).trim();

    if (!url.isEmpty()) {
      sanitizedUrl.append(completeUrl(url, protocol, host));
    }

    if (sanitizedUrl.toString().isEmpty()) {
      return null;
    }

    return sanitizedUrl.toString();
  }

  /**
   * Crawl cookies from a page
   * 
   * @param url - page where are cookies
   * @param cookiesToBeCrawled - list(string) of cookies to be crawled
   * @param domain - domain to set in cookie
   * @param path - path to set in cookie
   * @param session - crawler session
   * @return List<Cookie>
   */
  public static List<Cookie> fetchCookiesFromAPage(String url, List<String> cookiesToBeCrawled, String domain, String path, Session session) {
    List<Cookie> cookies = new ArrayList<>();

    Map<String, String> cookiesMap = DataFetcher.fetchCookies(session, url, cookies, 1);
    for (Entry<String, String> entry : cookiesMap.entrySet()) {
      String cookieName = entry.getKey().trim();

      if (cookiesToBeCrawled == null || cookiesToBeCrawled.isEmpty() || cookiesToBeCrawled.contains(cookieName)) {
        BasicClientCookie cookie = new BasicClientCookie(cookieName, entry.getValue());
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookies.add(cookie);
      }

    }

    return cookies;
  }

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

  /**
   * 
   * @param doc
   * @param cssElement
   * @param token
   * @param finalIndex
   * @return
   * @throws JSONException
   * @throws ArrayIndexOutOfBoundsException
   * @throws IllegalArgumentException
   * @deprecated
   */
  public static JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex)
      throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
    return selectJsonFromHtml(doc, cssElement, token, finalIndex, true);
  }

  /**
   * 
   * @param doc
   * @param cssElement
   * @param token
   * @param finalIndex
   * @param withoutSpaces
   * @return
   * @throws JSONException
   * @throws ArrayIndexOutOfBoundsException
   * @throws IllegalArgumentException
   * @deprecated
   */
  public static JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex, boolean withoutSpaces)
      throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
    return selectJsonFromHtml(doc, cssElement, token, finalIndex, withoutSpaces, false);
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
   * @param withoutSpaces remove all spaces
   * @param lastFinalIndex if true, the substring will find last index of finalIndex
   * @return JSONObject
   * 
   * @throws JSONException
   * @throws ArrayIndexOutOfBoundsException if finalIndex doesn't exists or there is a duplicate
   * @throws IllegalArgumentException if doc is null
   */
  public static JSONObject selectJsonFromHtml(Document doc, String cssElement, String token, String finalIndex, boolean withoutSpaces,
      boolean lastFinalIndex) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {

    if (doc == null)
      throw new IllegalArgumentException("Argument doc cannot be null");

    JSONObject object = new JSONObject();

    Elements scripts = doc.select(cssElement);
    boolean hasToken = token != null;

    for (Element e : scripts) {
      String script = e.html();

      script = withoutSpaces ? script.replace(" ", "") : script;

      if (!hasToken) {
        object = stringToJson(script.trim());
        break;
      } else if (script.contains(token) && (finalIndex == null || script.contains(finalIndex))) {
        object = stringToJson(extractSpecificStringFromScript(script, token, finalIndex, lastFinalIndex));
        break;
      }
    }

    return object;
  }

  /**
   * Crawl json inside element html
   *
   * e.g: vtxctx = [{ skus:"825484", searchTerm:"", categoryId:"38", categoryName:"Leite infantil",
   * departmentyId:"4", departmentName:"Infantil", url:"www.araujo.com.br" }, {...}];
   *
   * token = "vtxctx=" finalIndex = ";"
   * 
   * @param doc
   * @param cssElement selector used to get the desired json element
   * @param token whithout spaces
   * @param finalIndex if final index is null or is'nt in html, substring will use only the token
   * @param withoutSpaces remove all spaces
   * @param lastFinalIndex if true, the substring will find last index of finalIndex
   * @return JSONArray
   * 
   * @throws JSONException
   * @throws ArrayIndexOutOfBoundsException if finalIndex doesn't exists or there is a duplicate
   * @throws IllegalArgumentException if doc is null
   */
  public static JSONArray selectJsonArrayFromHtml(Document doc, String cssElement, String token, String finalIndex, boolean withoutSpaces,
      boolean lastFinalIndex) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {

    if (doc == null)
      throw new IllegalArgumentException("Argument doc cannot be null");

    JSONArray object = new JSONArray();

    Elements scripts = doc.select(cssElement);
    boolean hasToken = token != null;

    for (Element e : scripts) {
      String script = e.html();

      script = withoutSpaces ? script.replace(" ", "") : script;

      if (!hasToken) {
        object = stringToJsonArray(script.trim());
        break;
      } else if (script.contains(token) && (finalIndex == null || script.contains(finalIndex))) {
        object = stringToJsonArray(extractSpecificStringFromScript(script, token, finalIndex, lastFinalIndex));
        break;
      }
    }


    return object;
  }

  /**
   * Extract Json string from script(string) e.g: vtxctx = [{ skus:"825484", searchTerm:"",
   * categoryId:"38", categoryName:"Leite infantil", departmentyId:"4", departmentName:"Infantil",
   * url:"www.araujo.com.br" }, {...}];
   *
   * token = "vtxctx=" finalIndex = ";"
   * 
   * @param token whithout spaces
   * @param finalIndex if final index is null or is'nt in html, substring will use only the token
   * @param lastFinalIndex if true, the substring will find last index of finalIndex
   * @return
   */
  public static String extractSpecificStringFromScript(String script, String token, String finalIndex, boolean lastFinalIndex) {
    String json = null;

    int x = script.indexOf(token) + token.length();

    if (finalIndex != null) {
      int y;

      if (lastFinalIndex) {
        y = script.lastIndexOf(finalIndex);
      } else {
        y = script.indexOf(finalIndex, x);
      }

      int plusIndex = 0;

      if (finalIndex.equals("};")) {
        plusIndex = 1;
      }

      json = script.substring(x, y + plusIndex).trim();
    } else {
      json = script.substring(x).trim();
    }

    return json;
  }

  public static JSONObject stringToJson(String str) {
    JSONObject json = new JSONObject();

    if (str.startsWith("{") && str.endsWith("}")) {
      try {
        json = new JSONObject(str);
      } catch (Exception e1) {
        Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e1));
      }
    }

    return json;
  }

  public static JSONArray stringToJsonArray(String str) {
    JSONArray json = new JSONArray();

    if (str.startsWith("[") && str.endsWith("]")) {
      try {
        json = new JSONArray(str);
      } catch (Exception e1) {
        Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e1));
      }
    }

    return json;
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
   * @deprecated Because you need to send paramater card after sellerNameLowerList
   * 
   *             AssembleMarketplaceFromMap Return object Marketplaces only with sellers of the market
   * 
   * @param marketplaceMap -> map of sellerName - Prices
   * @param sellerNameLowerList -> list of principal sellers
   * @param session -> session of crawler
   * 
   * @return Marketplace
   */
  public static Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap, List<String> sellerNameLowerList, Session session) {
    return assembleMarketplaceFromMap(marketplaceMap, sellerNameLowerList, Card.VISA, session);
  }

  public static Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap, List<String> sellerNameLowerList, Card card,
      Session session) {
    return assembleMarketplaceFromMap(marketplaceMap, sellerNameLowerList, Arrays.asList(card), session);
  }

  /**
   * AssembleMarketplaceFromMap
   * 
   * Return object Marketplaces only with sellers of the market
   * 
   * @param marketplaceMap -> map of sellerName - Prices
   * @param sellerNameLowerList -> list of principal sellers
   * @param card -> models.Card like Card.VISA
   * @param session -> session of crawler
   * 
   * @return Marketplace
   */
  public static Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap, List<String> sellerNameLowerList, List<Card> cards,
      Session session) {
    Marketplace marketplace = new Marketplace();

    for (Entry<String, Prices> entry : marketplaceMap.entrySet()) {
      if (!sellerNameLowerList.contains(entry.getKey())) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", entry.getKey());

        Prices prices = entry.getValue();

        if (prices != null && !prices.isEmpty()) {
          Float price = extractPriceFromPrices(prices, cards);
          sellerJSON.put("price", price);
          sellerJSON.put("prices", prices.toJSON());

          try {
            Seller seller = new Seller(sellerJSON);
            marketplace.add(seller);
          } catch (Exception e) {
            Logging.printLogError(LOGGER, session, Util.getStackTraceString(e));
          }
        }
      }
    }

    return marketplace;
  }

  /**
   * 
   * Extract 1x price from model prices
   * 
   * @param prices - model.Prices
   * @param card - model.Card
   * @return
   */
  public static Float extractPriceFromPrices(Prices prices, Card card) {
    return extractPriceFromPrices(prices, Arrays.asList(card));
  }


  /**
   * 
   * Extract 1x price from model prices
   * 
   * @param prices - model.Prices
   * @param List<Card> - model.Card
   * @return
   */
  public static Float extractPriceFromPrices(Prices prices, List<Card> cards) {
    Float price = null;

    if (!prices.isEmpty()) {
      Map<String, Map<Integer, Double>> cardsInstallments = prices.getInstallmentPrice();
      for (Card card : cards) {
        if (cardsInstallments.containsKey(card.toString())) {
          Map<Integer, Double> installments = cardsInstallments.get(card.toString());
          if (installments.containsKey(1)) {
            Double priceDouble = installments.get(1);
            price = priceDouble.floatValue();
            break;
          }
        }
      }
    }

    return price;
  }

  /**
   * 
   * @param url
   * @param session
   * @return
   */
  public static String crawlFinalUrl(String url, Session session) {
    if (url.equals(session.getRedirectedToURL(url)) || session.getRedirectedToURL(url) == null) {
      return url;
    }

    return session.getRedirectedToURL(url);
  }

  /**
   * 
   * 
   * @param document
   * @param selector
   * @return
   */
  public static CategoryCollection crawlCategories(Document document, String selector) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(selector);

    for (Element e : elementCategories) {
      categories.add(e.text().replace(">", "").trim());
    }

    return categories;
  }

  /**
   * @param document
   * @param selector
   * @param ignoreFirstChild - ignore first element from cssSelector
   * @return
   */
  public static CategoryCollection crawlCategories(Document document, String selector, boolean ignoreFirstChild) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(selector);

    for (int i = ignoreFirstChild ? 1 : 0; i < elementCategories.size(); i++) {
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  /**
   * 
   * @param json
   * @param key
   * @return
   */
  public static Float getFloatValueFromJSON(JSONObject json, String key) {
    Float price = null;

    if (json.has(key)) {
      Object priceObj = json.get(key);

      if (priceObj instanceof Integer) {
        price = MathUtils.normalizeTwoDecimalPlaces(((Integer) priceObj).floatValue());
      } else if (priceObj instanceof Double) {
        price = MathUtils.normalizeTwoDecimalPlaces(((Double) priceObj).floatValue());
      } else {
        String text = priceObj.toString().replaceAll("[^0-9.]", "");

        if (!text.isEmpty()) {
          price = Float.parseFloat(text);
        }
      }
    }

    return price;
  }

  /**
   * 
   * @param json
   * @param key
   * @return
   */
  public static Double getDoubleValueFromJSON(JSONObject json, String key) {
    Double price = null;

    if (json.has(key)) {
      Object priceObj = json.get(key);

      if (priceObj instanceof Integer) {
        price = ((Integer) priceObj).doubleValue();
      } else if (priceObj instanceof Double) {
        price = (Double) priceObj;
      } else {
        String text = priceObj.toString().replaceAll("[^0-9.]", "");

        if (!text.isEmpty()) {
          price = Double.parseDouble(text);
        }
      }
    }

    return price;
  }

  /**
   * Crawl simple installment with this text example:
   * 
   * 2x de R$12,90
   * 
   * @param cssSelector - if null, you must pass the specific element in the html parameter
   * @param html - document html or element html
   * @param ownText - if the returned text of the element is taken from the first child
   * @return Pair<Integer, Float>
   */
  public static Pair<Integer, Float> crawlSimpleInstallment(String cssSelector, Element html, boolean ownText) {
    return crawlSimpleInstallment(cssSelector, html, ownText, "x");
  }

  /**
   * Crawl simple installment with this text example:
   * 
   * 2x de R$12,90
   * 
   * @param cssSelector - if null, you must pass the specific element in the html parameter
   * @param html - document html or element html
   * @param ownText - if the returned text of the element is taken from the first child
   * @return Pair<Integer, Float>
   */
  public static Pair<Integer, Float> crawlSimpleInstallment(String cssSelector, Element html, boolean ownText, String firstDelimiter) {
    return crawlSimpleInstallment(cssSelector, html, ownText, firstDelimiter, "", true);
  }

  /**
   * Crawl simple installment with this text example:
   * 
   * 2 (@param delimiter) R$12,90
   * 
   * @param cssSelector - if null, you must pass the specific element in the html parameter
   * @param html - document html or element html
   * @param ownText - if the returned text of the element is taken from the first child
   * @param delimiter - string to separate a intallment from your value like "12x 101,08 com juros de
   *        (2.8%)" delimiter will be "x"
   * @param lastDelimiter - string to separate a value from text like "12x 101,08 com juros de (2.8%)"
   *        lastDelimiter will be "com"
   * @param lastOccurrenceForLastDelimiter - if lastDelimiter will be last ocurrence on text
   * @return Pair<Integer, Float>
   */
  public static Pair<Integer, Float> crawlSimpleInstallment(String cssSelector, Element html, boolean ownText, String delimiter, String lastDelimiter,
      boolean lastOccurrenceForLastDelimiter) {
    Pair<Integer, Float> pair = new Pair<>();

    Element installment = cssSelector != null ? html.selectFirst(cssSelector) : html;

    if (installment != null) {
      String text = ownText ? installment.ownText().toLowerCase() : installment.text().toLowerCase();
      if (text.contains(delimiter) && text.contains(lastDelimiter)) {
        int x = text.indexOf(delimiter);
        int y;

        if (lastOccurrenceForLastDelimiter) {
          y = text.lastIndexOf(lastDelimiter);
        } else {
          y = text.indexOf(lastDelimiter, x);
        }

        String installmentNumber = text.substring(0, x).replaceAll("[^0-9]", "").trim();
        Float value = MathUtils.parseFloatWithComma(text.substring(x, y));

        if (!installmentNumber.isEmpty() && value != null) {
          pair.set(Integer.parseInt(installmentNumber), value);
        }
      } else if (text.contains("vista")) {
        Float value = MathUtils.parseFloatWithComma(text);

        if (value != null) {
          pair.set(1, value);
        }
      }
    }

    return pair;
  }

  /**
   * Crawls images from a javascript inside the page for Magento markets.
   * 
   * @param doc
   * @return JSONArray
   */
  public static JSONArray crawlArrayImagesFromScriptMagento(Document doc) {
    JSONArray images = new JSONArray();

    JSONObject scriptJson = CrawlerUtils.selectJsonFromHtml(doc, ".product.media script[type=\"text/x-magento-init\"]", null, null, true, false);

    if (scriptJson.has("[data-gallery-role=gallery-placeholder]")) {
      JSONObject mediaJson = scriptJson.getJSONObject("[data-gallery-role=gallery-placeholder]");

      if (mediaJson.has("mage/gallery/gallery")) {
        JSONObject gallery = mediaJson.getJSONObject("mage/gallery/gallery");

        if (gallery.has("data")) {
          JSONArray arrayImages = gallery.getJSONArray("data");

          for (Object o : arrayImages) {
            JSONObject imageJson = (JSONObject) o;

            if (imageJson.has("full")) {
              images.put(imageJson.get("full"));
            } else if (imageJson.has("img")) {
              images.put(imageJson.get("img"));
            } else if (imageJson.has("thumb")) {
              images.put(imageJson.get("thumb"));
            }
          }
        }
      }
    }

    return images;
  }

  /**
   * Get total products of search in crawler Ranking
   * 
   * @param doc
   * @param selector
   * @parm owntext - if true this function will use element.ownText(), if false will be used
   *       element.text()
   * @return default value is 0
   */
  public static Integer scrapTotalProductsForRanking(Document doc, String selector, boolean ownText) {
    Integer total = 0;

    Element totalElement = doc.select(selector).first();

    if (totalElement != null) {
      String text = (ownText ? totalElement.ownText() : totalElement.text()).replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        total = Integer.parseInt(text);
      }
    }

    return total;
  }
}
