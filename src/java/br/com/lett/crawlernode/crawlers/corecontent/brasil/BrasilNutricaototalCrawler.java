package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-box.price-final_price", "data-product-id");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=\"sku\"]", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.page-title > span", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li[class^=category]");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "head > meta[property=\"og:image\"]", Arrays.asList("content"), "https", "www.nutricaototal.com.br");
         String secondaryImages = crawlSecondaryImages(doc, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.product.attribute.overview > div > b", "div.product.attribute.overview > div"));
         Integer stock = null;
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "span.price", null, true, ',', session);
         Prices prices = crawlPrices(price, doc);
         boolean available = checkAvaliability(doc, "#product-addtocart-button > span");
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
      return doc.selectFirst(".product-info-main") != null;
   }

   private String crawlSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      JSONObject productInfo = CrawlerUtils.selectJsonFromHtml(doc, ".product.media [type=\"text/x-magento-init\"]", null, null, true, false);

      if (productInfo.has("[data-gallery-role=gallery-placeholder]")) {
         productInfo = productInfo.getJSONObject("[data-gallery-role=gallery-placeholder]");
         if (productInfo.has("mage/gallery/gallery")) {
            productInfo = productInfo.getJSONObject("mage/gallery/gallery");
            if (productInfo.has("data")) {
               JSONArray imagesArray = productInfo.getJSONArray("data");
               for (int i = 0; i < imagesArray.length(); i++) {
                  JSONObject jsonImg = imagesArray.getJSONObject(i);
                  String image = jsonImg.getString("img");

                  if (!primaryImage.contains(image) && !primaryImage.equals(image)) {
                     if (image.startsWith(HOME_PAGE)) {
                        secondaryImagesArray.put(CommonMethods.sanitizeUrl(image));
                     } else {
                        secondaryImagesArray.put(CommonMethods.sanitizeUrl(HOME_PAGE + image));
                     }
                  }
               }
            }


         }

      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      StringBuilder str = new StringBuilder();
      str.append("https://www.nutricaototal.com.br/review/product/listAjax/id/");
      str.append(internalId);

      Document docRating = sendRequest(str.toString());

      Integer totalNumOfEvaluations = crawlNumOfEvaluations(docRating, "ol > li");
      Double avgRating = crawlAvgRating(docRating, "ol > li span[itemprop=\"ratingValue\"]");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(docRating, "ol > li span[itemprop=\"ratingValue\"]");

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Document sendRequest(String str) {
      Request request = RequestBuilder.create().setUrl(str).setCookies(cookies).build();
      String endpointResponseStr = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(endpointResponseStr);
   }

   private Integer crawlNumOfEvaluations(Document doc, String selector) {
      Integer numRating = 0;
      Elements el = doc.select(selector);

      for (Element reviews : el) {
         numRating++;
      }
      return numRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc, String selector) {
      String starsText = "";
      Integer stars = null;

      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(selector);


      for (Element review : reviews) {

         starsText = review.text().replace("%", "");
         starsText = starsText.replaceAll("[^0-9]", "");
         stars = Integer.parseInt(starsText);
         stars = (stars * 5) / 100;


         // On a html this value will be like this: (1)


         switch (stars) {
            case 5:
               star5++;
               break;
            case 4:
               star4++;
               break;
            case 3:
               star3++;
               break;
            case 2:
               star2++;
               break;
            case 1:
               star1++;
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

   private Double crawlAvgRating(Document doc, String selector) {
      Double num = null;
      Elements e = doc.select(selector);

      for (Element el : e) {
         String avRating = e.text().replace("%", "");
         num = Double.parseDouble(avRating);
         num = (num * 5) / 100;
      }

      return num;
   }

   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         Element priceFrom = doc.select("div.price-box.price-final_price > span > span.preco-desconto > b > span").first();
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
