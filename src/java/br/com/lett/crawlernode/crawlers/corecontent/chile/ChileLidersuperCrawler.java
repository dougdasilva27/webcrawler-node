package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 04/12/2018
 *
 * @author Gabriel Dornelas
 */
public class ChileLidersuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.lider.cl/supermercado/";

   public ChileLidersuperCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=productID]", true);
         String name = scrapName(doc);
         Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "#productPrice .price", true);
         Prices prices = crawlPrices(price, doc);
         Integer stock = crawlStock(internalId);
         boolean available = stock > 0;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a > span");
         List<String> images = crawlImages(internalId);
         String primaryImage = !images.isEmpty() ? images.get(0) : null;
         String secondaryImages = crawlSecondaryImages(images);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-features"));
         RatingsReviews ratingReviews = null;

         JSONObject jsonEan = selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]");
         List<String> eans = scrapEans(jsonEan);
         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(new Marketplace()).setRatingReviews(ratingReviews).setEans(eans).build();

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
            images.add("http://images.lider.cl/wmtcl?source=url[" + image + "]&sink");
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

      Request request = RequestBuilder.create()
         .setUrl(
            "https://www.lider.cl/supermercado/includes/inventory/inventoryInformation.jsp?productNumber=" + id + "&useProfile=true&consolidate=true")
         .setCookies(cookies).build();
      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

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
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Document doc) {
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
