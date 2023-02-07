package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
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

public class SiteMercadoCrawler extends Crawler {

   public SiteMercadoCrawler(Session session) {
      super(session);
      this.config.setParser(Parser.JSON);
   }

   private static final Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.ELO.toString());

   private final String LOAD_API_URL = getLoadApiUrl();
   private final String API_URL = getApiUrl();
   private static final String MAIN_SELLER_NAME = "Sitemercado";
   private String homePage = getHomePage();
   private String loadPayload = getLoadPayload();
   private Map<String, Integer> lojaInfo = getLojaInfo();

   protected String getHomePage() {
      return session.getOptions().optString("url");
   }

   protected String getApiUrl() {
      return "https://www.sitemercado.com.br/api/b2c/";
   }

   protected String getLoadApiUrl() {
      return "https://www.sitemercado.com.br/api/v1/b2c/";
   }

   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> storeMap = new HashMap<>();
      storeMap.put("IdLoja", session.getOptions().optInt("idLoja"));
      storeMap.put("IdRede", session.getOptions().optInt("idRede"));
      return storeMap;
   }

   protected String getLoadPayload() {
      JSONObject payload = new JSONObject();
      String[] split = homePage.split("/");

      payload.put("lojaUrl", CommonMethods.getLast(split));
      payload.put("redeUrl", split[split.length - 2]);

      return payload.toString();
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(homePage);
   }

   @Override
   protected Response fetchResponse() {
      return crawlProductInformatioFromApi(session.getOriginalURL());
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      super.extractInformation(jsonSku);
      List<Product> products = new ArrayList<>();

      if (jsonSku.has("idLojaProduto")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String pruductUrl = session.getOriginalURL().replace("//produto", "/produto");
         String internalId = jsonSku.optString("idProduct");
         String name = jsonSku.optString("excerpt");
         String description = crawlDescription(jsonSku);
         CategoryCollection categories = crawlCategories(jsonSku);

         JSONArray imagensFromArray = JSONUtils.getValueRecursive(jsonSku, "images", JSONArray.class);
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(imagensFromArray, "img", null, "https", "img.sitemercado.com.br", session);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;

         Integer stock = jsonSku.has("quantityStock") && jsonSku.opt("quantitytock") instanceof Integer ? jsonSku.optInt("quantityStock") : null;
         boolean available = jsonSku.has("isSale") && !jsonSku.isNull("isSale") && jsonSku.optBoolean("isSale");
         Offers offers = available ? scrapOffers(jsonSku) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(pruductUrl)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   protected Offers scrapOffers(JSONObject jsonSku) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonSku);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject jsonSku) throws MalformedPricingException {
      Double spotlightPrice = crawlPrice(jsonSku);
      Double spPrice = JSONUtils.getValueRecursive(jsonSku, "prices.0.price", Double.class);
      Double fPrice = JSONUtils.getDoubleValueFromJSON(jsonSku, "price_old", true);

      if (spotlightPrice != null) {
         Double priceFrom = crawlPriceFrom(jsonSku);
         CreditCards creditCards = scrapCreditCards(spotlightPrice);

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
            .build();
      }

      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   private Double crawlPrice(JSONObject jsonSku) {
      Double price = null;

      if (jsonSku.has("unit")) {
         String unit = jsonSku.get("unit").toString().replace("null", "").trim();

         if (jsonSku.has("prices")) {
            JSONArray prices = jsonSku.getJSONArray("prices");

            for (Object obj : prices) {
               JSONObject priceJson = (JSONObject) obj;

               if (!unit.isEmpty() && priceJson.has("unit") && !unit.equals(priceJson.get("unit").toString().trim())) {
                  continue;
               }

               if (priceJson.has("price")) {
                  Object pObj = priceJson.opt("price");

                  if (pObj instanceof Double) {
                     price = MathUtils.normalizeTwoDecimalPlaces(((Double) pObj));

                     if (price == 0d) {
                        price = null;
                     }

                     break;
                  }
               }
            }
         }
      }

      return price;
   }

   private Double crawlPriceFrom(JSONObject jsonSku) {
      Double price = null;

      if (jsonSku.has("price_old")) {
         Object pObj = jsonSku.opt("price_old");

         if (pObj instanceof Double) {
            price = MathUtils.normalizeTwoDecimalPlaces((Double) pObj);

            if (price == 0d) {
               price = null;
            }
         }
      }

      return price;
   }

   protected CategoryCollection crawlCategories(JSONObject jsonSku) {
      CategoryCollection categories = new CategoryCollection();

      if (jsonSku.has("department")) {
         String category = jsonSku.get("department").toString().replace("null", "").trim();

         if (!category.isEmpty()) {
            categories.add(category);
         }
      }

      if (jsonSku.has("category")) {
         String category = jsonSku.get("category").toString().replace("null", "").trim();

         if (!category.isEmpty()) {
            categories.add(category);
         }
      }

      if (jsonSku.has("subCategory")) {
         String category = jsonSku.get("subCategory").toString().replace("null", "").trim();

         if (!category.isEmpty()) {
            categories.add(category);
         }
      }

      return categories;
   }

   private String crawlDescription(JSONObject jsonSku) {
      StringBuilder description = new StringBuilder();

      if (jsonSku.has("description")) {
         description.append(jsonSku.optString("description"));
         description.append(" ");
      }

      if (jsonSku.has("additionalInformation")) {
         description.append(jsonSku.optString("additionalInformation"));
      }

      return description.toString();
   }

   /**
    * Get the json of gpa api, this api has all info of product
    *
    * @return
    */
   protected Response crawlProductInformatioFromApi(String productUrl) {
      String lojaUrl = CommonMethods.getLast(getHomePage().split("sitemercado.com.br"));
      String loadUrl = LOAD_API_URL + "page/store" + lojaUrl;
      String productName = CommonMethods.getLast(productUrl.split("/")).split("\\?")[0];
      String url = API_URL + "product/" + productName + "?store_id=" + lojaInfo.get("IdLoja");

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.REFERER, productUrl);
      headers.put(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(loadUrl)
         .setCookies(cookies)
         .setHeaders(headers)
         .setPayload(loadPayload)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY, ProxyCollection.LUMINATI_RESIDENTIAL_BR))
         .build();


      Map<String, String> responseHeaders = dataFetcher.get(session, request).getHeaders();

      JSONObject jsonObject = responseHeaders != null ? JSONUtils.stringToJson(responseHeaders.get("sm-token")) : new JSONObject();
      jsonObject.put("IdLoja", lojaInfo.get("IdLoja"));
      jsonObject.put("IdRede", lojaInfo.get("IdRede"));
      headers.put("sm-token", jsonObject.toString());
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9,pt-BR;q=0.8,pt;q=0.7");
      Request requestApi = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY, ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY, ProxyCollection.LUMINATI_RESIDENTIAL_BR))
         .build();

      return this.dataFetcher.get(session, requestApi);
   }
}

