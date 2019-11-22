package br.com.lett.crawlernode.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.session.Session;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class CrawlerUtils {
   private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
   private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
   private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");

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
    * @param cssSelector - if null will scrap info from doc
    * @param ownText - if must use element.ownText(), if false will be used element.text()
    * @return
    */
   public static String scrapStringSimpleInfo(Element doc, String cssSelector, boolean ownText) {
      String info = null;

      Element infoElement = cssSelector != null ? doc.selectFirst(cssSelector) : doc;
      if (infoElement != null) {
         info = ownText ? infoElement.ownText().trim() : infoElement.text().trim();
      }

      return info;
   }

   /**
    * Scrap simple string from html
    * 
    * Element: <div class="name">
    * 
    * Att: class
    * 
    * Return: name
    * 
    * @param doc
    * @param cssSelector - if null will scrap info from doc
    * @param att - ex: class, id, value, content
    * @return
    */
   public static String scrapStringSimpleInfoByAttribute(Element doc, String cssSelector, String att) {
      String info = null;

      Element infoElement = cssSelector != null ? doc.selectFirst(cssSelector) : doc;
      if (infoElement != null) {
         info = infoElement.hasAttr(att) ? infoElement.attr(att) : null;
      }

      return info;
   }


   /**
    * 
    * Scrap simple price from html
    * 
    * @param doc - html
    * @param cssSelector - cssSelector for scrap info (cssSelector != null ?
    *        doc.selectFirst(cssSelector) : doc)
    * @param att - ex: class, id, value, content
    * @param ownText - if must use element.ownText(), if false will be used element.text()
    * @param priceFormat - '.' for price like this: "2099.0" or ',' for price like this: "2.099,00"
    * @return
    */
   public static Float scrapFloatPriceFromHtml(Element doc, String cssSelector, String att, boolean ownText, char priceFormat, Session session) {
      Float price = null;

      String priceStr = att != null ? scrapStringSimpleInfoByAttribute(doc, cssSelector, att) : scrapStringSimpleInfo(doc, cssSelector, ownText);
      if (priceStr != null) {
         try {
            if (priceFormat == '.') {
               price = MathUtils.parseFloatWithDots(priceStr);
            } else if (priceFormat == ',') {
               price = MathUtils.parseFloatWithComma(priceStr);
            }
         } catch (NumberFormatException e) {
            Logging.printLogWarn(LOGGER, session, CommonMethods.getStackTrace(e));
         }
      }

      return price;
   }

   /**
    * Scrap simple price from text
    * 
    * @param text - string containing price
    * @param priceFormat - '.' for price like this: "2099.0" or ',' for price like this: "2.099,00"
    * @param firstDelimiter
    * @param lastDelimiter
    * @param session
    * @return
    */
   public static Float scrapFloatPriceFromString(String text, char priceFormat, String firstDelimiter, String lastDelimiter, Session session) {
      Float price = null;

      String priceStr = text;

      if (priceStr != null) {

         int first = priceStr.indexOf(firstDelimiter);
         if (first != -1) {
            priceStr = priceStr.substring(first);
         }

         int last = priceStr.indexOf(lastDelimiter);
         if (last != -1) {
            priceStr = priceStr.substring(0, last);
         }

         try {
            if (priceFormat == '.') {
               price = MathUtils.parseFloatWithDots(priceStr);
            } else if (priceFormat == ',') {
               price = MathUtils.parseFloatWithComma(priceStr);
            }
         } catch (NumberFormatException e) {
            Logging.printLogWarn(LOGGER, session, CommonMethods.getStackTrace(e));
         }
      }

      return price;
   }

   /**
    * 
    * Scrap simple price from html
    * 
    * @param doc - html
    * @param cssSelector - cssSelector for scrap info (cssSelector != null ?
    *        doc.selectFirst(cssSelector) : doc)
    * @param att - ex: class, id, value, content
    * @param ownText - if must use element.ownText(), if false will be used element.text()
    * @param priceFormat - '.' for price like this: "2099.0" or ',' for price like this: "2.099,00"
    * @return
    */
   public static Double scrapDoublePriceFromHtml(Element doc, String cssSelector, String att, boolean ownText, char priceFormat, Session session) {
      Float price = scrapFloatPriceFromHtml(doc, cssSelector, att, ownText, priceFormat, session);
      return price != null ? MathUtils.normalizeTwoDecimalPlaces(price.doubleValue()) : null;
   }

   /**
    * Scrap simple price from html
    * 
    * @param document
    * @param cssSelector
    * @param ownText
    * 
    * @deprecated use scrapFloatPriceFromHtml
    * 
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
    * 
    * @deprecated use scrapFloatPriceFromHtml
    * 
    * @return Float
    */
   public static Float scrapSimplePriceFloatWithDots(Element document, String cssSelector, boolean ownText) {
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
    * 
    * @deprecated use scrapDoublePriceFromHtml
    * 
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
    * 
    * @deprecated use scrapDoublePriceFromHtml
    * 
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
    * Scrap simple number integer from html
    * 
    * @param document
    * @param cssSelector
    * @param ownText
    * @return Float
    */
   public static Integer scrapSimpleInteger(Element document, String cssSelector, boolean ownText) {
      Integer number = null;

      Element priceElement = document.selectFirst(cssSelector);
      if (priceElement != null) {
         number = MathUtils.parseInt(ownText ? priceElement.ownText().trim() : priceElement.text().trim());
      }

      return number;
   }

   /**
    * Scrap simple description from html for the first result based on selectors
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
    * Scrap simple description from html for all results based on selectors
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
   public static String scrapSimplePrimaryImage(Element doc, String cssSelector, List<String> attributes, String protocol, String host) {
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
    * Select url from html - Append host and protocol if url needs Scroll through all the attributes in
    * the list sent in sequence to find a url
    * 
    * @param doc - html
    * @param cssSelector - Ex: "a.url"
    * @param attributes - ex: "href", "src"
    * @param protocol - ex: https: or https:// or http: or http://
    * @param host - send host in this format: "www.hostname.com.br"
    * @return Url with protocol and host
    */
   public static String scrapUrl(Element doc, String cssSelector, String attribute, String protocol, String host) {
      return scrapUrl(doc, cssSelector, Arrays.asList(attribute), protocol, host);
   }

   /**
    * Select url from html - Append host and protocol if url needs Scroll through all the attributes in
    * the list sent in sequence to find a url
    * 
    * @param doc - html
    * @param cssSelector - Ex: "a.url"
    * @param attributes - ex: "href", "src"
    * @param protocol - ex: https: or https:// or http: or http://
    * @param host - send host in this format: "www.hostname.com.br"
    * @return Url with protocol and host
    */
   public static String scrapUrl(Element doc, String cssSelector, List<String> attributes, String protocol, String host) {
      String url = null;

      Element urlElement = cssSelector != null ? doc.selectFirst(cssSelector) : doc;
      if (urlElement != null) {
         url = sanitizeUrl(urlElement, attributes, protocol, host);
      }

      return url;
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

      if (url != null) {
         boolean hasHost = url.contains(host);

         // This is necessary when url contains a host different than host on parameters
         // We not use URI always because if url is in this format: www.imgcdn.com/image.png
         // This library don't recognize "www.imgcdn.com" as a host, only if starts with "//"
         if (!hasHost) {
            try {
               URI uri = new URI(url);
               hasHost = uri.getHost() != null;
            } catch (URISyntaxException e) {
               Logging.printLogWarn(LOGGER, CommonMethods.getStackTrace(e));
            }
         }

         if (url.startsWith("../")) {
            url = CommonMethods.getLast(url.split("\\.\\.\\/"));
         }

         if (!protocol.endsWith(":") && !protocol.endsWith("//")) {
            protocol += ":";
         }

         if (!url.startsWith("http") && hasHost) {
            sanitizedUrl.append(protocol.endsWith("//") || url.startsWith("//") ? protocol : protocol + "//").append(url);
         } else if (!hasHost && !url.startsWith("http")) {
            sanitizedUrl.append(protocol.endsWith("//") ? protocol : protocol + "//").append(host)
                  .append(url.startsWith("/") ? url : "/" + url);
         } else {
            sanitizedUrl.append(url);
         }
      } else {
         return null;
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
    * @param cookiesClient
    * @param session - crawler session
    * @return List<Cookie>
    */
   public static List<Cookie> fetchCookiesFromAPage(String url, List<String> cookiesToBeCrawled, String domain, String path,
         List<Cookie> cookiesClient, Session session, DataFetcher dataFetcher) {

      return fetchCookiesFromAPage(url, cookiesToBeCrawled, domain, path, cookiesClient, session, null, dataFetcher);
   }

   /**
    * Crawl cookies from a page
    * 
    * @param url - page where are cookies
    * @param cookiesToBeCrawled - list(string) of cookies to be crawled
    * @param domain - domain to set in cookie
    * @param path - path to set in cookie
    * @param cookiesClient
    * @param session - crawler session
    * @param headers - request headers
    * @return List<Cookie>
    */
   public static List<Cookie> fetchCookiesFromAPage(String url, List<String> cookiesToBeCrawled, String domain, String path,
         List<Cookie> cookiesClient, Session session, Map<String, String> headers, DataFetcher dataFetcher) {
      List<Cookie> cookies = new ArrayList<>();

      Request request = RequestBuilder.create().setCookies(cookiesClient).setUrl(url).setHeaders(headers).build();
      Response response = dataFetcher.get(session, request);

      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         if (cookiesToBeCrawled == null || cookiesToBeCrawled.isEmpty() || cookiesToBeCrawled.contains(cookieResponse.getName())) {
            BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
            cookie.setDomain(domain);
            cookie.setPath(path);
            cookies.add(cookie);
         }
      }

      return cookies;
   }

   /**
    * Crawl cookies from a page
    * 
    * @param url - page where are cookies
    * @param cookiesToBeCrawled - list(string) of cookies to be crawled
    * @param domain - domain to set in cookie
    * @param path - path to set in cookie
    * @param cookiesClient
    * @param session - crawler session
    * @param headers - request headers
    * @return List<Cookie>
    */
   public static List<Cookie> fetchCookiesFromAPage(Request request, String domain, String path, List<String> cookiesToBeCrawled, Session session,
         DataFetcher dataFetcher) {
      List<Cookie> cookies = new ArrayList<>();

      Response response = dataFetcher.get(session, request);

      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         if (cookiesToBeCrawled == null || cookiesToBeCrawled.isEmpty() || cookiesToBeCrawled.contains(cookieResponse.getName())) {
            cookies.add(setCookie(cookieResponse.getName(), cookieResponse.getValue(), domain, path));
         }
      }

      return cookies;
   }



   /**
    * Return a Cookie apache
    * 
    * @param name
    * @param value
    * @param domain
    * @param path
    * @return
    */
   public static Cookie setCookie(String name, String value, String domain, String path) {
      BasicClientCookie cookie = new BasicClientCookie(name, value);
      cookie.setDomain(domain);
      cookie.setPath(path);

      return cookie;
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
            Logging.printLogWarn(LOGGER, session, "Error creating JSONObject from var skuJson_0");
            Logging.printLogWarn(LOGGER, session, CommonMethods.getStackTraceString(e));
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
    * @param lastOccurrenceOfFirstIndex if true, the substring will find the last occurrence of first
    *        index
    * @return JSONArray
    * 
    * @throws JSONException
    * @throws ArrayIndexOutOfBoundsException if finalIndex doesn't exists or there is a duplicate
    * @throws IllegalArgumentException if doc is null
    */
   public static JSONArray selectJsonArrayFromHtml(Document doc, String cssElement, String token, String finalIndex, boolean withoutSpaces,
         boolean lastFinalIndex, boolean lastOccurrenceOfFirstIndex) throws JSONException, ArrayIndexOutOfBoundsException, IllegalArgumentException {

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
            object = stringToJsonArray(extractSpecificStringFromScript(script, token, lastOccurrenceOfFirstIndex, finalIndex, lastFinalIndex));
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
    * @deprecated
    */
   public static String extractSpecificStringFromScript(String script, String token, String finalIndex, boolean lastFinalIndex) {
      return extractSpecificStringFromScript(script, token, false, finalIndex, lastFinalIndex);
   }

   /**
    * Extract Json string from script(string) e.g: vtxctx = [{ skus:"825484", searchTerm:"",
    * categoryId:"38", categoryName:"Leite infantil", departmentyId:"4", departmentName:"Infantil",
    * url:"www.araujo.com.br" }, {...}];
    *
    * token = "vtxctx=" finalIndex = ";"
    * 
    * @param firstIndexString whithout spaces
    * @param lastOccurrenceOfFirstIndex if true, the substring will find last index of firstIndexString
    * @param lastIndexString if final index is null or is'nt in html, substring will use only the token
    *        if lastIndex is "},", "};" or "})" this function will treat caracter "}" as part of json
    * @param lastOccurrenceOfLastIndex if true, the substring will find last index of lastIndexString
    * @return
    */
   public static String extractSpecificStringFromScript(String script, String firstIndexString, boolean lastOccurrenceOfFirstIndex,
         String lastIndexString,
         boolean lastOccurrenceOfLastIndex) {
      String json = null;

      int x = (lastOccurrenceOfFirstIndex ? script.lastIndexOf(firstIndexString) : script.indexOf(firstIndexString)) + firstIndexString.length();

      if (lastIndexString != null) {
         int y;

         if (lastOccurrenceOfLastIndex) {
            y = script.lastIndexOf(lastIndexString);
         } else {
            y = script.indexOf(lastIndexString, x);
         }

         int plusIndex = 0;

         // This happen when we need scrap a specific json on script
         // Sometime we have more than one json
         // So the last index in this case will be "},", "})" or "};"
         if (lastIndexString.equals("};") || lastIndexString.equals("},") || lastIndexString.startsWith("})")) {
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

      if (str.trim().startsWith("{") && str.trim().endsWith("}")) {
         try {
            // We use gson to parse because this library treats "\n" and duplicate keys
            JsonObject gson = ((JsonObject) new JsonParser().parse(str.trim()));
            json = new JSONObject(gson.toString());
         } catch (Exception e1) {
            Logging.printLogWarn(LOGGER, CommonMethods.getStackTrace(e1));
         }
      }

      return json;
   }

   public static JSONObject stringToJSONObject(String str) {
      JSONObject json = new JSONObject();

      if (str.trim().startsWith("{") && str.trim().endsWith("}")) {
         try {
            json = new JSONObject(str.trim());
         } catch (Exception e1) {
            Logging.printLogWarn(LOGGER, CommonMethods.getStackTrace(e1));
         }
      }

      return json;
   }

   public static JSONArray stringToJsonArray(String str) {
      JSONArray json = new JSONArray();

      if (str.trim().startsWith("[") && str.trim().endsWith("]")) {
         try {
            json = new JSONArray(str.trim());
         } catch (Exception e1) {
            Logging.printLogWarn(LOGGER, CommonMethods.getStackTrace(e1));
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
   public static String crawlDescriptionFromFlixMedia(String storeId, String ean, DataFetcher dataFetcher, Session session) {
      StringBuilder description = new StringBuilder();

      String url =
            "https://media.flixcar.com/delivery/js/inpage/" + storeId + "/br/ean/" + ean + "?&=" + storeId + "&=br&ean=" + ean + "&ssl=1&ext=.js";

      Response response = dataFetcher.get(session, RequestBuilder.create().setUrl(url).build());

      String script = response.getBody();
      final String token = "$(\"#flixinpage_\"+i).inPage";

      JSONObject productInfo = new JSONObject();

      if (script.contains(token)) {
         int x = script.indexOf(token + " (") + token.length() + 2;
         int y = script.indexOf(");", x);

         String json = script.substring(x, y);

         try {
            productInfo = new JSONObject(json);
         } catch (JSONException e) {
            Logging.printLogWarn(LOGGER, session, CommonMethods.getStackTrace(e));
         }
      }

      if (productInfo.has("product")) {
         String id = productInfo.getString("product");

         String urlDesc = "https://media.flixcar.com/delivery/inpage/show/" + storeId + "/br/" + id + "/json?c=jsonpcar" + storeId + "r" + id
               + "&complimentary=0&type=.html";

         Response response2 = dataFetcher.get(session, RequestBuilder.create().setUrl(urlDesc).build());

         String scriptDesc = response2.getBody();

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
               Logging.printLogWarn(LOGGER, session, CommonMethods.getStackTrace(e));
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
                  Logging.printLogWarn(LOGGER, session, Util.getStackTraceString(e));
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
   public static String getRedirectedUrl(String url, Session session) {
      String redirectedUrl = session.getRedirectedToURL(url);
      if (redirectedUrl != null && !url.equalsIgnoreCase(redirectedUrl)) {
         return redirectedUrl;
      }

      return url;
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

   public static Float getFloatValueFromJSON(JSONObject json, String key) {
      return getFloatValueFromJSON(json, key, true, null);
   }

   /**
    * 
    * @param json
    * @param key
    * @param stringWithFloatLayout -> if price string is a float in a string format like "23.99"
    * @param priceWithComma -> e.g: R$ 2.779,20 returns the Float 2779.2
    * @return
    */
   public static Float getFloatValueFromJSON(JSONObject json, String key, boolean stringWithFloatLayout, Boolean priceWithComma) {
      Float price = null;

      if (json.has(key)) {
         Object priceObj = json.get(key);

         if (priceObj instanceof Integer) {
            price = MathUtils.normalizeTwoDecimalPlaces(((Integer) priceObj).floatValue());
         } else if (priceObj instanceof Double) {
            price = MathUtils.normalizeTwoDecimalPlaces(((Double) priceObj).floatValue());
         } else {
            if (stringWithFloatLayout) {
               price = MathUtils.parseFloatWithDots(priceObj.toString());
            } else if (priceWithComma) {
               price = MathUtils.parseFloatWithComma(priceObj.toString());
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
    * @deprecated use {@link getDoubleValueFromJSON}
    */
   public static Double getDoubleValueFromJSON(JSONObject json, String key) {
      return getDoubleValueFromJSON(json, key, true, null);
   }

   /**
    * 
    * @param json
    * @param key
    * @param stringWithDoubleLayout -> if price string is a double in a string format like "23.99"
    * @param doubleWithComma -> e.g: R$ 2.779,20 returns the Double 2779.2
    * @return
    */
   public static Double getDoubleValueFromJSON(JSONObject json, String key, boolean stringWithDoubleLayout, Boolean doubleWithComma) {
      Double doubleValue = null;

      if (json.has(key)) {
         Object obj = json.get(key);

         if (obj instanceof Integer) {
            doubleValue = ((Integer) obj).doubleValue();
         } else if (obj instanceof Double) {
            doubleValue = (Double) obj;
         } else {

            if (stringWithDoubleLayout) {
               String text = obj.toString().replaceAll("[^0-9.]", "");

               if (!text.isEmpty()) {
                  doubleValue = Double.parseDouble(text);
               }
            } else {

               if (doubleWithComma) {
                  doubleValue = MathUtils.parseDoubleWithComma(obj.toString());
               } else {
                  doubleValue = MathUtils.parseDoubleWithDot(obj.toString());
               }
            }
         }
      }

      return doubleValue;
   }

   /**
    * 
    * @param json
    * @param key
    * @param defaultValue - return this value if key not exists
    * @return
    */
   public static Integer getIntegerValueFromJSON(JSONObject json, String key, Integer defaultValue) {
      Integer value = defaultValue;

      if (json.has(key)) {
         Object valueObj = json.get(key);

         if (valueObj instanceof Integer) {
            value = (Integer) valueObj;
         } else {
            String text = valueObj.toString().replaceAll("[^0-9]", "");

            if (!text.isEmpty()) {
               value = Integer.parseInt(text);
            }
         }
      }

      return value;
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
    * @deprecated Because of non possibility to parse price with ',' or '.'
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

      return crawlSimpleInstallment(cssSelector, html, ownText, delimiter, lastDelimiter, lastOccurrenceForLastDelimiter, '.');
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
    * @param priceFormat - '.' for price like this: "2099.0" or ',' for price like this: "2.099,00"
    * @return Pair<Integer, Float>
    */
   public static Pair<Integer, Float> crawlSimpleInstallment(String cssSelector, Element html, boolean ownText, String delimiter, String lastDelimiter,
         boolean lastOccurrenceForLastDelimiter, char priceFormat) {
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
            Float value = null;

            if (priceFormat == '.') {
               value = MathUtils.parseFloatWithDots(text.substring(x, y));
            } else if (priceFormat == ',') {
               value = MathUtils.parseFloatWithComma(text.substring(x, y));
            }

            if (!installmentNumber.isEmpty() && value != null) {
               pair.set(Integer.parseInt(installmentNumber), value);
            }
         } else if (text.contains("vista")) {
            Float value = null;

            if (priceFormat == '.') {
               value = MathUtils.parseFloatWithDots(text);
            } else if (priceFormat == ',') {
               value = MathUtils.parseFloatWithComma(text);
            }

            if (value != null) {
               pair.set(1, value);
            }
         }
      }

      return pair;
   }

   /**
    * Crawl simple installment with this text example:
    * 
    * 2 (@param delimiter) R$12,90
    * 
    * @param text string to search
    * @param delimiter - string to separate a intallment from your value like "12x 101,08 com juros de
    *        (2.8%)" delimiter will be "x"
    * @param lastDelimiter - string to separate a value from text like "12x 101,08 com juros de (2.8%)"
    *        lastDelimiter will be "com"
    * @param lastOccurrenceForLastDelimiter - if lastDelimiter will be last ocurrence on text
    * @return Pair<Integer, Float>
    */
   public static Pair<Integer, Float> crawlSimpleInstallmentFromString(String text, String delimiter, String lastDelimiter,
         boolean lastOccurrenceForLastDelimiter) {
      Pair<Integer, Float> pair = new Pair<>();

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

         JSONObject gallery = new JSONObject();
         if (mediaJson.has("mage/gallery/gallery")) {
            gallery = mediaJson.getJSONObject("mage/gallery/gallery");
         } else if (mediaJson.has("Xumulus_FastGalleryLoad/js/gallery/custom_gallery")) {
            gallery = mediaJson.getJSONObject("Xumulus_FastGalleryLoad/js/gallery/custom_gallery");
         }

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
    * @deprecated
    */
   public static Integer scrapIntegerFromHtml(Element doc, String selector, boolean ownText) {
      Integer total = 0;

      Element totalElement = selector != null ? doc.selectFirst(selector) : doc;

      if (totalElement != null) {
         String text = (ownText ? totalElement.ownText() : totalElement.text()).replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            total = Integer.parseInt(text);
         }
      }

      return total;
   }

   /**
    * 
    * @param doc
    * @param selector
    * @param ownText - if true this function will use element.ownText(), if false will be used
    *        element.text()
    * @param defaultValue - return value if condition == null
    * @return
    */
   public static Integer scrapIntegerFromHtml(Element doc, String selector, boolean ownText, Integer defaultValue) {
      Integer total = defaultValue;

      Element totalElement = selector != null ? doc.selectFirst(selector) : doc;

      if (totalElement != null) {
         String text = (ownText ? totalElement.ownText() : totalElement.text()).replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            total = Integer.parseInt(text);
         }
      }

      return total;
   }

   /**
    * Utility method to extract integer from from a html with is contained on a attribute of a html
    * tag. <br>
    * <br>
    * Ex: <br>
    * < tag value="37"/> <br>
    * Extracts: 37
    * 
    * @param doc
    * @param selector
    * @param attr - attribute to search
    * @param defaultValue - return value if condition == null
    * @return
    */
   public static Integer scrapIntegerFromHtmlAttr(Element doc, String selector, String attr, Integer defaultValue) {
      Integer total = defaultValue;

      Element totalElement = selector != null ? doc.selectFirst(selector) : doc;

      if (totalElement != null && totalElement.hasAttr(attr)) {
         String text = totalElement.attr(attr).replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            total = Integer.parseInt(text);
         }
      }

      return total;
   }

   /**
    * 
    * @param doc
    * @param selector
    * @param ownText - if true this function will use element.ownText(), if false will be used
    *        element.text()
    * @param defaultValue - return value if condition == null
    * @return
    * @deprecated
    */
   public static Integer scrapIntegerFromHtml(Element doc, String selector, String delimiter, boolean ownText, Integer defaultValue) {
      Integer total = defaultValue;

      Element totalElement = selector != null ? doc.selectFirst(selector) : doc;

      if (totalElement != null) {
         String text = (ownText ? totalElement.ownText() : totalElement.text());

         if (delimiter != null && text.contains(delimiter)) {
            int x = text.indexOf(delimiter);
            text = text.substring(0, x).replaceAll("[^0-9]", "").trim();
         }

         if (!text.isEmpty()) {
            total = Integer.parseInt(text);
         }
      }

      return total;
   }

   /**
    * 
    * @param doc
    * @param selector
    * @param ownText - if true this function will use element.ownText(), if false will be used
    *        element.text()
    * @param defaultValue - return value if condition == null
    * @return
    */
   public static Integer scrapIntegerFromHtml(Element doc, String selector, String firstDelimiter, String lastDelimiter,
         boolean lastOccurrenceOfLastDelimiter, boolean ownText, Integer defaultValue) {
      Integer total = defaultValue;

      Element totalElement = selector != null ? doc.selectFirst(selector) : doc;

      if (totalElement != null) {
         String text = (ownText ? totalElement.ownText() : totalElement.text());

         if (firstDelimiter != null && text.contains(firstDelimiter)) {
            if (lastDelimiter != null && text.contains(lastDelimiter)) {
               int x = text.indexOf(firstDelimiter);
               int y = lastOccurrenceOfLastDelimiter ? text.lastIndexOf(lastDelimiter) : text.indexOf(lastDelimiter, x);
               text = text.substring(x, y).replaceAll("[^0-9]", "").trim();
            } else {
               int x = text.indexOf(firstDelimiter);
               text = text.substring(x).replaceAll("[^0-9]", "").trim();
            }
         } else if (lastDelimiter != null && text.contains(lastDelimiter)) {
            int x = lastOccurrenceOfLastDelimiter ? text.lastIndexOf(lastDelimiter) : text.indexOf(lastDelimiter);
            text = text.substring(0, x).replaceAll("[^0-9]", "").trim();
         } else {
            text = text.replaceAll("[^0-9]", "");
         }

         if (!text.isEmpty()) {
            total = Integer.parseInt(text);
         }
      }

      return total;
   }

   /**
    * Get JSONArray wich contains the EAN data.
    * 
    * @param doc document to be searched
    * @return JSONArray object
    */
   public static JSONArray scrapEanFromVTEX(Document doc) {
      JSONArray arr = new JSONArray();

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "vtex.events.addData(", ");", true, false);

      if (json.has("productEans")) {
         arr = json.getJSONArray("productEans");
      }

      return arr;
   }


   /**
    * This function scrap standout html for markets descriptions
    * 
    * @param slugMarket - Ex: gpa, ikesaki (you need analyse the site to find this slug)
    * @param session
    * @param cookies
    * @return
    */
   public static String scrapStandoutDescription(String slugMarket, Session session, List<Cookie> cookies, DataFetcher dataFetcher) {
      StringBuilder str = new StringBuilder();

      String url = "https://standout.com.br/" + slugMarket + "/catchtag.php?distributor=" + slugMarket + "sku=&url=" + session.getOriginalURL();

      Response response = dataFetcher.get(session, RequestBuilder.create().setUrl(url).setCookies(cookies).build());
      JSONObject specialDesc = CrawlerUtils.stringToJson(response.getBody());

      if (specialDesc.has("div")) {
         Element e = Jsoup.parse(specialDesc.get("div").toString()).selectFirst("[id^=standout]");

         if (e != null) {
            StringBuilder descriptionUrl = new StringBuilder();
            descriptionUrl.append("https://www.standout.com.br/");
            descriptionUrl.append(e.attr("i")).append("/");
            descriptionUrl.append("p/").append("WmZEhtkrUJQ,/");
            descriptionUrl.append(e.attr("x")).append("/");
            descriptionUrl.append(e.attr("y"));

            Response response2 = dataFetcher.get(session, RequestBuilder.create().setUrl(descriptionUrl.toString()).setCookies(cookies).build());
            str.append(response2.getBody());
         }
      }

      return str.toString();
   }

   /**
    * This function scrap our html on ecommerce's
    * 
    * Using fetcher because there is no need of use proxy.
    * 
    * @param internalId
    * @param session
    * @return
    */
   public static Document scrapLettHtml(String internalId, Session session, Integer marketId) {
      Document doc = new Document("");
      DataFetcher dataFetcher = new FetcherDataFetcher();

      String url = "https://api.lettcdn.com/api/v3/" + marketId.toString() + "/skumap.json";
      FetcherOptions options = new FetcherOptions();
      options.setMustUseMovingAverage(false);
      options.setRetrieveStatistics(true);

      Request request = RequestBuilder.create().setUrl(url).setProxyservice(Arrays.asList(ProxyCollection.NO_PROXY)).setFetcheroptions(options).build();

      JSONObject skuMap = CrawlerUtils.stringToJson(dataFetcher.get(session, request).getBody());
      if (skuMap.has(internalId)) {
         Request requestSkuMap = RequestBuilder.create().setUrl(skuMap.get(internalId).toString())
               .setProxyservice(Arrays.asList(ProxyCollection.NO_PROXY)).setFetcheroptions(options).build();
         doc = Jsoup.parse(dataFetcher.get(session, requestSkuMap).getBody());
      }

      return doc;
   }

   public static String toSlug(String input) {
      String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
      String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
      String slug = NONLATIN.matcher(normalized).replaceAll("");
      slug = EDGESDHASHES.matcher(slug).replaceAll("");
      return slug.toLowerCase(Locale.ENGLISH);
   }
}