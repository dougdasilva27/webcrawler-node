package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilNutrineCrawler extends Crawler {
   private static final String HOME_PAGE = "https://www.nutrine.com.br/";

   public BrasilNutrineCrawler(Session session) {
      super(session);
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

         JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);

         String internalId = jsonInfo.has("sku") ? jsonInfo.get("sku").toString() : null;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".fixed-info .product-name", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".PrecoPrincipal input", "value", false, '.', session);
         Prices prices = crawlPrices(doc, price);
         boolean available = !doc.select(".botao-comprar").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".f-wrap .breadcrumb-item", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".zoom img", Arrays.asList("src"), "https",
               "www.nutrine.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
               ".item:not(:first-child) .box-img img",
               Arrays.asList("src"), "https", "www.nutrine.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".page-info-product"));
         RatingsReviews ratingsReviews = scrapRatingReviews(doc);
         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
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
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }



   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installments = new HashMap<>();
         installments.put(1, price);
         prices.setBankTicketPrice(price);
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PrecoPrincipal input", "value", true, '.', session));

         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment("#produto_preco #info_preco", doc, false, "x", "Sem", false, '.');
         if (!pair.isAnyValueNull()) {
            installments.put(pair.getFirst(), pair.getSecond());
         }

         prices.insertCardInstallment(Card.VISA.toString(), installments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installments);
         prices.insertCardInstallment(Card.DINERS.toString(), installments);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installments);
         prices.insertCardInstallment(Card.AMEX.toString(), installments);
         prices.insertCardInstallment(Card.ELO.toString(), installments);
      }

      return prices;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".product-essential .ratings .rating-links a", false, 0);
      Double avgRating = scrapAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalComments);
      ratingReviews.setTotalWrittenReviews(totalComments);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double scrapAvgRating(Document doc) {
      Double avg = 0d;

      Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".row .product-shop [itemprop=ratingValue]", null, false, ',', session);
      if (percentage != null) {
         avg = (percentage / 100) * 5;
      }

      return avg;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select("#tab_review_tabbed_contents #product-customer-reviews .review-area .ratings-list .rating-item .rating-box .rating");

      for (Element review : reviews) {
         if (review != null && review.hasAttr("style")) {

            String percentageString = review.attr("style").replaceAll("[^0-9]+", ""); // "100" or ""

            if (percentageString != null) {

               Integer val = !percentageString.isEmpty() ? Integer.parseInt(percentageString) : 0;

               switch (val) {
                  case 20:
                     star1 += 1;
                     break;
                  case 40:
                     star2 += 1;
                     break;
                  case 60:
                     star3 += 1;
                     break;
                  case 80:
                     star4 += 1;
                     break;
                  case 100:
                     star5 += 1;
                     break;
                  default:
                     break;
               }
            }
         }
      }

      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("#product-container").isEmpty();
   }
}