package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.Seller;
import models.Util;
import models.prices.Prices;

/**
 * 02/02/2018
 * 
 * @author gabriel
 *
 */
public class BrasilCarrefourCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.carrefour.com.br/";
   private static final String SELLER_NAME_LOWER = "carrefour";

   public BrasilCarrefourCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL()));
   }

   protected String fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("referer", HOME_PAGE);

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .setHeaders(headers)
            .setFetcheroptions(
                  FetcherOptionsBuilder.create()
                        .mustUseMovingAverage(false)
                        .build())
            .setProxyservice(Arrays.asList(
                  ProxyCollection.INFATICA_RESIDENTIAL_BR,
                  ProxyCollection.STORM_RESIDENTIAL_EU))
            .build();

      int attempts = 0;
      Response response = this.dataFetcher.get(session, request);
      String body = response.getBody();

      Integer statusCode = 0;
      List<RequestsStatistics> requestsStatistics = response.getRequests();
      if (!requestsStatistics.isEmpty()) {
         statusCode = requestsStatistics.get(requestsStatistics.size() - 1).getStatusCode();
      }

      boolean retry = statusCode == null ||
            (Integer.toString(statusCode).charAt(0) != '2'
                  && Integer.toString(statusCode).charAt(0) != '3'
                  && statusCode != 404);

      // If fetcher don't return the expected response we try with apache
      // If apache do the same, we try with javanet
      if (retry) {
         do {
            if (attempts == 0) {
               body = new ApacheDataFetcher().get(session, request).getBody();
            } else if (attempts == 1) {
               body = new JavanetDataFetcher().get(session, request).getBody();
            }

            attempts++;
         } while (attempts < 2 && (body == null || body.isEmpty()));
      }

      return body;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(session.getOriginalURL());
         String name = crawlName(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         String description = crawlDescription(doc);
         String internalPid = crawlInternalPid(doc);
         Elements marketplacesElements = doc.select(".list-group-item");
         Map<String, Prices> marketplaceMap;
         Offers offers = scrapOffers(doc, internalPid);
         Double priceFrom = crawlPriceFrom(doc);

         if (marketplacesElements.isEmpty()) {
            marketplaceMap = crawlMarketplaceForSingleSeller(doc, internalPid);
         } else {
            marketplaceMap = crawlMarketplaceForMutipleSellers(marketplacesElements);
         }

         Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, marketplaceMap.size() > 1 ? null : priceFrom);

         boolean available = marketplaceMap.containsKey(SELLER_NAME_LOWER);
         Prices prices = available ? marketplaceMap.get(SELLER_NAME_LOWER) : new Prices();
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".priceBig", null, false, ',', session);

         RatingsReviews rating = crawlRatingReviews(doc);

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
               .setOffers(offers)
               .setRatingReviews(rating)
               .build();

         products.add(product);

      } else

      {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document doc) {
      return doc.select(".product-details-panel").first() != null;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlInternalPid(Document document) {
      String internalId = null;
      Element internalIdElement = document.select("#productCod").first();

      if (internalIdElement != null) {
         internalId = internalIdElement.val().trim();
      }

      return internalId;
   }

   private String crawlInternalId(String url) {
      String internalPid = null;

      if (url.contains("?")) {
         url = url.split("\\?")[0];
      }

      if (url.contains("/p/")) {
         String[] tokens = url.split("/p/");

         if (tokens.length > 1 && tokens[1].contains("/")) {
            internalPid = tokens[1].split("/")[0];
         } else if (tokens.length > 1) {
            internalPid = tokens[1];
         }
      }
      return internalPid;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("h1[itemprop=name]").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   // private Integer scrapStock(String internalId, String internalPid, Document doc) {
   // Integer stock = 0;
   //
   // Map<String, String> headers = scrapHeadersForAccessStockApi(doc);
   //
   // StringBuilder url = new StringBuilder();
   // url.append("https://api2.carrefour.com.br/cci/publico/cci-ecom-consulta-estoque/v3/content/products/")
   // .append(internalId);
   //
   // if (internalPid.startsWith("M")) {
   // url.append("?productPartnerId=").append(internalPid);
   // }
   //
   // Request request = RequestBuilder.create()
   // .setUrl(url.toString())
   // .setCookies(cookies)
   // .setHeaders(headers)
   // .build();
   //
   // JSONObject apiJson = JSONUtils.stringToJson(new ApacheDataFetcher().get(session,
   // request).getBody());
   //
   // if (apiJson.has("product") && apiJson.get("product") instanceof JSONObject) {
   // JSONObject product = apiJson.getJSONObject("product");
   //
   // if (product.has("stock") && product.get("stock") instanceof JSONObject) {
   // JSONObject stockJson = product.getJSONObject("stock");
   //
   // stock = JSONUtils.getIntegerValueFromJSON(stockJson, "quantity", 0);
   // }
   // }
   //
   // return stock;
   // }
   //
   // private Map<String, String> scrapHeadersForAccessStockApi(Document doc) {
   // Map<String, String> headers = new HashMap<>();
   // headers.put("accept", "application/json, text/javascript, */*; q=0.01");
   // headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
   // headers.put("referer", HOME_PAGE);
   //
   // String authToken = "apiCatalogAuthorization=\"";
   // String clientToken = "apiCatalogIbmClientId=\"";
   // Elements scripts = doc.select("script");
   // for (Element e : scripts) {
   // String script = e.html().replace(" ", "");
   //
   // if (script.contains(authToken) && script.contains(clientToken)) {
   // String auth = CrawlerUtils.extractSpecificStringFromScript(script, authToken, true, "\";",
   // false);
   // String clientId = CrawlerUtils.extractSpecificStringFromScript(script, clientToken, true, "\";",
   // false);
   //
   // headers.put(HttpHeaders.AUTHORIZATION, "Basic " + auth);
   // headers.put("X-IBM-Client-Id", clientId);
   //
   // break;
   // }
   // }
   //
   // return headers;
   // }

   private Float crawlMainPagePrice(Document document) {
      return CrawlerUtils.scrapFloatPriceFromHtml(document, ".prince-product-default :first-child", null, true, ',', session);
   }

   private Offers scrapOffers(Document doc, String internalPid) {
      Offers offers = new Offers();

      JSONArray dataLayer = CrawlerUtils.selectJsonArrayFromHtml(doc, "script[type=\"text/javascript\"]", "dataLayer = ", ";", false, false);
      if (dataLayer.length() > 0) {
         JSONObject productInfo = dataLayer.getJSONObject(0);

         if (productInfo.has("buyBoxOffers") && !productInfo.isNull("buyBoxOffers")) {
            JSONArray buyBoxOffers = productInfo.getJSONArray("buyBoxOffers");

            int position = 1;
            for (Object o : buyBoxOffers) {
               JSONObject offerJson = (JSONObject) o;

               if (offerJson.has("miraklVendor") && !offerJson.isNull("miraklVendor") && offerJson.has("price") && !offerJson.isNull("price")) {
                  JSONObject miraklVendor = offerJson.getJSONObject("miraklVendor");
                  JSONObject priceJson = offerJson.getJSONObject("price");

                  if (miraklVendor.has("name") && miraklVendor.has("code") && priceJson.has("value")) {
                     String sellerName = miraklVendor.get("name").toString();

                     try {
                        Offer offer = new OfferBuilder().setInternalSellerId(miraklVendor.get("code").toString()).setMainPagePosition(position)
                              .setMainPrice(JSONUtils.getDoubleValueFromJSON(priceJson, "value", true)).setSellerFullName(sellerName)
                              .setSlugSellerName(CommonMethods.toSlug(sellerName)).setIsBuybox(true).build();
                        offers.add(offer);
                     } catch (OfferException e) {
                        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
                     }

                     position++;
                  }
               }
            }

         } else if (productInfo.has("sellerId")) {
            String sellerId = productInfo.optString("sellerId");
            float price = productInfo.optFloat("price");
            offers = scrapOffersForSingleSeller(doc, internalPid, sellerId, price);
         }
      }

      return offers;
   }

   private Offers scrapOffersForSingleSeller(Document document, String internalPid, String sellerId, Float price) {
      Offers offers = new Offers();

      Element notifyMeElement = document.select(".text-not-product-avisme").first();

      if (notifyMeElement == null && price != null) {
         Element oneMarketplaceInfo = document.selectFirst(".block-add-cart #moreInformation" + internalPid);
         Element oneMarketplace = document.selectFirst(".block-add-cart > span");

         String sellerName = "Carrefour";

         if (oneMarketplaceInfo != null && oneMarketplace != null) {
            String text = oneMarketplace.ownText().trim();

            if (text.contains("por") && text.contains(".")) {
               int x = text.indexOf("por") + 3;
               int y = text.lastIndexOf('.');

               sellerName = text.substring(x, y).trim();

            }
         }

         String slugName = CommonMethods.toSlug(sellerName);

         try {
            Offer offer = new OfferBuilder().setInternalSellerId(sellerId).setMainPagePosition(1).setMainPrice(price.doubleValue())
                  .setSellerFullName(sellerName).setSlugSellerName(slugName).setIsBuybox(false).build();
            offers.add(offer);
         } catch (OfferException e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
         }
      }

      return offers;
   }

   private Map<String, Prices> crawlMarketplaceForSingleSeller(Document document, String internalPid) {
      Map<String, Prices> marketplaces = new HashMap<>();

      Element oneMarketplaceInfo = document.select(".block-add-cart #moreInformation" + internalPid).first();
      Element oneMarketplace = document.select(".block-add-cart > span").first();
      Float price = crawlMainPagePrice(document);

      if (oneMarketplace != null || price != null) {
         Prices prices = crawlPrices(price, document);

         if (oneMarketplaceInfo != null && oneMarketplace != null) {
            // This text appears like: "Venda por taQi."
            // Sometimes marketplace name it's inside a link, so for this
            // we need to use ".text()" instead ".ownText()"
            // Check on: https://www.carrefour.com.br/Coifa-de-Parede-Inox-Consul-CAP90AR-90cm-110V/p/5086450
            String text = oneMarketplace.text().trim().toLowerCase();

            if (text.contains("por") && text.contains(".")) {
               int x = text.indexOf("por") + 3;
               int y = text.lastIndexOf('.');

               String sellerName = text.substring(x, y).trim();

               marketplaces.put(sellerName, prices);
            }
         } else {
            marketplaces.put(SELLER_NAME_LOWER, prices);
         }
      }

      return marketplaces;
   }

   private Map<String, Prices> crawlMarketplaceForMutipleSellers(Elements marketplacesElements) {
      Map<String, Prices> marketplaces = new HashMap<>();

      for (Element e : marketplacesElements) {
         Element name = e.select(".font-mirakl-vendor-name strong").first();
         Element price = e.select("span.big-price").first();

         if (name != null && price != null) {
            String sellerName = name.ownText().trim().toLowerCase();
            Float sellerPrice = MathUtils.parseFloatWithComma(price.ownText());

            if (sellerPrice != null && !sellerName.isEmpty()) {
               marketplaces.put(sellerName, crawlPrices(sellerPrice, e));
            }
         }
      }

      return marketplaces;
   }

   private Float crawlPrice(Prices prices) {
      Float price = null;

      if (prices != null && !prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
         Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
         price = priceDouble.floatValue();
      }

      return price;
   }

   private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap, Double priceFrom) {
      Marketplace marketplace = new Marketplace();

      for (String sellerName : marketplaceMap.keySet()) {
         if (!sellerName.equalsIgnoreCase(SELLER_NAME_LOWER)) {
            JSONObject sellerJSON = new JSONObject();
            sellerJSON.put("name", sellerName);

            Prices prices = marketplaceMap.get(sellerName);

            sellerJSON.put("price", crawlPrice(prices));
            sellerJSON.put("prices", prices.toJSON());

            try {
               Seller seller = new Seller(sellerJSON);
               marketplace.add(seller);
            } catch (Exception e) {
               Logging.printLogError(logger, session, Util.getStackTraceString(e));
            }
         }
      }

      return marketplace;
   }

   private Double crawlPriceFrom(Element e) {
      Double price = null;

      Element priceFrom = e.select(".price-old").first();
      if (priceFrom != null) {
         price = MathUtils.parseDoubleWithComma(priceFrom.text());
      }

      return price;
   }

   private String crawlPrimaryImage(Document document) {
      String primaryImage = null;
      Element primaryImageElement = document.select(".sust-gallery div.item .thumb img").first();

      if (primaryImageElement != null) {
         String image = primaryImageElement.attr("data-zoom-image");
         if (image != null) {
            primaryImage = image.trim();
         }

      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document document) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imagesElement = document.select(".sust-gallery div.item .thumb img");

      for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image
                                                       // is the primary image
         Element e = imagesElement.get(i);

         if (e.attr("data-zoom-image") != null && !e.attr("data-zoom-image").isEmpty()) {
            secondaryImagesArray.put(e.attr("data-zoom-image").trim());
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
      Elements elementCategories = document.select(".breadcrumb > li > a");

      for (int i = 1; i < elementCategories.size() - 1; i++) { // starting from index 1, because the
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();

      Element desc2 = document.select(".productDetailsPageShortDescription").first();
      if (desc2 != null) {
         description.append(desc2.outerHtml());
      }

      Elements descriptionElements = document.select("#accordionFichaTecnica");
      if (descriptionElements != null) {
         description.append(descriptionElements.html());
      }

      Element desc = document.select(".productDetailsPageDescription").first();
      if (desc != null) {
         description.append(desc.outerHtml());
      }

      return description.toString();
   }

   private Prices crawlPrices(Float price, Element e) {
      Prices prices = new Prices();

      if (price != null) {
         prices.setBankTicketPrice(price);
         prices.setPriceFrom(crawlPriceFrom(e));

         Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
         installmentPriceMapShop.put(1, price);

         Pair<Integer, Float> pairShopCards =
               CrawlerUtils.crawlSimpleInstallment(".card .installment-payment strong, .price-carrefour .prince-product-blue", e, false, "x");
         if (!pairShopCards.isAnyValueNull()) {

            installmentPriceMapShop.put(pairShopCards.getFirst(), pairShopCards.getSecond());
         }

         prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShop);

         Map<Integer, Float> installmentPriceMap = new HashMap<>();
         installmentPriceMap.put(1, price);

         Pair<Integer, Float> pairNormalCards = CrawlerUtils.crawlSimpleInstallment(".installment span:last-child", e, false, "x");
         if (!pairNormalCards.isAnyValueNull()) {
            installmentPriceMap.put(pairNormalCards.getFirst(), pairNormalCards.getSecond());
         } else {
            pairNormalCards = CrawlerUtils.crawlSimpleInstallment(".installment .margin-bottom-for-product-promotions:not(:first-child)", e, false, "x");

            if (!pairNormalCards.isAnyValueNull()) {
               installmentPriceMap.put(pairNormalCards.getFirst(), pairNormalCards.getSecond());
            }
         }

         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

         if (pairShopCards.isAnyValueNull()) {
            prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
         }
      }


      return prices;
   }

   private RatingsReviews crawlRatingReviews(Document document) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Map<String, Integer> ratingDistribution = crawlRatingDistribution(document);
      AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValueWithRate(ratingDistribution);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setDate(session.getDate());
      ratingReviews.setTotalRating(computeTotalReviewsCount(ratingDistribution));
      ratingReviews.setAverageOverallRating(crawlAverageOverallRating(document));

      return ratingReviews;
   }

   private Integer computeTotalReviewsCount(Map<String, Integer> ratingDistribution) {
      Integer totalReviewsCount = 0;

      for (String rating : ratingDistribution.keySet()) {
         if (ratingDistribution.get(rating) != null)
            totalReviewsCount += ratingDistribution.get(rating);
      }

      return totalReviewsCount;
   }

   private Double crawlAverageOverallRating(Document document) {
      Double avgOverallRating = null;

      Element avgOverallRatingElement =
            document.select(".sust-review-container .block-review-pagination-bar div.block-rating div.rating.js-ratingCalc").first();
      if (avgOverallRatingElement != null) {
         String dataRatingText = avgOverallRatingElement.attr("data-rating").trim();
         try {
            JSONObject dataRating = new JSONObject(dataRatingText);
            if (dataRating.has("rating")) {
               avgOverallRating = dataRating.getDouble("rating");
            }
         } catch (JSONException e) {
            Logging.printLogWarn(logger, session, "Error converting String to JSONObject");
         }
      }

      return avgOverallRating;
   }

   private Map<String, Integer> crawlRatingDistribution(Document document) {
      Map<String, Integer> ratingDistributionMap = new HashMap<String, Integer>();

      Elements ratingLineElements = document.select("div.tab-review ul.block-list-starbar li");
      for (Element ratingLine : ratingLineElements) {
         Element ratingStarElement = ratingLine.select("div").first();
         Element ratingStarCount = ratingLine.select("div").last();

         if (ratingStarElement != null && ratingStarCount != null) {
            String ratingStarText = ratingStarElement.text();
            String ratingCountText = ratingStarCount.attr("data-star");

            List<String> parsedNumbers = MathUtils.parseNumbers(ratingStarText);
            if (parsedNumbers.size() > 0 && !ratingCountText.isEmpty()) {
               ratingDistributionMap.put(parsedNumbers.get(0), Integer.parseInt(ratingCountText));
            }
         }
      }

      return ratingDistributionMap;
   }

   public static AdvancedRatingReview getTotalStarsFromEachValueWithRate(Map<String, Integer> ratingDistribution) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      for (Map.Entry<String, Integer> entry : ratingDistribution.entrySet()) {

         if (entry.getKey().equals("1")) {
            star1 = entry.getValue();
         }

         if (entry.getKey().equals("2")) {
            star2 = entry.getValue();
         }

         if (entry.getKey().equals("3")) {
            star3 = entry.getValue();
         }

         if (entry.getKey().equals("4")) {
            star4 = entry.getValue();
         }

         if (entry.getKey().equals("5")) {
            star5 = entry.getValue();
         }

      }

      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
   }
}
