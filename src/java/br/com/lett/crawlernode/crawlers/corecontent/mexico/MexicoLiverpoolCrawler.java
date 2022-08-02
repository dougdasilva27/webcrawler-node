package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class MexicoLiverpoolCrawler extends Crawler {

   public MexicoLiverpoolCrawler(Session session) {
      super(session);
   }

   protected Object fetch() {

      Map<String, String> headers = new HashMap<>();
      headers.put("Host", "www.liverpool.com.mx");
      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate");
      headers.put("Accept-Language", "en-US,en;q=0.5");

      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setCookies(this.cookies)
         .setUrl(session.getOriginalURL())
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY
         ))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "get");

      return Jsoup.parse(response.getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);
      JSONObject productJson = JSONUtils.getValueRecursive(pageJson, "query.data.mainContent.records.0.allMeta", JSONObject.class, new JSONObject());


      if (!productJson.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productJson.optString("id");
         String internalPid = internalId;
         String description = productJson.optString("productDescription");
         String primaryImage = JSONUtils.getValueRecursive(productJson, "variants.0.largeImage", String.class, null);
         JSONArray secondaryImagesJson = JSONUtils.getValueRecursive(productJson, "variants.0.galleriaImages", JSONArray.class, new JSONArray());
         List<String> secondaryImages = CrawlerUtils.scrapImagesListFromJSONArray(secondaryImagesJson, null, null, "https", "ss357.liverpool.com.mx", session);
         CategoryCollection categories = crawlCategories(productJson);
         String name = productJson.optString("title");
         RatingsReviews ratingsReviews = crawlRatingsReviews(productJson);

         Offers offers = scrapOffers(productJson);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setRatingReviews(ratingsReviews)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private RatingsReviews crawlRatingsReviews(JSONObject productJson) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      JSONObject ratingsReviewsJson = JSONUtils.getJSONValue(productJson, "ratingInfo");
      if (ratingsReviewsJson != null) {
         ratingsReviews.setTotalRating(ratingsReviewsJson.optInt("productRatingCount", 0));
         ratingsReviews.setAverageOverallRating(ratingsReviewsJson.optDouble("productAvgRating", 0));
      }

      return ratingsReviews;

   }

   private CategoryCollection crawlCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray categoriesJson = productJson.optJSONArray("categoryBreadCrumbs");

      for (int i = 0; i < categoriesJson.length(); i++) {
         categories.add(categoriesJson.optJSONObject(i).optString("categoryName"));
      }

      return categories;
   }

   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("liverpool")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      JSONObject pricesJson = JSONUtils.getValueRecursive(productJson, "variants.0.prices", JSONObject.class, new JSONObject());

      Double priceFrom = CrawlerUtils.getDoubleValueFromJSON(pricesJson, "listPrice", true, false);
      Double spotlightPrice = getSpotlightPrice(pricesJson);

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

   private Double getSpotlightPrice(JSONObject pricesJson) {
      Double spotlightPrice = CrawlerUtils.getDoubleValueFromJSON(pricesJson, "salePrice", true, false);
      Integer discount = CrawlerUtils.getIntegerValueFromJSON(pricesJson, "discountPercentage", 0);

      if (discount > 0) {
         spotlightPrice = spotlightPrice - (spotlightPrice * discount) / 100;
      }

      return spotlightPrice;

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

}
