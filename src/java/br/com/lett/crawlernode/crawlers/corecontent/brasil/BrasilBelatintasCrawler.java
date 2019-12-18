package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class BrasilBelatintasCrawler extends Crawler {
   public BrasilBelatintasCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }



   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".produtoInfo-title .fbits-sku", false).replaceAll("[^0-9]", "").trim();
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".produto-comprar a", "id").replaceAll("[^0-9]", "").trim();
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produtoInfo-title h1", false);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#fbits-forma-pagamento #divFormaPagamento .precoPor", null, false, ',', session);
         Float BankTicket = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#fbits-forma-pagamento .precoVista .fbits-boleto-preco", null, false, ',', session);
         Prices prices = crawlPrices(doc, price, BankTicket);
         boolean available = !doc.select("meta[property*=\"product:availability\"]").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#fbits-breadcrumb li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fbits-componente-imagem #zoomImagemProduto", Arrays.asList("src"), "https",
               "www.supermercadodospets.com.br");
         // String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,".product-essential
         // .product-img-box img:not(:last-child)",
         // Arrays.asList("src"), "https", "www.supermercadodospets.com.br", primaryImage);

         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".infoProd .paddingbox"));
         RatingsReviews ratingsReviews = scrapRatingReviews(doc);
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
               // .setSecondaryImages(secondaryImages)
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


   private Prices crawlPrices(Document doc, Float price, Float BankTicket) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installments = new HashMap<>();
         installments.put(1, price);
         prices.setBankTicketPrice(BankTicket);
         prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, "#fbits-forma-pagamento #divFormaPagamento .precoPor", null, true, ',', session));

         // Integer pricess = CrawlerUtils.scrapSimpleInteger(doc, ".fbits-forma-pagamento .precoParcela
         // .fbits-quantidadeParcelas", false);
         // Float valorParcela = price / pricess;
         // System.err.println(valorParcela);

         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".fbits-forma-pagamento .precoParcela .fbits-quantidadeParcelas", doc, true);
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
            // On a html this value will be like this: 100%
            String percentageString = review.attr("style").replaceAll("[^0-9]+", ""); // "100" or ""

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

      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".content.produto").isEmpty();
   }


}
