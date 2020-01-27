package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilFarmadeliveryCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.farmadelivery.com.br/";

   public BrasilFarmadeliveryCrawler(Session session) {
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

      if (isProductPage(this.session.getOriginalURL(), doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         // ID interno
         String internalId = null;
         Element elementID = doc.select("input[name=product]").first();
         if (elementID != null) {
            internalId = Integer.toString(Integer.parseInt(elementID.val()));
         }

         // Pid
         String internalPid = null;
         Elements elementInternalPid = doc.select("#product-attribute-specs-table tr.sku td.data");
         if (elementInternalPid != null && elementInternalPid.size() > 1) {
            internalPid = elementInternalPid.get(0).text().trim();
         }

         // Nome
         Element elementName = doc.select(".product-name h1").first();
         String name = elementName.text().replace("'", "").replace("’", "").trim();

         Float price = crawlPrice(doc);

         // Disponibilidade
         boolean available = true;
         Element buttonUnavailable = doc.select("a.btn-esgotado").first();
         if (buttonUnavailable != null) {
            available = false;
         }

         // Categorias
         String category1 = "";
         String category2 = "";
         String category3 = "";
         ArrayList<String> categories = new ArrayList<>();
         Elements elementCategories = doc.select(".breadcrumbs ul li");

         for (Element e : elementCategories) {
            if (!e.attr("class").equals("home") && !e.attr("class").equals("product")) {
               categories.add(e.select("a").text());
            }
         }

         for (String category : categories) {
            if (category1.isEmpty()) {
               category1 = category;
            } else if (category2.isEmpty()) {
               category2 = category;
            } else if (category3.isEmpty()) {
               category3 = category;
            }
         }

         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         String description = scrapDescription(doc, internalId);
         Integer stock = null;
         Prices prices = crawlPrices(doc, price);
         Marketplace marketplace = new Marketplace();
         String ean = crawlEan(doc);
         RatingsReviews ratingReviews = crawRating(doc);
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         Product product = new Product();
         product.setUrl(this.session.getOriginalURL());
         product.setInternalId(internalId);
         product.setInternalPid(internalPid);
         product.setName(name);
         product.setPrice(price);
         product.setPrices(prices);
         product.setCategory1(category1);
         product.setCategory2(category2);
         product.setCategory3(category3);
         product.setPrimaryImage(primaryImage);
         product.setSecondaryImages(secondaryImages);
         product.setDescription(description);
         product.setStock(stock);
         product.setMarketplace(marketplace);
         product.setAvailable(available);
         product.setEans(eans);
         product.setRatingReviews(ratingReviews);

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(String url, Document doc) {
      Element elementProduct = doc.select("div.product-view").first();
      return elementProduct != null && !url.contains("/review/");
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element elementPrimaryImage = doc.select(".inner-container-productimage img").first();
      if (elementPrimaryImage != null) {
         primaryImage = elementPrimaryImage.attr("data-zoom-image").trim();

         if (primaryImage.isEmpty() || primaryImage.contains("banner_produto_sem_imagem")) {
            primaryImage = elementPrimaryImage.attr("src").trim();
         }
      }

      if (primaryImage != null && primaryImage.contains("banner_produto_sem_imagem")) {
         primaryImage = null;
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;

      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select(".more-views li a:not(.ver-video)");// Ignore video if there exists

      for (int i = 1; i < images.size(); i++) {
         Element e = images.get(i);
         String image = e.attr("data-zoom-image").trim();

         if (image.isEmpty() || image.equals("#")) {
            image = e.attr("data-image").trim();
         }

         secondaryImagesArray.put(image);
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private Float crawlPrice(Document doc) {
      Float price = null;
      Element elementPrice = doc.select(".product-shop .price-box .regular-price .price").first();
      if (elementPrice == null) {
         elementPrice = doc.select(".product-shop .price-box .special-price .price").first();
      }

      Element elementSpecialPrice = doc.select(".product-shop .pagamento .boleto span").first();

      if (elementPrice != null) {
         price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      } else if (elementSpecialPrice != null) {
         price = MathUtils.parseFloatWithComma(elementSpecialPrice.ownText());
      }

      return price;
   }

   /**
    * In product page only has bank slip price and showcase price
    * 
    * @param document
    * @return
    */
   private Prices crawlPrices(Document document, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Element bankSlip = document.select(".pagamento .boleto > span").first();

         if (bankSlip != null) {
            Float bankSlipPrice = MathUtils.parseFloatWithComma(bankSlip.text());
            prices.setBankTicketPrice(bankSlipPrice);
         }

         Element priceFrom = document.select(".old-price span[id]").first();
         if (priceFrom != null) {
            prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
         }

         Map<Integer, Float> installmentPriceMap = new TreeMap<>();

         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      return prices;
   }

   private String crawlEan(Document doc) {
      String ean = null;
      Element e = doc.selectFirst(".box-collateral.box-additional .codigo_barras .data");

      if (e != null) {
         ean = e.text().trim();
      }

      return ean;
   }

   private String scrapDescription(Document doc, String internalId) {
      StringBuilder description = new StringBuilder();

      Element elementDescription = doc.selectFirst("div.product-collateral .box-description");
      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      Element elementAdditional = doc.selectFirst("div.product-collateral .box-additional");
      if (elementAdditional != null) {
         description.append(elementAdditional.html());
      }

      description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

      return description.toString();
   }

   RatingsReviews crawRating(Document document) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());


      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(document);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document doc, Integer ratingCount) {
      Double avgRating = 0d;

      if (ratingCount > 0) {
         Element avg = doc.select(".rating-box-product .rating-box .rating").first();

         if (avg != null) {
            Double percentage = MathUtils.normalizeTwoDecimalPlaces(Double.parseDouble(avg.attr("style").replaceAll("[^0-9.]", "").trim()));

            if (percentage != null) {
               avgRating = MathUtils.normalizeTwoDecimalPlaces(5 * (percentage / 100d));
            }
         }
      }

      return avgRating;
   }

   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = 0;
      Element totalRatingElement = docRating.select(".rating-box-product .amount a").first();

      if (totalRatingElement != null) {
         String text = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select("#customer-reviews .review-item .ratings-table .rating-box");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".rating");

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("style");
            String sN = stringStarNumber.replaceAll("[^0-9]", "").trim();
            Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0;

            switch (numberOfStars) {
               case 100:
                  star5 += 1;
                  break;
               case 80:
                  star4 += 1;
                  break;
               case 60:
                  star3 += 1;
                  break;
               case 40:
                  star2 += 1;
                  break;
               case 20:
                  star1 += 1;
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

}
