package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 11/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilNutriiCrawler extends Crawler {

   private final String HOME_PAGE = "http://www.nutrii.com.br/";

   public BrasilNutriiCrawler(Session session) {
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

         String internalId = crawlInternalId(doc);
         String internalPid = crawlInternalPid(doc);
         String name = crawlName(doc);
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(price, doc);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#slider img, .more-views ul a", Arrays.asList("src", "href"), "https",
               "www.nutrii.com.br", primaryImage);

         String description = crawlDescription(doc);
         Integer stock = null;
         Marketplace marketplace = crawlMarketplace();
         RatingsReviews ratingReviews = crawlRating(doc);

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
      if (doc.select("input[name=product]").first() != null) {
         return true;
      }
      return false;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("input[name=product]").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private RatingsReviews crawlRating(Document document) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document);
      AdvancedRatingReview adRating = scrapAdvancedRatingReview(document);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(adRating);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer val = 0;
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select("#customer-reviews > dl > dd table > tbody > tr > td > div > div");



      for (Element review : reviews) {
         String el = review.attr("style").replace("%", "");
         el = el.replace("width:", "");
         el = el.replace(";", "");
         if (!el.isEmpty()) {
            val = Integer.parseInt(el);
         }
         val = (val * 5) / 100;


         // On a html this value will be like this: (1)


         switch (val) {
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

      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }

   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;
      Element avg = doc.select("div.rating").first();

      if (avg != null) {
         avgRating = MathUtils.parseDoubleWithDot(avg.attr("style"));
         if (avgRating != null) {
            avgRating = (avgRating / 100) * 5;
         } else {
            avgRating = (double) 0;
         }
      }

      return avgRating;
   }

   private Integer getTotalNumOfRatings(Document doc) {
      Integer ratingNumber = 0;
      Element reviews = doc.select(".rating-links span.cor").first();

      if (reviews != null) {
         String text = reviews.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            ratingNumber = Integer.parseInt(text);
         }
      }

      return ratingNumber;
   }


   private String crawlInternalPid(Document doc) {
      String internalPid = null;
      Element pdi = doc.select(".product-info p[itemprop=mpn]").first();

      if (pdi != null) {
         internalPid = pdi.ownText().replaceAll("[^0-9]", "").trim();
      }

      return internalPid;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select(".product-info h1").first();

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      } else {
         Element nameSpecial = document.selectFirst(".product-title");

         if (nameSpecial != null) {
            name = nameSpecial.ownText();
         }
      }

      return name;
   }

   private Float crawlPrice(Document document) {
      Float price = null;
      Element discountPrice = document.select(".display-preco-com-desconto").first();
      Element salePriceElement = document.select(".price-box .price").first();

      if (discountPrice != null) {
         price = MathUtils.parseFloatWithComma(discountPrice.text());
      } else if (salePriceElement != null) {
         price = MathUtils.parseFloatWithComma(salePriceElement.text());
      }

      return price;
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }


   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;
      Element elementPrimaryImage = doc.select(".product-image > a").first();

      if (elementPrimaryImage != null) {
         primaryImage = elementPrimaryImage.attr("href");
      } else {
         Element imageSpecial = doc.selectFirst("#slider img");

         if (imageSpecial != null) {
            primaryImage = imageSpecial.attr("src");
         }
      }

      return primaryImage;
   }

   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumbs li[class^=category] a");

      for (int i = 0; i < elementCategories.size(); i++) {
         String cat = elementCategories.get(i).text().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description-new-prod", ".desc.hidden-mobile")));

      Elements specialDesc = doc.select(".product-custom-layout .product-nav, .product-custom-layout .featured-banner, .product-custom-layout section");
      for (Element e : specialDesc) {
         description.append(e.html());
      }

      return description.toString();
   }

   private boolean crawlAvailability(Document doc) {
      return doc.select(".prod-esg").first() == null;
   }

   /**
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         Element bank = doc.select("p[class=fs-14] span.bold").first();
         Element specialPrice = doc.select(".bg-price.ico-boleto").first();

         if (bank != null) {
            Float bankTicket = MathUtils.parseFloatWithComma(bank.ownText());

            if (bankTicket != null) {
               prices.setBankTicketPrice(bankTicket);
            } else {
               prices.setBankTicketPrice(price);
            }
         } else if (specialPrice != null) {
            Float bankTicket = MathUtils.parseFloatWithComma(specialPrice.ownText());

            if (bankTicket != null) {
               prices.setBankTicketPrice(bankTicket);
            } else {
               prices.setBankTicketPrice(price);
            }

         } else {
            prices.setBankTicketPrice(price);
         }

         Element installmentsElement = doc.select("div .bg-price.ico-cartao").first();

         if (installmentsElement != null) {
            String text = installmentsElement.ownText().toLowerCase().trim();

            if (text.contains("x")) {
               int x = text.indexOf('x');

               String installmentText = CommonMethods.getLast(text.substring(0, x).trim().split(" ")).replaceAll("[^0-9]", "");
               Float value = MathUtils.parseFloatWithComma(text.substring(x).trim());

               if (!installmentText.isEmpty() && value != null) {
                  installmentPriceMap.put(Integer.parseInt(installmentText), value);
               }
            }
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      }

      return prices;
   }

}
