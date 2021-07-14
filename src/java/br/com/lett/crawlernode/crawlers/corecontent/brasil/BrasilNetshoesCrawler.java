package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * date: 27/03/2018
 *
 * @author gabriel
 */

public class BrasilNetshoesCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.netshoes.com.br/";
   private static final String MAIN_SELLER_NAME = "Netshoes";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilNetshoesCrawler(Session session) {
      super(session);
   }

   private String userAgent;
   private LettProxy proxyUsed;

   @Override
   public void handleCookiesBeforeFetch() {
      this.userAgent = FetchUtilities.randUserAgent();

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers).build();
      Response response = this.dataFetcher.get(session, request);

      this.proxyUsed = response.getProxyUsed();

      for (Cookie cookieResponse : response.getCookies()) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain(".netshoes.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, this.userAgent);

      Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).setHeaders(headers).setProxy(proxyUsed).build();
      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
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

         JSONObject chaordicJson = crawlChaordicJson(doc);

         String internalPid = crawlInternalPid(chaordicJson);
         CategoryCollection categories = crawlCategories(doc);
         String description = crawlDescription(doc);

         // sku data in json
         JSONArray arraySkus = chaordicJson != null && chaordicJson.has("skus") ? chaordicJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = crawlInternalId(jsonSku);
            String name = crawlName(chaordicJson, jsonSku);
            boolean availableToBuy = jsonSku.has("status") && jsonSku.get("status").toString().equals("available");
            Document docSku = availableToBuy && arraySkus.length() > 1 ? crawlDocumentSku(internalId, jsonSku, doc) : doc;

            Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();
            String primaryImage = crawlPrimaryImage(docSku);
            String secondaryImages = crawlSecondaryImages(docSku);
            RatingsReviews ratingReviews = crawlRating(doc);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
               .setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setRatingReviews(ratingReviews).setOffers(offers).build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.select(".reference").first() != null;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("sku")) {
         internalId = json.getString("sku").trim();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject skuJson) {
      String internalPid = null;

      if (skuJson.has("id")) {
         internalPid = skuJson.get("id").toString();
      }

      return internalPid;
   }

   private RatingsReviews crawlRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".rating-box__numberOfReviews", false, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".rating-box__value", null, false, '.', session);
      AdvancedRatingReview adRating = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(adRating);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      int stars = 0;
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      Elements reviews = doc.select("#reviews > div.reviews__customerFeedback > div.reviews__feedback > div > div.reviews__feedback-reviews-rating > span > i");

      for (Element review : reviews) {

         String reviewText = review.text();
         reviewText = reviewText.replaceAll("[^0-9]", "");
         if (!reviewText.isEmpty()) {
            stars = Integer.parseInt(review.text());
         }

         // On a html this value will be like this: (1)


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

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   private String crawlName(JSONObject chaordicJson, JSONObject skuJson) {
      StringBuilder name = new StringBuilder();

      if (chaordicJson.has("name")) {
         name.append(chaordicJson.getString("name"));

         if (skuJson.has("specs")) {
            JSONObject specs = skuJson.getJSONObject("specs");

            Set<String> keys = specs.keySet();

            for (String key : keys) {
               if (!key.equalsIgnoreCase("color")) {
                  name.append(" " + specs.get(key));
               }
            }
         }
      }

      return name.toString();
   }

   private Document crawlDocumentSku(String internalId, JSONObject skuJson, Document doc) {
      Document docSku = doc;

      if (skuJson.has("specs")) {
         JSONObject specs = skuJson.getJSONObject("specs");

         if (specs.has("size")) {
            String url = "https://www.netshoes.com.br/refactoring/" + internalId;
            String payload = "sizeLabelSelected=" + specs.get("size") + "&isQuickView=false";

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put(HttpHeaders.USER_AGENT, this.userAgent);

            Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setPayload(payload).setProxy(proxyUsed).build();
            String body = this.dataFetcher.get(session, request).getBody();

            if (body != null) {
               docSku = Jsoup.parse(body);

               Element test = docSku.select("div").first();

               if (test == null) {
                  docSku = doc;
               }
            }
         }
      }

      return docSku;
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element image = doc.select(".photo-figure img.zoom").first();

      if (image != null) {
         primaryImage = image.attr("data-large-img-url");

         if (!primaryImage.startsWith("http")) {
            primaryImage = "https:" + primaryImage;
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select(".swiper-slide:not(.active) img");

      for (Element e : images) {
         String image = e.attr("data-src-large").trim();

         if (image.isEmpty()) {
            image = e.attr("src");
         }

         if (!image.startsWith("http")) {
            image = "https:" + image;
         }

         secondaryImagesArray.put(image);
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private String crawlSeller(Document doc) {
      String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-seller-info", true);

      if (sellerName == null) {
         sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".if-available  .product-seller .product-seller-name .product__seller_name span", false);
      }
      if (sellerName == null) {
         sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".if-available  .product-seller .product__seller_name span", false);

      }

      return sellerName != null ? sellerName : MAIN_SELLER_NAME;

   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumb li > a span");

      for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element shortDescription = doc.select(".description").first();
      if (shortDescription != null) {
         description.append(shortDescription.html());
      }

      Element elementInformation = doc.select("#features").first();
      if (elementInformation != null) {
         description.append(elementInformation.html());
      }

      return description.toString();
   }

   private Offers scrapOffers(Document document) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);
      List<String> sales = scrapSales(pricing);

      String sellerName = crawlSeller(document);
      Boolean isMainSeller = sellerName.equalsIgnoreCase(MAIN_SELLER_NAME);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainSeller)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".default-price.reduce", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".default-price span strong", null, false, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice, doc);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }


   private CreditCards scrapCreditCards(Double priceCard, Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      Integer installment = CrawlerUtils.scrapIntegerFromHtml(doc, ".installments-price", true, 0);
      Double value = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".amountInstallments", null, true, ',', session);

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(priceCard)
         .build());

      if (installment != 0 && value != null) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());
      }


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


   private JSONObject crawlChaordicJson(Document doc) {
      JSONObject skuJson = new JSONObject();

      Elements scripts = doc.select("script");

      for (Element e : scripts) {
         String script = e.outerHtml();


         if (script.contains("freedom.metadata.chaordic(")) {
            String token = "loader.js', '";
            int x = script.indexOf(token) + token.length();
            int y = script.indexOf("');", x);

            String json = script.substring(x, y);

            if (json.startsWith("{") && json.endsWith("}")) {
               try {
                  JSONObject chaordic = new JSONObject(json);

                  if (chaordic.has("product")) {
                     skuJson = chaordic.getJSONObject("product");
                  }
               } catch (Exception e1) {
                  Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
               }
            }

            break;
         }
      }

      return skuJson;
   }
}
