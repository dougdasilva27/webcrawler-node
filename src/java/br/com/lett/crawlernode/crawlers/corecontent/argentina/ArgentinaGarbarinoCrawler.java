package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class ArgentinaGarbarinoCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.garbarino.com/";

   public ArgentinaGarbarinoCrawler(Session session) {
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
         String internalPid = null;
         String name = crawlName(doc);
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(price, doc);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".gb-breadcrumb > ul > li:not(:first-child) > a");
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc, primaryImage);
         String description = crawlDescription(doc);
         Marketplace marketplace = crawlMarketplace();
         RatingsReviews ratingsReviews = scrapRatingReviews(doc);
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
               .setMarketplace(marketplace)
               .setRatingReviews(ratingsReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select(".title-product").first() != null;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.selectFirst(".gb--gray");
      if (internalIdElement != null) {
         internalId = internalIdElement.attr("data-product-id");
      }

      return internalId;
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gb-js-main-image-thumbnail > a", Arrays.asList("href", "data-standart"),
            "https:", "d34zlyc2cp9zm7.cloudfront.net");

      if (primaryImage == null) {
         Element image = doc.selectFirst(".gb-main-detail-gallery-grid-img-full img");
         if (image != null) {
            primaryImage = CrawlerUtils.sanitizeUrl(image, "src", "https:", "d34zlyc2cp9zm7.cloudfront.net");
         }
      }

      if (primaryImage != null && primaryImage.contains(".")) {
         int x = primaryImage.lastIndexOf('.');
         primaryImage = primaryImage.substring(0, x) + ".jpg";
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".gb-js-main-image-thumbnail > a", Arrays.asList("href", "data-standart"),
            "https:", "d34zlyc2cp9zm7.cloudfront.net", primaryImage);

      if (secondaryImages == null) {
         JSONArray secondaryImagesArray = new JSONArray();

         Elements images = doc.select(".gb-main-detail-gallery-grid-list li img");
         for (Element e : images) {
            String image = CrawlerUtils.sanitizeUrl(e, "src", "https:", "d34zlyc2cp9zm7.cloudfront.net");

            if (image != null && image.contains(".")) {
               int x = image.lastIndexOf('.');
               image = image.substring(0, x) + ".jpg";
            }

            if (primaryImage == null || !primaryImage.equals(image)) {
               secondaryImagesArray.put(image);
            }
         }

         if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString();
         }
      }

      return secondaryImages;
   }

   private String crawlName(Document doc) {
      String name = null;
      Element nameElement = doc.selectFirst(".title-product > h1");

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }
      return name;
   }

   private Float crawlPrice(Document doc) {
      Float price = null;

      Element specialPrice = doc.selectFirst("#final-price");

      if (specialPrice != null) {
         price = MathUtils.parseFloatWithComma(specialPrice.ownText());
      }
      return price;
   }

   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         prices.setPriceFrom(crawlPriceFrom(doc));

         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         Elements installments =
               doc.select(".payments .payment-method:first-child .gb-detail-fees-detail[data-payment-id=1] .gb-detail-fees-table-body ul li:first-child");
         for (Element e : installments) {
            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, true, "de");

            if (!pair.isAnyValueNull()) {
               installmentPriceMap.put(pair.getFirst(), pair.getSecond());
            }
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);


      }

      return prices;
   }



   private Double crawlPriceFrom(Document doc) {
      Double price = null;

      Element from = doc.selectFirst(".value-note > del");
      if (from != null) {
         price = MathUtils.parseDoubleWithComma(from.ownText());
      }
      return price;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalComments = CrawlerUtils.scrapIntegerFromHtml(doc, ".gb-main-detail-content .gb-main-detail-title .gb-rating .gb-rating-legend:last-child", false, 0);
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

      Double percentage = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".modal-body .gb-product-reviews-rating strong", null, false, '.', session);
      if (percentage != null) {
         avg = percentage;
      }

      return avg;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".gb-product-reviews .gb-product-reviews-rating-detail .gb-product-reviews-rating-detail-row");

      for (Element review : reviews) {

         Element starNumber = review.selectFirst(".pipi");

         if (starNumber != null) {
            String sN = starNumber.text().replaceAll("[^0-9]", "");
            Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0;

            Element voteNumber = review.selectFirst(".product-calif:not(:empty)");

            if (voteNumber != null) {

               String vN = voteNumber.text().replaceAll("[^0-9]", "");
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0;

               switch (numberOfStars) {
                  case 5:
                     star5 = numberOfVotes;
                     break;
                  case 4:
                     star4 = numberOfVotes;
                     break;
                  case 3:
                     star3 = numberOfVotes;
                     break;
                  case 2:
                     star2 = numberOfVotes;
                     break;
                  case 1:
                     star1 = numberOfVotes;
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

   private boolean crawlAvailability(Document document) {
      boolean available = false;

      Element outOfStockElement = document.selectFirst(".gb-button");
      if (outOfStockElement != null) {
         available = true;
      }

      return available;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element ean = doc.selectFirst("script[data-flix-ean]");
      if (ean != null) {

         String url = "https://media.flixcar.com/delivery/js/inpage/2468/f4/40/ean/" + ean.attr("data-flix-ean") + "?&=2468&=f4&ean="
               + ean.attr("data-flix-ean") + "&ssl=1&ext=.js";

         if (!ean.attr("data-flix-mpn").isEmpty()) {
            url = url.replace("/40/", "/mpn/" + ean.attr("data-flix-mpn") + "/") + "&mpn=" + ean.attr("data-flix-mpn");
         }

         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         String script = this.dataFetcher.get(session, request).getBody();

         final String token = "$(\"#flixinpage_\"+i).inPage";

         JSONObject productInfo = new JSONObject();

         if (script.contains(token)) {
            int x = script.indexOf(token + " (") + token.length() + 2;
            int y = script.indexOf(");", x);

            String json = script.substring(x, y);

            try {
               productInfo = new JSONObject(json);
            } catch (JSONException e) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         }

         if (productInfo.has("product")) {
            String id = productInfo.getString("product");

            String urlDesc =
                  "https://media.flixcar.com/delivery/inpage/show/2468/f4/" + id + "/json?c=jsonpcar2468f4" + id + "&complimentary=0&type=.html";

            Request requestDesc = RequestBuilder.create().setUrl(urlDesc).setCookies(cookies).build();
            String scriptDesc = this.dataFetcher.get(session, requestDesc).getBody();

            if (scriptDesc.contains("({")) {
               int x = scriptDesc.indexOf("({") + 1;
               int y = scriptDesc.lastIndexOf("})") + 1;

               String json = scriptDesc.substring(x, y);

               try {
                  JSONObject jsonInfo = new JSONObject(json);

                  if (jsonInfo.has("html")) {
                     if (jsonInfo.has("css")) {
                        description.append("<link href=\"" + jsonInfo.getString("css") + "\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\">");
                     }

                     description.append(jsonInfo.get("html").toString().replace("//media", "https://media"));
                  }
               } catch (JSONException e) {
                  Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
               }
            }
         }
      }

      Element techElement = doc.selectFirst(".gb-tech-spec");

      if (techElement != null) {
         description.append(techElement.html());
      }

      Element descriptionElement = doc.selectFirst("gb-description");

      if (descriptionElement != null) {
         description.append(descriptionElement.html());
      }

      return description.toString();
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }

}
