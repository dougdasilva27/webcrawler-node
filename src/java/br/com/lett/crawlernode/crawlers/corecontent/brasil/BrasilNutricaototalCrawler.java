package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilNutricaototalCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.nutricaototal.com.br/";

   public BrasilNutricaototalCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-essential .no-display input[name=product]", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-essential .product-shop .sku", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-essential .product-name h1", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li[class^=category]");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#productImage", Arrays.asList("src"), "https", "www.nutricaototal.com.br");
         String secondaryImages =
               CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-img-box .more-views [id=additional-carousel] .slider-item a[href=\"#image\"]",
                     Arrays.asList("data-rel"), "https", "www.nutricaototal.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-essential .short-description .std",
               "#product_tabs_description_tabbed_contents", "#product_tabs_additional_tabbed_contents"));
         Integer stock = null;
         Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".product-essential .regular-price .price", true);
         Prices prices = crawlPrices(price, doc);
         boolean available = checkAvaliability(doc, ".add-to-box .add-to-cart");
         RatingsReviews ratingReviews = crawlRating(doc, internalId);

         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
               .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setRatingReviews(ratingReviews).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-essential") != null;
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = crawlNumOfEvaluations(doc, "meta[itemprop=ratingCount]");
      Double avgRating = crawlAvgRating(doc, "meta[itemprop=ratingValue]");

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }

   private Integer crawlNumOfEvaluations(Document doc, String selector) {
      Element e = doc.selectFirst(selector);

      if (e != null) {
         String aux = e.attr("content");

         if (!aux.isEmpty()) {
            return Integer.parseInt(aux);
         }
      }

      return 0;
   }

   private Double crawlAvgRating(Document doc, String selector) {
      Element e = doc.selectFirst(selector);

      if (e != null) {
         String aux = e.attr("content");

         if (!aux.isEmpty()) {
            return Double.parseDouble(aux);
         }
      }

      return 0.0;
   }

   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         Element priceFrom = doc.select(".product-essential .regular-price strong").first();
         if (priceFrom != null) {
            prices.setBankTicketPrice(MathUtils.parseDoubleWithComma(priceFrom.text()));
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      return prices;
   }

   private boolean checkAvaliability(Document doc, String selector) {
      return doc.selectFirst(selector) != null;
   }
}
