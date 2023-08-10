package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilNestleAteVoceCrawler extends Crawler {

   public BrasilNestleAteVoceCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private static final String SELLER_FULL_NAME = "Nestle Ate voce";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());

   private final String LOGIN = getLogin();

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   private final String PASSWORD = getPassword();

   protected String getPassword() {
      return session.getOptions().optString("password");
   }

   private final String GRAPHQLQUERY = getGraphqlQuery();
   protected String getGraphqlQuery() {
      return session.getOptions().optString("graphql_query");
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = fetchToken();
      String header = "x-authorization";
      String requestURL = getSessionUrl();

      try {
         HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .header(header, headers.get(header))
            .uri(URI.create(requestURL))
            .build();
         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return new Response.ResponseBuilder()
            .setBody(response.body())
            .setLastStatusCode(response.statusCode())
            .build();
      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + requestURL, e);
      }
   }

   protected Map<String, String> fetchToken() {
      HttpResponse<String> response;
      Map<String, String> headers = new HashMap<>();

      String payload = "{\"operationName\":\"signIn\",\"variables\":{\"taxvat\":\"" + LOGIN + "\",\"password\":\"" + PASSWORD + "\", \"chatbot\":null},\"query\":\"mutation signIn($taxvat: String!, $password: String!, $chatbot: String) {\\ngenerateCustomerToken(taxvat: $taxvat, password: $password, chatbot: $chatbot) {\\ntoken\\nis_clube_nestle\\nenabled_club_nestle\\n__typename\\n}\\n}\\n\"}";

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(HttpHeaders.ACCEPT, "*/*")
            .header(HttpHeaders.ORIGIN, "https://www.nestleatevoce.com.br")
            .header(HttpHeaders.REFERER, "https://www.nestleatevoce.com.br/login")
            .header("x-authorization", "")
            .uri(URI.create("https://www.nestleatevoce.com.br/graphql"))
            .build();

         response = client.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (Exception e) {
         throw new RuntimeException("Failed in load document: " + session.getOriginalURL(), e);
      }
      if (response != null) {
         JSONObject objResponseToken = JSONUtils.stringToJson(response.body());
         String token = JSONUtils.getValueRecursive(objResponseToken, "data.generateCustomerToken.token", String.class);
         headers.put("x-authorization", "Bearer " + token);
      }

      return headers;
   }

   private String getSessionUrl() {
      String urlKey = regexUrlKey();

      if (urlKey != null) {
         String variables = "{\"urlKey\":\"" + urlKey + "\"}";
         String variablesEncoded = URLEncoder.encode(variables, StandardCharsets.UTF_8);
         return "https://www.nestleatevoce.com.br/graphql?query=" + GRAPHQLQUERY + "&variables=" + variablesEncoded;
      }

      return "";
   }

   private String regexUrlKey() {
      String regex = "com.br\\/(.*).html";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject data = jsonObject != null ? jsonObject.optJSONObject("data") : null;

      if (data != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productsJson = assertProductJson(data);

         String internalPid = productsJson.optString("sku");
         String name = productsJson.optString("name");
         List<String> images = crawlImages(productsJson);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         String description = productsJson.optString("description");
         List<String> eans = productsJson.optString("ean") != null ? Arrays.asList(productsJson.optString("ean")) : null;
         CategoryCollection categories = crawlCategories(productsJson);

         JSONArray variants = productsJson.optJSONArray("variants");
         if (variants == null) {
            String internalId = productsJson.optString("id");
            boolean available = productsJson.optString("stock_status").equals("IN_STOCK");
            Offers offers = available ? scrapOffers(productsJson) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setCategories(categories)
               .setOffers(offers)
               .setEans(eans)
               .build();

            products.add(product);

         } else {
            for (int i = 0; i < variants.length(); i++) {
               JSONObject variantProduct = JSONUtils.getValueRecursive(variants, i + ".product", JSONObject.class, new JSONObject());
               JSONArray variantAttributes = JSONUtils.getValueRecursive(variants, i + ".attributes", JSONArray.class, new JSONArray());
               String internalId = variantProduct.optString("id", null);
               String variantName = assembleVariantName(variantAttributes, name);

               boolean available = variantProduct.optString("stock_status").equals("IN_STOCK");
               Offers offers = available ? scrapOffers(variantProduct) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(variantName)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .setCategories(categories)
                  .setOffers(offers)
                  .setEans(eans)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private JSONObject assertProductJson(JSONObject data) {
      JSONArray items = JSONUtils.getValueRecursive(data, "products.items", JSONArray.class);

      if (items.length() == 1) {
         return JSONUtils.getValueRecursive(data, "products.items.0", JSONObject.class);
      }
      for (Object item : items) {
         JSONObject itemObj = (JSONObject) item;
         String urlKey = regexUrlKey();
         String objectUrlKey = itemObj.optString("url_key");

         if (urlKey != null && urlKey.equals(objectUrlKey)) {
            return itemObj;
         }
      }
      return null;
   }

   private String assembleVariantName(JSONArray variantAttributes, String name) {
      String label = JSONUtils.getValueRecursive(variantAttributes, "0.label", String.class);
      if (label != null) {
         if (label.equals("CS")) {
            return name + " - CAIXA";
         }
         if (label.equals("DS")) {
            return name + " - DISPLAY";
         }
         if (label.equals("EA")) {
            return name + " - UNIDADE";
         }
         return name + " - " + label;
      }

      return name;
   }

   private CategoryCollection crawlCategories(JSONObject productsJson) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoriesJson = productsJson.optJSONArray("categories");
      for (int i = 0; i < categoriesJson.length(); i++) {
         JSONObject categoryJson = categoriesJson.optJSONObject(i);
         String category = categoryJson.optString("name");
         categories.add(category);
      }

      return categories;
   }

   private List<String> crawlImages(JSONObject productsJson) {
      List<String> images = new ArrayList<>();

      JSONArray imagesArray = productsJson.optJSONArray("media_gallery_entries");
      if (imagesArray != null) {
         for (int i = 0; i < imagesArray.length(); i++) {
            String imageFile = JSONUtils.getValueRecursive(imagesArray, i + ".file", String.class);
            if (imageFile != null) {
               images.add("https://frontend.nestleatevoce.com.br/media/catalog/product" + imageFile);
            }
         }
      }

      return images;
   }

   private Offers scrapOffers(JSONObject variantProduct) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(variantProduct);
      List<String> sales = scrapSales(variantProduct);

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

   private Pricing scrapPricing(JSONObject variantProduct) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.final_price.value", Double.class, null);
      if (spotlightPrice == null) {
         spotlightPrice = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.final_price.value", Integer.class, null).doubleValue();
      }

      Double priceFrom = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.regular_price.value", Double.class, null);
      if (priceFrom == null) {
         priceFrom = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.regular_price.value", Integer.class, null).doubleValue();
      }

      if (priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private List<String> scrapSales(JSONObject variantProduct) {
      List<String> sales = new ArrayList<>();

      Double amount_off = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.discount.amount_off", Double.class, null);
      Double percent_off = JSONUtils.getValueRecursive(variantProduct, "price_range.minimum_price.discount.percent_off", Double.class, null);

      sales.add(String.valueOf(amount_off));
      sales.add(String.valueOf(percent_off));

      return sales;
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
}
