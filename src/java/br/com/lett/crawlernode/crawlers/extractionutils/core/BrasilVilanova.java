package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

import static java.util.Collections.singletonList;

public class BrasilVilanova extends Crawler {

   public static final String HOME_PAGE = "https://www.vilanova.com.br/";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilVilanova(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
      super.config.setParser(Parser.HTML);
   }

   public String getCnpj() {
      return session.getOptions().optString("cnpj");
   }

   public String getPassword() {
      return session.getOptions().optString("password");
   }

   public String getSellerFullname() {
      return session.getOptions().optString("seller");
   }

   public String getMarket() {
      return session.getOptions().optString("market");
   }

   public String getToken() {
      return session.getOptions().optString("token");
   }

   public String getCookieLogin() {
      return session.getOptions().optString("cookie_login");
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Response loginResponse = new Response();
      try {
         Request request = Request.RequestBuilder.create()
            .setUrl("https://www.vilanova.com.br/loginlett/access/account/token/" + getToken())
            .setProxy(
               getFixedIp()
            )
            .build();
         loginResponse = this.dataFetcher.get(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }

      List<Cookie> cookiesResponse = loginResponse.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
         cookie.setDomain("www.vilanova.com.br");
         cookie.setPath("/");
         this.cookies.add(cookie);
      }
   }

   public LettProxy getFixedIp() throws IOException {
      LettProxy lettProxy = new LettProxy();
      lettProxy.setSource("fixed_ip");
      lettProxy.setPort(3144);
      lettProxy.setAddress("haproxy.lett.global");
      lettProxy.setLocation("brazil");
      return lettProxy;
   }

   @Override
   protected Response fetchResponse() {

      Response response = new Response();
      try {
         Request request = Request.RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setProxy(
               getFixedIp()
            )
            .setCookies(this.cookies)
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

         String jsonString = CrawlerUtils.scrapScriptFromHtml(doc, "#product-options-wrapper > div > script");
         JSONArray jsonArray = JSONUtils.stringToJsonArray(jsonString);
         JSONObject json = JSONUtils.getValueRecursive(jsonArray, "0.[data-role=swatch-options].Magento_Swatches/js/swatch-renderer", JSONObject.class);
         if (json != null && !json.isEmpty()) {
            JSONObject jsonProduct = json.optJSONObject("jsonConfig");

            String internalPid = jsonProduct.optString("productId");
            List<String> eans = singletonList(CrawlerUtils.scrapStringSimpleInfo(doc, ".ean", true));
            String description = CrawlerUtils.scrapSimpleDescription(doc, singletonList(".product.attribute.description"));
            JSONArray variationsArray = getAttributes(jsonProduct, "variant_embalagem");
            String baseName = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title", false);
            for (Object v : variationsArray) {
               JSONObject variation = (JSONObject) v;
               String name = baseName + variation.optString("label");
               JSONObject objectMarket = getObjectMarket(jsonProduct);
               String internalId = findId(variation, objectMarket);
               JSONArray imagesArray = getImages(internalId, jsonProduct);
               String primaryImage = getPrimaryImage(imagesArray);
               List<String> secondaryImages = getSecondaryImages(imagesArray, primaryImage);
               boolean available = objectMarket.optBoolean("status");
               Offers offers = available ? getOffersJson(internalId, jsonProduct) : new Offers();
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setOffers(offers)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
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

   private String getPrimaryImage(JSONArray images) {
      for (Object o : images) {
         JSONObject objImage = (JSONObject) o;
         if (objImage.optBoolean("isMain")) {
            return objImage.optString("img");
         }
      }
      return null;
   }

   private List<String> getSecondaryImages(JSONArray images, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();
      for (Object o : images) {
         JSONObject objImage = (JSONObject) o;
         String image = objImage.optString("img");
         if (image != null && !image.isEmpty()) {
            secondaryImages.add(image);
         }
      }
      if (primaryImage != null && !primaryImage.isEmpty() && secondaryImages.size() > 0) {
         secondaryImages.remove(primaryImage);
      }
      return secondaryImages;
   }

   private JSONArray getImages(String id, JSONObject json) {
      JSONObject jsonImage = json.optJSONObject("images");
      return jsonImage.optJSONArray(id);
   }

   private Pricing scrapPricingJSON(JSONObject json) throws MalformedPricingException {
      JSONObject oldPrice = json.optJSONObject("oldPrice");
      JSONObject finalPrice = json.optJSONObject("finalPrice");
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(oldPrice, "amount", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(finalPrice, "amount", false);

      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private String findId(JSONObject variation, JSONObject objectMarket) {
      List<String> idsProducts = JSONUtils.jsonArrayToStringList(variation.optJSONArray("products"));
      List<String> idsMarket = JSONUtils.jsonArrayToStringList(objectMarket.optJSONArray("products"));
      for (String idProduct : idsProducts) {
         for (String idMarket : idsMarket) {
            if (idProduct.equals(idMarket)) {
               return idProduct;
            }
         }
      }
      return null;
   }

   private Offers getOffersJson(String id, JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      JSONObject objectPrices = JSONUtils.getValueRecursive(json, "optionPrices." + id, JSONObject.class, new JSONObject());
      Pricing pricing = scrapPricingJSON(objectPrices);
      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSellerFullname())
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());


      return offers;
   }

   private JSONObject getObjectMarket(JSONObject jsonObject) {
      JSONArray allMarkets = getAttributes(jsonObject, "variant_seller");
      String idMarket = getMarket();
      for (Object o : allMarkets) {
         JSONObject market = (JSONObject) o;
         String candidateIdMarket = market.optString("id", "");
         if (!candidateIdMarket.isEmpty() && idMarket.equals(candidateIdMarket)) {
            return market;
         }
      }
      return new JSONObject();
   }

   private JSONArray getAttributes(JSONObject json, String attribute) {
      JSONObject attributes = json.optJSONObject("attributes");
      Iterator<String> keys = attributes.keys();
      if (!attributes.isEmpty()) {
         while (keys.hasNext()) {
            String key = keys.next();
            String code = JSONUtils.getValueRecursive(attributes, key + ".code", String.class, "");
            if (attribute.equals(code)) {
               return JSONUtils.getValueRecursive(attributes, key + ".options", JSONArray.class, new JSONArray());
            }
         }
      }
      return new JSONArray();
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-info-basic").isEmpty();
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
