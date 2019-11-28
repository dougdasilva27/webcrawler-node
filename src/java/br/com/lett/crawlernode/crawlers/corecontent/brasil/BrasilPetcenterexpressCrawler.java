package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilPetcenterexpressCrawler extends Crawler {

   public BrasilPetcenterexpressCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   protected Object fetch() {
      // We append this parameter for load all coments to capture rating and reviews
      String url = session.getOriginalURL().contains("?") ? session.getOriginalURL() + "&comtodos=s" : session.getOriginalURL() + "?comtodos=s";

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-variation .selecionado [codigo_tamanho]", "codigo_tamanho");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name", true);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product-values .ctrValorMoeda", null, false, ',', session);
         Prices prices = crawlPrices(doc, price);
         boolean available = !doc.select(".product-info .ctrBotaoComprarArea:not(.hidden)").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".maisfotos-foto a", Arrays.asList("urlfoto"), "https:", "cdnv2.moovin.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".maisfotos-foto a", Arrays.asList("urlfoto"), "https:", "cdnv2.moovin.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-description.clearfix", ".product-features"));
         String ean = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-variation .selecionado [ean]", "ean");
         List<String> eans = ean != null && !ean.isEmpty() ? Arrays.asList(ean) : null;
         RatingsReviews rating = scrapRatingReviews(doc);

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
               .setEans(eans)
               .setRatingReviews(rating)
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
         prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product-values .ctrValorVistaMoeda", null, true, ',', session));

         Elements parcels = doc.select(".product-values .parcel");
         for (Element parcel : parcels) {
            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, parcel, false, "x");
            if (!pair.isAnyValueNull()) {
               installments.put(pair.getFirst(), pair.getSecond());
            }
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

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-content").isEmpty();
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = doc.select(".customer-comment .comentarios-score-lista").size();
      Integer totalWrittenReviews = totalNumOfEvaluations;
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
      Double avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".customer-comment .comentarios-score-lista");

      for (Element review : reviews) {
         Integer val = review.select(".cheia").size();

         switch (val) {
            case 1:
               star1 += 1;
               break;
            case 2:
               star2 += 1;
               break;
            case 3:
               star3 += 1;
               break;
            case 4:
               star4 += 1;
               break;
            case 5:
               star5 += 1;
               break;
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
}
