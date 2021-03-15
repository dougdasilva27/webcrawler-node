package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * @author gabriel date: 2019-09-24
 */
public abstract class BrasilSitemercadoCrawler extends Crawler {

   public BrasilSitemercadoCrawler(Session session) {
      super(session);
   }

   private static final Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.ELO.toString());

   private final String API_URL = getApiUrl();
   private static final String MAIN_SELLER_NAME = "Sitemercado";
   private String homePage = getHomePage();
   private String loadPayload = getLoadPayload();
   private Map<String, Integer> lojaInfo = getLojaInfo();

   protected abstract String getHomePage();
   protected String getApiUrl(){
      return "https://sitemercado-b2c-api-whitelabel.azurefd.net/api/v1/b2c/";
   }

   protected abstract Map<String, Integer> getLojaInfo();

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
   protected JSONObject fetch() {
      return crawlProductInformatioFromApi(session.getOriginalURL());
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      super.extractInformation(jsonSku);
      List<Product> products = new ArrayList<>();

      if (jsonSku.has("idLojaProduto")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         CategoryCollection categories = crawlCategories(jsonSku);
         String description = crawlDescription(jsonSku);
         Integer stock = jsonSku.has("quantityStock") && jsonSku.get("quantityStock") instanceof Integer ? jsonSku.getInt("quantityStock") : null;
         boolean available = jsonSku.has("isSale") && !jsonSku.isNull("isSale") && jsonSku.getBoolean("isSale");
         Offers offers = available ? scrapOffers(jsonSku) : new Offers();
         String name = crawlName(jsonSku);
         JSONArray imagensFromArray = JSONUtils.getValueRecursive(jsonSku, "images", JSONArray.class);
         List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(imagensFromArray, "img", null, "https", "img.sitemercado.com.br", session);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
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


   /*******************
    * General methods *
    *******************/

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("idLojaProduto")) {
         internalId = json.get("idLojaProduto").toString();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject json) {
      String internalId = null;

      if (json.has("idProduct")) {
         internalId = json.get("idProduct").toString();
      }

      return internalId;
   }

   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("excerpt")) {
         name = json.getString("excerpt");
      }

      return name;
   }

   protected Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);

      if (pricing != null) {
         offers.add(OfferBuilder.create()
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

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = crawlPrice(json);

      if (spotlightPrice != null) {
         Double priceFrom = crawlPriceFrom(json);
         CreditCards creditCards = scrapCreditCards(spotlightPrice);

         return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
            .build();
      }

      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   private Double crawlPrice(JSONObject json) {
      Double price = null;

      if (json.has("unit")) {
         String unit = json.get("unit").toString().replace("null", "").trim();

         if (json.has("prices")) {
            JSONArray prices = json.getJSONArray("prices");

            for (Object obj : prices) {
               JSONObject priceJson = (JSONObject) obj;

               if (!unit.isEmpty() && priceJson.has("unit") && !unit.equals(priceJson.get("unit").toString().trim())) {
                  continue;
               }

               if (priceJson.has("price")) {
                  Object pObj = priceJson.get("price");

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

   private Double crawlPriceFrom(JSONObject json) {
      Double price = null;

      if (json.has("price_old")) {
         Object pObj = json.get("price_old");

         if (pObj instanceof Double) {
            price = MathUtils.normalizeTwoDecimalPlaces((Double) pObj);

            if (price == 0d) {
               price = null;
            }
         }
      }

      return price;
   }


   protected CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      if (json.has("department")) {
         String category = json.get("department").toString().replace("null", "").trim();

         if (!category.isEmpty()) {
            categories.add(category);
         }
      }

      if (json.has("category")) {
         String category = json.get("category").toString().replace("null", "").trim();

         if (!category.isEmpty()) {
            categories.add(category);
         }
      }

      if (json.has("subCategory")) {
         String category = json.get("subCategory").toString().replace("null", "").trim();

         if (!category.isEmpty()) {
            categories.add(category);
         }
      }

      return categories;
   }


   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("description")) {
         description.append(json.get("description"));
      }

      return description.toString();
   }


   /**
    * Get the json of gpa api, this api has all info of product
    *
    * @return
    */
   protected JSONObject crawlProductInformatioFromApi(String productUrl) {
      String lojaUrl = CommonMethods.getLast(getHomePage().split("sitemercado.com.br"));
      String loadUrl = API_URL+"page/store"+lojaUrl;
      String url = API_URL+getLojaInfo().get("IdLoja")+"/product/" + CommonMethods.getLast(productUrl.split("/")).split("\\?")[0];

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.REFERER, productUrl);
      headers.put(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");

      Request request = RequestBuilder.create().setUrl(loadUrl).setCookies(cookies).setHeaders(headers).setPayload(loadPayload).build();
      Map<String, String> responseHeaders = new ApacheDataFetcher().get(session, request).getHeaders();

      JSONObject jsonObject = responseHeaders != null ? JSONUtils.stringToJson(responseHeaders.get("sm-token")) : new JSONObject();
      // jsonObject.remove("IdLoja");
      // jsonObject.remove("IdRede");
      jsonObject.put("IdLoja", lojaInfo.get("IdLoja"));
      jsonObject.put("IdRede", lojaInfo.get("IdRede"));
      headers.put("sm-token", jsonObject.toString());
      headers.put("sm-mmc", responseHeaders.get("sm-mmc"));
      headers.put(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9,pt-BR;q=0.8,pt;q=0.7");
      Request requestApi = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      JSONObject jsonApi = CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestApi).getBody());

      //Some products have not yet migrated to the new API and it is necessary to use the old API
      if(jsonApi.isEmpty()){
         requestApi.setUrl("https://www.sitemercado.com.br/api/b2c/product/"+productUrl+"?id_loja="+lojaInfo.get("IdLoja"));
         jsonApi = CrawlerUtils.stringToJson(this.dataFetcher.get(session, requestApi).getBody());
      }
      
      return jsonApi;
   }

}
