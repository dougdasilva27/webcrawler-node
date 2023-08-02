package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class ArgentinaLacoopeencasaCrawler extends Crawler {

   public ArgentinaLacoopeencasaCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private static final String SELLER_FULL_NAME = "La coope em casa";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());
   private String cookieSecurity = null;
   private final String locationCookie = getLocation();

   protected String getLocation() {
      return session.getOptions().optString("location_cookie");
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "*/*");
      headers.put("Host", "www.lacoopeencasa.coop");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.lacoopeencasa.coop/ws/index.php/comun/autentificacionController/autentificar_invitado")
         .setHeaders(headers)
         .setPayload("{}")
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR))
         .build();

      Response response = new FetcherDataFetcher().post(session, request);
      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("_lcec_sid_inv")) {
            this.cookieSecurity = cookieResponse.getValue();
         }
      }
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "application/json, text/plain, */*");
      headers.put("Host", "www.lacoopeencasa.coop");
      headers.put("Cookie", "_lcec_sid_inv=" + this.cookieSecurity + ";_lcec_linf=" + locationCookie + ";");

      Response response = fetchProductResponse(headers, "false");
      JSONObject json = CrawlerUtils.stringToJson(response.getBody());

      if (json.optString("mensaje").equals("No existe el articulo")) {
         response = fetchProductResponse(headers, "true"); //if product unavailable simple must be true so the field datos has information
      }

      return response;
   }

   private Response fetchProductResponse(Map<String, String> headers, String simple) {
      String urlRequest = assemblyUrl(simple);
      Request request = Request.RequestBuilder.create()
         .setUrl(urlRequest)
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR))
         .build();

      Response response = new FetcherDataFetcher().get(session, request);
      return response;
   }

   private String assemblyUrl(String simple) {
      String id = session.getOriginalURL().substring(session.getOriginalURL().lastIndexOf("/") + 1);
      if (id.contains("?")) {
         id = id.substring(0, id.lastIndexOf("?"));
      }
      String requestUrl = "https://www.lacoopeencasa.coop/ws/index.php/articulo/articuloController/articulo_detalle?cod_interno=" + id + "&simple=" + simple;
      return requestUrl;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject productJson = json.optJSONObject("datos");

      if (productJson != null) {
         String internalId = productJson.optString("cod_interno");
         String name = productJson.optString("descripcion");

         boolean isAvailable = setAvailability(productJson);
         List<String> images = isAvailable ? scrapImagesArray(productJson) : null;
         String primaryImage = scrapPrimaryImage(images, productJson);

         String description = productJson.optString("desc_larga");
         CategoryCollection categories = scrapCategories(productJson);

         Offers offers = isAvailable ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapPrimaryImage(List<String> images, JSONObject productJson) {
      if (images == null) {
         return productJson.optString("imagen");
      }
      return !images.isEmpty() ? images.remove(0) : null;
   }

   private List<String> scrapImagesArray(JSONObject productJson) {
      List<String> images = new ArrayList<>();
      JSONArray imagesJson = productJson.optJSONArray("imagenes");
      if (imagesJson != null) {
         images = CrawlerUtils.scrapImagesListFromJSONArray(imagesJson, "imagen", null, "", "", session);
      }
      return images;
   }

   private boolean setAvailability(JSONObject productJson) {
      if (productJson.has("disponibilidad")) {
         return productJson.optBoolean("disponibilidad");
      }
      int stock = productJson.optInt("stock");
      return stock > 0;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

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

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "precio", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(product, "precio_anterior", true);

      Double bankslipDiscount = Double.valueOf(JSONUtils.getIntegerValueFromJSON(product, "percent_boleto", 0));

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(bankslipDiscount).build())
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

   private CategoryCollection scrapCategories(JSONObject product) {
      CategoryCollection categories = new CategoryCollection();

      String category1 = product.optString("categoria_inicial_desc");
      String category2 = product.optString("categoria_secundaria_desc");
      String category3 = product.optString("categoria_terciaria_desc");

      categories.add(category1);
      categories.add(category2);
      categories.add(category3);

      return categories;
   }
}
