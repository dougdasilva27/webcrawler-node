package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChileSantaisabelapoquindoCrawler extends Crawler {

   public ChileSantaisabelapoquindoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSONARRAY);
   }

   private static final String SELLER_FULL_NAME = "Santa Isabel";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   private String storeSaleChannel() {
      return session.getOptions().optString("store-sale-channel");
   }

   @Override
   protected Response fetchResponse() {
      String linkText = scrapUrlProductName();
      Map<String, String> headers = new HashMap<>();
      headers.put("apiKey", "WlVnnB7c1BblmgUPOfg");
      headers.put("x-account", "apoquindo");
      headers.put("x-consumer", "santaisabel");
      String apiUrl = "https://sm-web-api.ecomm.cencosud.com/catalog/api/v1/apoquindo/product/" + linkText + "?sc=" + storeSaleChannel();
      Request request = new Request.RequestBuilder()
         .setUrl(apiUrl)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.LUMINATI_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
         ))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), this.dataFetcher, new JsoupDataFetcher(), new ApacheDataFetcher()), session, "get");
      return response;
   }

   @Override
   public List<Product> extractInformation(JSONArray responseJson) throws Exception {
      super.extractInformation(responseJson);
      List<Product> products = new ArrayList<>();
      JSONObject json = (JSONObject) responseJson.get(0);

      if (json.has("productId")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = JSONUtils.getValueRecursive(json, "items.0.itemId", String.class);
         String internalPid = json.optString("productId");
         String name = scrapName(json);
         CategoryCollection categories = scrapCategories(json);
         String description = scrapDescription(json);
         String primaryImage = JSONUtils.getValueRecursive(json, "items.0.images.0.imageUrl", String.class);
         List<String> secondaryImages = getSecondaryImages(json);
         RatingsReviews ratingsReviews = fetchRatingReviews(internalPid);
         Integer stock = JSONUtils.getValueRecursive(json, "items.0.sellers.0.commertialOffer.AvailableQuantity", Integer.class);
         boolean available = stock > 0 ? true : false;
         Offers offers = available ? scrapOffers(json) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setDescription(description)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setRatingReviews(ratingsReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> getSecondaryImages(JSONObject json) {
      List<String> secondaryImages = new ArrayList<>();
      JSONObject images = JSONUtils.getValueRecursive(json, "items.0", JSONObject.class);
      JSONArray imagesList = JSONUtils.getJSONArrayValue(images, "images");
      if (imagesList != null && !imagesList.isEmpty()) {
         for (Object e : imagesList) {
            JSONObject obj = (JSONObject) e;
            String imageUrl = obj.optString("imageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
               secondaryImages.add(imageUrl);
            }
         }
         if (secondaryImages.size() > 0) {
            secondaryImages.remove(0);
         }
      }
      return secondaryImages;
   }

   private String scrapUrlProductName() {
      String regex = "cl\\/(.*)\\/p";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         return matcher.group(1);
      }
      return null;
   }

   protected String scrapName(JSONObject productJson) {
      String name = null;
      if (productJson.has("productName")) {
         name = productJson.optString("productName");
      }
      if (name != null && !name.isEmpty() && productJson.has("brand")) {
         String brand = productJson.optString("brand");
         if (brand != null && !brand.isEmpty() && !checkIfNameHasBrand(brand, name)) {
            name = name + " " + brand;
         }
      }
      return name;
   }

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray categoriesArray = JSONUtils.getJSONArrayValue(product, "categories");
      for (int i = categoriesArray.length() - 1; i >= 0; i--) {
         String path = categoriesArray.get(i).toString();

         if (path.contains("/")) {
            categories.add(CommonMethods.getLast(path.split("/")));
         }
      }

      return categories;
   }

   private String scrapDescription(JSONObject productJson) throws UnsupportedEncodingException {
      String description = JSONUtils.getValueRecursive(productJson, "description", ".", String.class, "");
      if (description.equals("")) {
         String complementName = JSONUtils.getValueRecursive(productJson, "items.0.complementName", ".", String.class, "");
         String nameComplete = JSONUtils.getValueRecursive(productJson, "items.0.nameComplete", ".", String.class, "");

         if (!complementName.isEmpty() && !complementName.equals(nameComplete)) {
            description = description.concat(complementName);
         }
      }

      return description;
   }

   private boolean checkIfNameHasBrand(String brand, String name) {
      String brandStripAccents = StringUtils.stripAccents(brand);
      String nameStripAccents = StringUtils.stripAccents(name);
      return nameStripAccents.toLowerCase(Locale.ROOT).contains(brandStripAccents.toLowerCase(Locale.ROOT));
   }

   private Offers scrapOffers(JSONObject skuInfo) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(skuInfo);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject skuInfo) throws MalformedPricingException {
      Integer spotlightPriceInt = JSONUtils.getValueRecursive(skuInfo, "items.0.sellers.0.commertialOffer.Price", Integer.class);
      if (spotlightPriceInt == null) {
         Double priceDouble = JSONUtils.getValueRecursive(skuInfo, "items.0.sellers.0.commertialOffer.Price", Double.class);
         spotlightPriceInt = priceDouble.intValue();
      }
      spotlightPriceInt = spotlightPriceInt * 100;
      Integer priceFromInt = JSONUtils.getValueRecursive(skuInfo, "items.0.sellers.0.commertialOffer.ListPrice", Integer.class);
      if (priceFromInt == null) {
         Double priceDouble = JSONUtils.getValueRecursive(skuInfo, "items.0.sellers.0.commertialOffer.ListPrice", Double.class);
         priceFromInt = priceDouble.intValue();
      }
      priceFromInt = priceFromInt * 100;
      Double spotlightPrice = spotlightPriceInt / 100.0;
      Double priceFrom = priceFromInt / 100.0;


      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   private RatingsReviews fetchRatingReviews(String internalPid) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      Map<String, String> headers = new HashMap<>();
      headers.put("apiKey", "WlVnnB7c1BblmgUPOfg");
      headers.put("x-account", "santaisabel");
      headers.put("x-consumer", "santaisabel");
      String apiUrl = "https://sm-web-api.ecomm.cencosud.com/catalog/api/v1/reviews/ratings?ids=" + internalPid;

      Request request = new Request.RequestBuilder()
         .setUrl(apiUrl)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(List.of(ProxyCollection.BUY, ProxyCollection.LUMINATI_RESIDENTIAL_BR))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), this.dataFetcher, new JsoupDataFetcher(), new ApacheDataFetcher()), session, "get");

      if (response != null) {
         JSONArray reviewInfo = CrawlerUtils.stringToJsonArray(response.getBody());
         Double avg = JSONUtils.getValueRecursive(reviewInfo, "0.average", Double.class);
         if (avg == null) {
            Integer avgDouble = JSONUtils.getValueRecursive(reviewInfo, "0.average", Integer.class);
            avg = avgDouble.doubleValue();
         }
         Integer count = JSONUtils.getValueRecursive(reviewInfo, "0.totalCount", Integer.class);
         if (avg != null && count != null) {
            ratingsReviews.setTotalRating(count);
            ratingsReviews.setAverageOverallRating(avg.doubleValue());
         }
         return ratingsReviews;
      }
      return null;
   }
}
