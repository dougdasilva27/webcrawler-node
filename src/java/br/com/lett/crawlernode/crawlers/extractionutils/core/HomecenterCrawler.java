package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class HomecenterCrawler extends Crawler {

   private static final String HOST_IMAGE = "http://homecenterco.scene7.com/is/image/";
   private static final String RATING_API_KEY = "te6izc8u11ucvcxkbhpi70cig";
   private static final String RATING_API_URL = "https://api.bazaarvoice.com/data/statistics.json?apiversion=5.4";
   private static final String SELLER_FULL_NAME = "Homecenter";

   protected HomecenterCrawler(Session session) {
      super(session);
   }

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());

   public abstract String getCity();

   public abstract String getCityComuna();

   @Override
   public void handleCookiesBeforeFetch() {
      String comuna = getCityComuna();
      String city = getCity();
      if (city != null) {
         cookies.add(new BasicClientCookie("ZONE_NAME", city.toUpperCase(Locale.ROOT)));
      }
      if (comuna != null) {
         cookies.add(new BasicClientCookie("comuna", comuna));
      }
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String name = crawlName(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumbs.into-pdp .bread-crumb-wrapper:not(:last-child) a span", false);
         String description = crawlDescription(doc);
         JSONObject jsonImages = getJson(internalId);
         List<String> images = crawlImages(jsonImages);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         RatingsReviews ratingReviews = crawlRating(internalId);
         boolean available = crawlAvailability(doc);

         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String crawlDescription(Document doc) {
      String descriptionText = doc.select(".product-info .product-info-details").text();
      StringBuilder description = new StringBuilder();
      Elements elements = doc.select("tbody > tr");
      if (elements != null) {
         for (Element el : elements) {
            description.append(el.selectFirst(".product-tech-specs .row .title"));
            description.append(el.selectFirst(".product-tech-specs .row .value"));
         }
      }

      if (descriptionText != null && !descriptionText.isEmpty()) {
         description.append(descriptionText);
      }

      String iframe = getIframeDescription(doc);

      if (iframe != null) {
         description.append(iframe);
      }

      return description.toString();
   }


   private String getIframeDescription(Document doc) {
      String iframeUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#description iframe", "src");
      if (iframeUrl != null) {
         Request request = Request.RequestBuilder.create()
            .setUrl(iframeUrl)
            .build();
         return this.dataFetcher
            .get(session, request)
            .getBody();
      }

      return "";
   }

   private JSONObject getJson(String internalId) {
      String apiUrl = "https://homecenterco.scene7.com/is/image/SodimacCO/" + internalId + "?req=set,json&handler=s7Res&id=imgSet";
      JSONObject json = new JSONObject();

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();
      if (content.contains("s7Res(")) {
         String[] jsonSplit = content.split("s7Res\\(");
         if (jsonSplit.length > 0) {
            String fistIndex = jsonSplit[1];
            if (fistIndex.contains(",\"imgSet\");")) {
               String jsonString = fistIndex.split(",\"imgSet\"\\);")[0];
               json = CrawlerUtils.stringToJson(jsonString);

            }
         }
      }
      return json;
   }

   private List<String> crawlImages(JSONObject json) {
      List<String> images = new ArrayList<>();
      String linkIncomplete;
      JSONArray jsonImages = JSONUtils.getValueRecursive(json, "set.item", JSONArray.class);
      if (jsonImages != null) {
         for (Object obj : jsonImages) {
            if (obj instanceof JSONObject) {
               JSONObject image = (JSONObject) obj;
               linkIncomplete = JSONUtils.getValueRecursive(image, "i.n", String.class);
               images.add(HOST_IMAGE + linkIncomplete);
            }
         }
      } else {
         linkIncomplete = JSONUtils.getValueRecursive(json, "set.n", String.class);
         images.add(HOST_IMAGE + linkIncomplete);

      }

      return images;
   }

   private RatingsReviews crawlRating(String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      Integer totalNumOfEvaluations = null;
      Double avgRating = null;

      String urlApi = RATING_API_URL + "&PassKey=" + RATING_API_KEY + "&productid=" + "&stats=Reviews&filter=ProductId:" + internalId;
      Request request = Request.RequestBuilder.create().setUrl(urlApi).build();
      JSONObject reviewJson = CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
      JSONObject reviewStatistics = JSONUtils.getValueRecursive(reviewJson, "Results.0.ProductStatistics.ReviewStatistics", JSONObject.class);

      if (reviewStatistics != null) {
         totalNumOfEvaluations = JSONUtils.getIntegerValueFromJSON(reviewStatistics, "TotalReviewCount", 0);
         avgRating = JSONUtils.getDoubleValueFromJSON(reviewStatistics, "AverageOverallRating", true);
      }

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }


   private boolean isProductPage(Document doc) {
      return !doc.select(".product-manual").isEmpty();
   }

   private String crawlInternalId(Document document) {
      String internalPid = null;
      String[] internalIdArray = CrawlerUtils.scrapStringSimpleInfo(document, ".product-info .product-cod", false).split("Código ");
      if (internalIdArray.length > 1) {
         internalPid = internalIdArray[1];
      }

      return internalPid;
   }


   private String crawlName(Document doc) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info .product-title", true);
      String brand = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info .product-brand", true);
      String model = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info .product-model", true);

      String fullName = name != null && brand != null ? brand + " " + name : null;

      if (fullName == null) {
         return null;
      }

      return model != null ? fullName + " " + model : fullName;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      if (scrapSaleDiscount(pricing) != null) {
         sales.add(scrapSaleDiscount(pricing));
      }

      return sales;
   }

   private String scrapSaleDiscount(Pricing pricing) {

      return CrawlerUtils.calculateSales(pricing);
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".primary span:nth-child(2)", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".secondary span:nth-child(2)", null, true, ',', session);
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
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

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private boolean crawlAvailability(Document doc) {
      return doc.selectFirst(".product-info .fallback .cs-icon-add") != null;
   }


}
