package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 16/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilTerabyteCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.terabyteshop.com.br/";

   public BrasilTerabyteCrawler(Session session) {
      super(session);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "span[data-produto]", "data-produto");
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.tit-prod", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#body .val-prod, #body .p3", null, true, ',', session);
         Prices prices = crawlPrices(doc, price);
         Element availableElement = doc.selectFirst("#indisponivel");
         boolean available = availableElement == null && price != null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li a strong");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fotorama img", Arrays.asList("src"), "https:", "www.terabyteshop.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".fotorama img", Arrays.asList("src"), "https:",
               "www.terabyteshop.com.br", primaryImage);
         String description = crawlDescription(doc);
         RatingsReviews ratingReviews = crawRating(doc);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrice(price)
               .setPrices(prices)
               .setAvailable(available)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setMarketplace(new Marketplace())
               .setRatingReviews(ratingReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select("#partnumber").isEmpty();
   }

   private static String crawlInternalPid(Document doc) {
      String inernalPid = null;

      Element infoElement = doc.selectFirst("#partnumber");
      if (infoElement != null) {
         String text = infoElement.ownText();

         if (text.contains(":")) {
            inernalPid = CommonMethods.getLast(text.split(":")).trim();
         } else {
            inernalPid = CommonMethods.getLast(text.trim().split(" ")).trim();
         }
      }

      return inernalPid;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Elements selos = doc.select(".boxs .box-selo");
      for (Element e : selos) {
         if (e.select("> a").isEmpty()) {
            description.append(e.outerHtml());
         }
      }

      description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".especificacoes .descricao", ".especificacoes .tecnicas")));

      return description.toString();
   }

   /**
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);
         prices.setBankTicketPrice(price);



         Elements parcels = doc.select("#verparcelamento ul li");
         for (Element e : parcels) {
            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x");
            if (!pair.isAnyValueNull()) {
               installmentPriceMap.put(pair.getFirst(), pair.getSecond());
            }
         }

         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      }

      return prices;
   }


   private RatingsReviews crawRating(Document doc) {

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject productInfo = crawlProductInfo(doc);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(productInfo);
      Double avgRating = getTotalAvgRating(productInfo);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;

   }

   private Double getTotalAvgRating(JSONObject ratingJson) {
      Double avgRating = 0d;

      if (ratingJson.has("ratingValue")) {
         avgRating = MathUtils.parseDoubleWithDot(ratingJson.get("ratingValue").toString());

         if (avgRating == null) {
            avgRating = 0d;
         }
      }

      return avgRating;
   }

   private Integer getTotalNumOfRatings(JSONObject ratingJson) {
      Integer totalRating = 0;

      if (ratingJson.has("reviewCount")) {
         String text = ratingJson.get("reviewCount").toString().replaceAll("[^0-9]", "");

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }

   private JSONObject crawlProductInfo(Document doc) {
      JSONObject obj = new JSONObject();

      Elements scripts = doc.select("script");
      for (Element e : scripts) {
         String script = e.html().replace(" ", "");

         if (script.contains("aggregateRating")) {
            obj = CrawlerUtils.stringToJson(script);
         }
      }

      if (obj.has("aggregateRating")) {
         return obj.getJSONObject("aggregateRating");
      }

      return obj;
   }

}
