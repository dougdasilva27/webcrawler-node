package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.HttpClientFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeruInkafarmaCrawler extends Crawler {

   public static final String GOOGLE_KEY = "AIzaSyC2fWm7Vfph5CCXorWQnFqepO8emsycHPc";
   private final String grammatureRegex = "(\\d+[.,]?\\d*\\s?)(ml|l|g|gr|mg|kg)";
   private final String quantityRegex = "(\\d+[.,]?\\d*\\s?)(und|un)";
   private static final String SELLER_NAME = "inkafarma";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString());

   private final String storeID = getStoreId();

   protected String getStoreId() {
      return session.getOptions().optString("store_id");
   }


   public PeruInkafarmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      JSONObject skuJson = new JSONObject();
      String parameterSku = getSkuFromUrl(session.getOriginalURL());

      if (parameterSku != null) {
         Map<String, String> headersToken = new HashMap<>();
         headersToken.put(HttpHeaders.CONTENT_TYPE, "application/json");

         Request requestToken = RequestBuilder.create()
            .setUrl("https://www.googleapis.com/identitytoolkit/v3/relyingparty/signupNewUser?key=" + GOOGLE_KEY)
            .setPayload("{\"returnSecureToken\":true}")
            .setProxyservice(Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            ))
            .setHeaders(headersToken)
            .build();

         Response responseToken = CrawlerUtils.retryRequestWithListDataFetcher(requestToken, List.of(new HttpClientFetcher(), new ApacheDataFetcher()), session, "post");
         JSONObject apiTokenJson = JSONUtils.stringToJson(responseToken.getBody());

         if (apiTokenJson.has("idToken") && !apiTokenJson.isNull("idToken")) {
            String accesToken = apiTokenJson.get("idToken").toString();

            Map<String, String> headers = new HashMap<>();
            headers.put("x-access-token", accesToken);
            headers.put("AndroidVersion", "100000");
            headers.put("content-type", "application/json");
            headers.put(HttpHeaders.REFERER, session.getOriginalURL());
            if (storeID != null) {
               headers.put("drugstore-stock", storeID);
            }

            String payload = "{\"departmentsFilter\":[],\"categoriesFilter\":[],\"subcategoriesFilter\":[],\"brandsFilter\":[]," +
               "\"ranking\":0,\"page\":0,\"rows\":8,\"order\":\"ASC\",\"sort\":\"ranking\",\"productsFilter\":[\"" + parameterSku + "\"]}";

            Request request = RequestBuilder.create()
               .setUrl("https://5doa19p9r7.execute-api.us-east-1.amazonaws.com/MMPROD/filtered-products")
               .setHeaders(headers)
               .setPayload(payload)
               .mustSendContentEncoding(false)
               .setProxyservice(Arrays.asList(
                  ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
                  ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
                  ProxyCollection.NETNUT_RESIDENTIAL_ES,
                  ProxyCollection.NETNUT_RESIDENTIAL_BR
               ))
               .build();

            Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session, "post");
            skuJson = JSONUtils.stringToJson(response.getBody());
         }
      }

      return skuJson;
   }

   private String getSkuFromUrl(String url) {
      String parameterSku = null;

      if (url.contains("?") && url.contains("sku=")) {
         for (String parameter : CommonMethods.getLast(url.split("\\?")).split("&")) {
            if (parameter.startsWith("sku=")) {
               parameterSku = CommonMethods.getLast(parameter.split("="));
            }
         }
      } else if (url.contains("/")) {
         parameterSku = CommonMethods.getLast(url.split("/")).split("\\?")[0];
      }

      return parameterSku;
   }

   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      List<Product> products = new ArrayList<>();

      if (jsonSku != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productJson = JSONUtils.getValueRecursive(jsonSku, "rows.0", JSONObject.class);

         String internalId = productJson.optString("id");
         String internalPid = internalId;
         String name = scrapName(productJson);
         String description = productJson.optString("longDescription");

         String primaryImage = JSONUtils.getValueRecursive(productJson, "imageList.0.url", String.class);
         CategoryCollection categories = scrapCategories(productJson);
         List<String> secondaryImages = scrapSecondaryImages(productJson, primaryImage);
         boolean available = productJson.optString("productStatus").equalsIgnoreCase("AVAILABLE");
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapSecondaryImages(JSONObject data, String primaryImage) {
      JSONArray array = JSONUtils.getValueRecursive(data, "imageList", JSONArray.class);
      List<String> list = new ArrayList<>();
      if (array != null && !array.isEmpty()) {
         JSONArray thumbnails = JSONUtils.getValueRecursive(array, "0.thumbnails", JSONArray.class);
         if (thumbnails != null && !thumbnails.isEmpty()) {
            for (Integer i = 0; i < thumbnails.length(); i++) {
               String image = (String) thumbnails.get(i);
               if (image != null && !image.equals(primaryImage)) {
                  list.add(image);
               }
            }
         }

      }


      return list;
   }

   private String scrapName(JSONObject productJson) {
      String name = productJson.optString("name");
      String quantity = productJson.optString("noFractionatedText");

      if (!stringHasPattern(name, grammatureRegex) && stringHasPattern(quantity, grammatureRegex)) {
         name += " " + extractPattern(quantity, grammatureRegex);
      }

      if (!stringHasPattern(name, quantityRegex) && stringHasPattern(quantity, quantityRegex)) {
         name += " " + extractPattern(quantity, quantityRegex);
      }

      return name;
   }

   private boolean stringHasPattern(String string, String patternRegex) {
      Pattern pattern = Pattern.compile(patternRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(string);

      return matcher.find();
   }

   private String extractPattern(String string, String patternRegex) {
      Pattern pattern = Pattern.compile(patternRegex, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(string);

      return matcher.find() ? matcher.group(0) : "";
   }

   private CategoryCollection scrapCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();

      String categorie = JSONUtils.getValueRecursive(productJson, "categoryList.0.name", String.class);
      categories.add(categorie);

      return categories;
   }

   private Offers scrapOffers(JSONObject productJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productJson, "priceAllPaymentMethod", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(productJson, "price", true);
      if (spotlightPrice == null || spotlightPrice == 0) {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(productJson, "price", true);
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create().setSpotlightPrice(spotlightPrice).setPriceFrom(priceFrom).setCreditCards(creditCards).build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create().setInstallmentNumber(1).setInstallmentPrice(spotlightPrice).build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create().setBrand(card).setInstallments(installments).setIsShopCard(false).build());
      }

      return creditCards;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }
}
