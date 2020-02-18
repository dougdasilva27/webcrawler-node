package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class SaopauloPanvelCrawler extends Crawler {

   private final String HOME_PAGE_HTTP = "http://www.panvel.com/";
   private final String HOME_PAGE_HTTPS = "https://www.panvel.com/";

   public SaopauloPanvelCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);

   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
   }



   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         String description = crawlDescription(doc);

         JSONObject chaordic = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.chaordic_meta =", ";", false);
         JSONObject dataLayer = scrapSpecialJSON(doc);
         JSONObject productJson = chaordic.has("product") ? chaordic.getJSONObject("product") : new JSONObject();

         String internalId = crawlInternalId(productJson, dataLayer);
         String internalPid = internalId;
         String name = crawlName(productJson, dataLayer);
         Float price = available ? crawlPrice(productJson, dataLayer) : null;
         Prices prices = crawlPrices(price, productJson, dataLayer);
         RatingsReviews ratingReviews = crawlRatingsReviews(doc, internalId);
         String url = internalId != null ? CrawlerUtils.getRedirectedUrl(session.getOriginalURL(), session) : session.getOriginalURL();

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(url)
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
      return doc.select(".item-detalhe").first() != null;
   }

   /**
    * We need this json for some products. Ex:
    * https://www.panvel.com/panvel/etira-100mgml-sol-oral-100ml-seringa-dosadora-c1/p-117841
    * 
    * JSON Example:
    * 
    * window.dataLayer = window.dataLayer || []; window.dataLayer.push({"statusProduto":
    * 'indisponível'}); window.dataLayer.push({"productName":"Etira 100mg\/ml Sol Oral 100ml + Seringa
    * Dosadora
    * C1","productSku":117841,"productCategoryIdN1":1,"productCategoryNameN1":"Medicamentos","productCategoryIdN2":28,"productCategoryNameN2":"Sistema
    * Nervoso","productCategoryIdN3":142,"productCategoryNameN3":"Convulsão","productStock":0,"productDescription":"lorem
    * ipsum
    * 30g.","productImageUrl":"https:\/\/cdn1.staticpanvel.com.br\/produtos\/15\/forbidden.jpg?ims=400x","productPrice":"43.80","productPriceOriginal":"50.87"});
    * 
    * window.chaordic_meta = {"page":{"name":"landing_page"}};
    * 
    * window.chaordic_meta.page.timestamp = new Date();
    * 
    * @param doc
    * @return
    */
   private JSONObject scrapSpecialJSON(Document doc) {
      JSONObject dataLayer = new JSONObject();

      String firstIndexString = "dataLayer.push(";
      String lastIndexString = "});";
      String idKey = "productSku";

      Elements scripts = doc.select("script");

      for (Element e : scripts) {
         String html = e.html();

         if (html.contains(firstIndexString) && html.contains(lastIndexString) && html.contains(idKey)) {
            String jsonString = CrawlerUtils.extractSpecificStringFromScript(html, firstIndexString, true, lastIndexString, false);
            dataLayer = CrawlerUtils.stringToJson(jsonString);
            break;
         }
      }

      return dataLayer;
   }

   private String crawlInternalId(JSONObject product, JSONObject dataLayer) {
      String internalId = null;

      if (product.has("id") && product.isNull("id")) {
         internalId = product.get("id").toString();
      } else if (dataLayer.has("productSku") && !dataLayer.isNull("productSku")) {
         internalId = dataLayer.get("productSku").toString();
      }

      return internalId;
   }

   private RatingsReviews crawlRatingsReviews(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      Integer commentsNumber = getTotalNumOfRatings(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(commentsNumber);
      ratingReviews.setTotalWrittenReviews(commentsNumber);
      ratingReviews.setAverageOverallRating(getTotalAvgRating(doc, commentsNumber));
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document document, Integer commentsNumber) {
      Double stars = 0d;

      if (commentsNumber > 0) {
         Double values = 0d;
         Elements comments = document.select(".box-comment .comment-title__rating");
         for (Element e : comments) {
            values += e.select("i.active").size();
         }

         stars = MathUtils.normalizeTwoDecimalPlaces(values / commentsNumber);
      }
      return stars;
   }

   private Integer getTotalNumOfRatings(Document doc) {
      return doc.select(".box-comment").size();
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".comment-title__rating .material-icons.active");

      for (Element review : reviews) {

         Element elementStarNumber = review.attr("class", ".material-icons.active");

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.ownText();
            System.err.println(stringStarNumber);
            Integer numberOfStars = MathUtils.parseInt(stringStarNumber);
            Integer stars = numberOfStars.SIZE;


            switch (stars) {
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

   private String crawlName(JSONObject product, JSONObject dataLayer) {
      String name = null;

      if (product.has("name") && !product.isNull("name")) {
         name = product.getString("name");
      } else if (dataLayer.has("productName") && !dataLayer.isNull("productName")) {
         name = dataLayer.get("productName").toString();
      }

      return name;
   }

   private Float crawlPrice(JSONObject product, JSONObject dataLayer) {
      Float price = null;

      if (product.has("price")) {
         price = CrawlerUtils.getFloatValueFromJSON(product, "price", true, false);
      } else if (dataLayer.has("productPrice") && !dataLayer.isNull("productPrice")) {
         price = CrawlerUtils.getFloatValueFromJSON(dataLayer, "productPrice", true, false);
      }

      return price;
   }

   private boolean crawlAvailability(Document doc) {
      return !doc.select(".item-detalhe [data-click=\"addToCart\"]").isEmpty();
   }

   private String crawlPrimaryImage(Document document) {
      String primaryImage = null;
      Element primaryImageElement = document.select(".slideshow__slides div > img").first();

      if (primaryImageElement != null) {
         primaryImage = primaryImageElement.attr("src").trim();
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document document) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imagesElement = document.select(".slideshow__slides div > img");

      for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
         String image = imagesElement.get(i).attr("src").trim();
         secondaryImagesArray.put(image);
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = doc.select(".breadcrumb a span");

      for (int i = 1; i < elementCategories.size(); i++) { // first index is the home page
         categories.add(elementCategories.get(i).text().replace("/", "").trim());
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();
      Element desc = document.select(".item-description").first();

      if (desc != null) {
         description.append(desc.html());
      }

      return description.toString();
   }

   private Prices crawlPrices(Float price, JSONObject product, JSONObject dataLayer) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);
         prices.setBankTicketPrice(price);

         if (product.has("old_price")) {
            prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(product, "old_price", true, false));
         } else {
            prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(dataLayer, "productPriceOriginal", true, false));
         }

         if (product.has("installment")) {
            JSONObject installment = product.getJSONObject("installment");

            if (installment.has("count") && installment.has("price")) {
               String textCount = installment.get("count").toString().replaceAll("[^0-9]", "");
               String textPrice = installment.get("price").toString().replaceAll("[^0-9.]", "");

               if (!textCount.isEmpty() && !textPrice.isEmpty()) {
                  installmentPriceMap.put(Integer.parseInt(textCount), Float.parseFloat(textPrice));
               }
            }
         }

         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      }

      return prices;
   }
}
