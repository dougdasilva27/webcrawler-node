package br.com.lett.crawlernode.crawlers.corecontent.chile;


import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date: 04/12/2018
 *
 * @author Gabriel Dornelas
 */
public class ChileLidersuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.lider.cl/supermercado/";

   public ChileLidersuperCrawler(Session session) {
      super(session);
      super.config.setParser(br.com.lett.crawlernode.core.models.Parser.HTML);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String url = "https://www.lider.cl/supermercado";
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("authority", "www.pedidosya.com.ar");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().setForbiddenCssSelector("#px-captcha").build())
         .setSendUserAgent(false)
         .build();
      Response response = this.dataFetcher.get(session, request);

      this.cookies = response.getCookies();
   }


   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Response fetchResponse() {

      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "www.lider.cl");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setSendUserAgent(true)
         .mustSendContentEncoding(true)
         .build();

      return new JsoupDataFetcher().get(session, request);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=productID]", true);
         String name = scrapName(doc);
         Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "#productPrice .price", true);
         Prices prices = crawlPrices(price);
         Integer stock = crawlStock(internalId);
         boolean available = stock > 0;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a > span");
         List<String> images = crawlImages(internalId);
         String primaryImage = !images.isEmpty() ? images.get(0) : null;
         String secondaryImages = crawlSecondaryImages(images);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-features"));

         JSONObject jsonEan = selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]");
         List<String> eans = scrapEans(jsonEan);
         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setPrice(price)
            .setPrices(prices)
            .setAvailable(available)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private List<String> scrapEans(JSONObject jsonEan) {
      List<String> eans = new ArrayList<>();

      if (jsonEan.has("gtin13")) {
         eans.add(jsonEan.getString("gtin13"));
      }

      return eans;
   }

   private JSONObject selectJsonFromHtml(Document doc, String cssSelector) {
      Element element = doc.selectFirst(cssSelector);
      JSONObject json = new JSONObject();

      if (element != null) {
         String strJson = sanitizeStringJson(element.html());
         json = CrawlerUtils.stringToJson(strJson);
      }

      return json;
   }

   private String sanitizeStringJson(String text) {
      String result = null;
      String substring = null;

      if (text.contains("/*") && text.contains("*/")) {
         substring = text.substring(text.indexOf("/*"), text.indexOf("*/") + 2);
         result = text.replace(substring, "");
      }

      return result;
   }

   private boolean isProductPage(Document doc) {
      return doc.select(".product-info") != null && doc.selectFirst(".product-info .no-available") == null;
   }

   private String scrapName(Document doc) {
      StringBuilder name = new StringBuilder();

      Elements names = doc.select(".product-info h1 span, .product-profile h1 span");
      for (Element e : names) {
         name.append(e.ownText().trim()).append(" ");
      }

      return name.toString().trim();
   }

   private List<String> crawlImages(String id) {
      List<String> images = new ArrayList<>();

      Request request = RequestBuilder.create().setUrl("https://wlmstatic.lider.cl/contentassets/galleries/" + id + ".xml").setCookies(cookies).build();
      Document docXml = Jsoup.parse(this.dataFetcher.get(session, request).getBody(), "", Parser.xmlParser());

      Elements items = docXml.getElementsByTag("image");
      for (Element e : items) {
         String image = e.text();

         if (image.contains("file:/")) {

            String format = "";

            if (image.contains(".jpg")) {
               format = "=format[jpg]";
            }

            images.add("http://images.lider.cl/wmtcl?source=url[" + image + "]&sink" + format);
         }
      }

      return images;
   }

   private String crawlSecondaryImages(List<String> images) {
      String secondaryImages = null;

      if (images.size() > 1) {
         JSONArray imagesArray = new JSONArray();

         for (int i = 1; i < images.size(); i++) {
            imagesArray.put(images.get(i));
         }

         if (imagesArray.length() > 0) {
            secondaryImages = imagesArray.toString();
         }
      }

      return secondaryImages;
   }

   private Integer crawlStock(String id) {
      Integer stock = 0;

      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "application/json, text/javascript, */*; q=0.01");
      headers.put("authority", "www.lider.cl");
      headers.put("cookie", CommonMethods.cookiesToString(cookies));
      headers.put("referer", session.getOriginalURL());
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      String payload = "productNumber=" + id + "&useProfile=true&consolidate=true";

      Request request = RequestBuilder.create()
         .setUrl(
            "https://www.lider.cl/supermercado/includes/inventory/inventoryInformation.jsp")
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setPayload(payload)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      JSONArray array = CrawlerUtils.stringToJsonArray(response.getBody());

      if (array.length() > 0) {
         JSONObject skuJson = array.getJSONObject(0);

         if (skuJson.has("stockLevel")) {
            String text = skuJson.get("stockLevel").toString().replaceAll("[^0-9]", "");

            if (!text.isEmpty()) {
               stock = Integer.parseInt(text);
            }
         }
      }

      return stock;
   }

   /**
    * In the time when this crawler was made, this market hasn't installments informations
    *
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();
         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      }

      return prices;
   }

}
