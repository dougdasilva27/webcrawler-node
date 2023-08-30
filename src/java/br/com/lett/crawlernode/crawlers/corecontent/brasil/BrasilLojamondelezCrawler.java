package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class BrasilLojamondelezCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.lojamondelez.com.br/";
   private static final String IMAGES_HOST = "images-mondelez.ifcshop.com.br";
   private static final String SELLER_NAME_LOWER = "loja mondelez brasil";
   private static final String LOGIN_URL = "https://www.lojamondelez.com.br/Cliente/Logar";
   private static final String ADMIN_URL = "https://www.lojamondelez.com.br/VendaAssistida/login";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilLojamondelezCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
      super.config.setParser(Parser.HTML);
   }

   private final String PASSWORD = getPassword();
   private final String CNPJ = getCnpj();
   private final String MASTER_USER = getMasterUser();

   protected String getMasterUser() {
      return session.getOptions().optString("master_user");
   }

   protected String getPassword() {
      return session.getOptions().optString("password");
   }

   protected String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   public LettProxy getFixedIp() throws IOException {

      LettProxy lettProxy = new LettProxy();
      lettProxy.setSource("fixed_ip");
      lettProxy.setPort(3144);
      lettProxy.setAddress("haproxy.lett.global");
      lettProxy.setLocation("brazil");

      return lettProxy;
   }

   private String cookiePHPSESSID = null;

   private void loginMasterAccount() {
      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put(HttpHeaders.REFERER, "https://www.lojamondelez.com.br/");
      Response response = new Response();
      String payloadString = "usuario=" + this.MASTER_USER + "&Senha=" + this.PASSWORD;
      try {
         Request request = RequestBuilder.create()
            .setUrl(ADMIN_URL)
            .setPayload(payloadString)
            .setHeaders(headers)
            .setProxy(
               getFixedIp()
            )
            .build();

         response = this.dataFetcher.post(session, request);

      } catch (IOException e) {
         e.printStackTrace();
      }
      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            this.cookiePHPSESSID = cookieResponse.getValue();
         }
      }
   }


   @Override
   public void handleCookiesBeforeFetch() {

      loginMasterAccount();
      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      StringBuilder payload = new StringBuilder();
      payload.append("usuario_cnpj=" + this.CNPJ);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
      headers.put("referer", "https://www.lojamondelez.com.br/VendaAssistida");
      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");
      Response response = new Response();
      try {
         Request request = RequestBuilder.create()
            .setUrl(LOGIN_URL)
            .setPayload(payload.toString())
            .setProxy(
               getFixedIp()
            )
            .setHeaders(headers)
            .build();
         response = this.dataFetcher.post(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected Response fetchResponse() {
      try {
         Thread.sleep(5000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "PHPSESSID=" + this.cookiePHPSESSID + ";");


      Response response = new Response();
      try {
         Request request = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setProxy(
               getFixedIp()
            )
            .setHeaders(headers)
            .build();
         response = this.dataFetcher.get(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }

      return response;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONArray productJsonArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "var dataLayer = ", null, false, true);
         JSONObject productJson = extractProductData(productJsonArray);

         String internalPid = crawlInternalPid(productJson);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#Breadcrumbs li a", true);
         String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList("#nav-descricao"));
         List<String> secondaryImages = scrapImages(doc);
         String primaryImage = secondaryImages.remove(0);
         Offers offers = new Offers();

         Elements variations = doc.select(".product-grid-container .sku-variation-content .picking");
         if (!variations.isEmpty()) {
            for (Element obj : variations) {
               String internalId = crawlInternalId(obj);
               String name = crawlName(productJson);
               if (obj.selectFirst("div.picking-quantity span") != null) {
                  name = name + " - " + obj.selectFirst(".picking-quantity span").text();
               }

               offers = isAvailable(obj) ? scrapOffers(obj) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setOffers(offers)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setEans(Arrays.asList(internalId))
                  .build();

               products.add(product);
            }
         } else {
            String internalId = productJson.optString("productSKU");
            String name = productJson.optString("productName");
            boolean isAvailable = productJson.optInt("productStock", 0) > 0;
            offers = isAvailable ? scrapOffersWithNoVariation(productJson) : offers;

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setOffers(offers)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setEans(Arrays.asList(internalId))
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private JSONObject extractProductData(JSONArray productJsonArray) {
      JSONObject productJson = new JSONObject();
      Object firstObjectFromArray = productJsonArray.length() > 0 ? productJsonArray.get(0) : null;

      if (firstObjectFromArray instanceof JSONObject) {
         productJson = (JSONObject) firstObjectFromArray;
      } else if (firstObjectFromArray instanceof JSONArray) {
         JSONArray prankArray = (JSONArray) firstObjectFromArray;
         productJson = prankArray.length() > 0 ? prankArray.getJSONObject(0) : new JSONObject();
      }

      return productJson.has("productData") ? productJson.getJSONObject("productData") : productJson;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".container .product-details") != null;
   }

   private String crawlInternalId(Element obj) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(obj, null, "data-sku-id");

      return internalId;
   }

   private String crawlInternalPid(JSONObject productJson) {
      String internalPid = null;

      if (productJson.has("productID") && !productJson.isNull("productID")) {
         internalPid = productJson.get("productID").toString();
      }

      return internalPid;
   }

   private String crawlName(JSONObject skuJson) {
      String name = null;

      if (skuJson.has("productName") && skuJson.get("productName") instanceof String) {
         name = skuJson.getString("productName");
      }

      return name;
   }

   private List<String> scrapImages(Document doc) {
      List<String> images = new ArrayList<String>();
      Elements imageElements = doc.select("div .product-gallery-thumbnails button");

      if (!imageElements.isEmpty()) {
         imageElements.forEach(e -> images.add(e.select("img").attr("src")));
      }

      return images;
   }

   private boolean isAvailable(Element doc) {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".produto-indisponivel-title", true) == null;
   }

   private Offers scrapOffers(Element doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>(); //no sales was found

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Offers scrapOffersWithNoVariation(JSONObject jsonObject) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricingJson(jsonObject);
      List<String> sales = new ArrayList<>(); //no sales was found

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, null, "data-preco-unit", true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, null, "data-preco-unit", true, ',', session);

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      if (spotlightPrice == 0d) {
         spotlightPrice = null;
      }

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      CreditCards creditCards = scrapCreditcards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private Pricing scrapPricingJson(JSONObject json) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "productOldPrice", true);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "productPrice", true);

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      CreditCards creditCards = scrapCreditcards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditcards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(spotlightPrice);

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      return installments;
   }
}
