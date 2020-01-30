package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilKalungaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.kalunga.com.br/";

   public BrasilKalungaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
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

         String internalId = crawlInternalId(doc);
         String internalPid = null;
         String name = crawlName(doc);
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(doc, price);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc, primaryImage);
         String description = crawlDescription(doc);
         Integer stock = null;
         Marketplace marketplace = crawlMarketplace();
         RatingsReviews ratingReviews = crawRating(doc);

         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
               .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setMarketplace(marketplace).setRatingReviews(ratingReviews).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("input#hdnCodProduto").first() != null;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("input#hdnCodProduto").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("h1.h5").first();

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }

   private Float crawlPrice(Document document) {
      Float price = null;
      Element salePriceElement = document.selectFirst("[itemprop=\"price\"]");

      if (salePriceElement != null && salePriceElement.hasAttr("content")) {
         price = MathUtils.parseFloatWithDots(salePriceElement.attr("content"));
      }

      return price;
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }


   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;
      Element elementPrimaryImage = doc.select(".carousel-produto-grande .item figure > a").first();

      if (elementPrimaryImage != null) {
         primaryImage = elementPrimaryImage.attr("href");
      }

      return primaryImage;
   }

   /**
    * Quando este crawler foi feito, nao tinha imagens secundarias
    * 
    * @param doc
    * @return
    */
   private String crawlSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select(".carousel-produto-grande .item figure > a");

      for (Element e : images) {
         String image = e.attr("href");

         if (!image.equals(primaryImage)) {
            secondaryImagesArray.put(image);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumbs > a");

      for (int i = 1; i < elementCategories.size(); i++) {
         String cat = elementCategories.get(i).ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element elementDescription = doc.select("#ctl00_Body_dvEspecificacaoAdicionalTop").first();

      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      Element specs = doc.select("#descricaoPadrao").first();

      if (specs != null) {
         description.append(specs.html());
      }

      return description.toString();
   }

   private boolean crawlAvailability(Document doc) {
      return doc.select("#ctl00_Body_ibtnComprar").first() != null;
   }

   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentsPriceMap = new HashMap<>();

         // Preço de boleto e 1x no cartao são iguais
         prices.setBankTicketPrice(price);
         installmentsPriceMap.put(1, price);

         Elements installments = doc.select(".container-price table.table-bordered tr");

         for (Element e : installments) {
            Element installmentElement = e.select("td").first();
            Element valueElement = e.select("td").last();

            if (installmentElement != null && valueElement != null) {
               String text = installmentElement.text().replaceAll("[^0-9]", "");
               Float value = MathUtils.parseFloatWithComma(valueElement.text());

               if (!text.isEmpty() && value != null) {
                  installmentsPriceMap.put(Integer.parseInt(text), value);
               }
            }
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentsPriceMap);
      }

      return prices;
   }



   private RatingsReviews crawRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document doc) {
      Double avgRating = null;
      Element rating = doc.selectFirst("span[itemprop=ratingValue]");

      if (rating != null) {
         String text = rating.ownText().trim();

         if (!text.isEmpty()) {
            avgRating = MathUtils.normalizeTwoDecimalPlaces(MathUtils.parseFloatWithComma(text).doubleValue());
         }
      }

      return avgRating;
   }


   private Integer getTotalNumOfRatings(Document doc) {
      Integer rating = null;
      Element ratingElement = doc.selectFirst("span[itemprop=reviewCount]");

      if (ratingElement != null) {
         rating = Integer.parseInt(ratingElement.ownText().replaceAll("[^0-9]", "").trim());
      }

      return rating;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".row.avaliacao_box");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst("[itemprop=reviewRating]");

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.selectFirst("span[itemprop=ratingValue]").toString();
            Integer numberOfStars = MathUtils.parseInt(stringStarNumber);

            switch (numberOfStars) {
               case 5:
                  star5 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 1:
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
