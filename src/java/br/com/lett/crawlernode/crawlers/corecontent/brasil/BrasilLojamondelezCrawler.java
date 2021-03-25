package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import models.prices.Prices;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilLojamondelezCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.lojamondelez.com.br/";
   private static final String IMAGES_HOST = "images-mondelez.ifcshop.com.br";

   private static final String LOGIN_URL = "https://www.lojamondelez.com.br/Cliente/Logar";
   private static final String CNPJ = "33033028004090";
   private static final String PASSWORD = "monica08";
   private static final String SELLER_NAME_LOWER = "loja mondelez brasil";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilLojamondelezCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private String cookiePHPSESSID = null;

   @Override
   public void handleCookiesBeforeFetch() {
      StringBuilder payload = new StringBuilder();
      payload.append("usuario_cnpj=" + CNPJ);
      payload.append("&usuario_senha=" + PASSWORD);

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("sec-fetch-mode", "cors");
      headers.put("origin", "https://www.lojamondelez.com.br");
      headers.put("sec-fetch-site", "same-origin");
      headers.put("x-requested-with", "XMLHttpRequest");

      Request request = RequestBuilder.create().setUrl(LOGIN_URL).setPayload(payload.toString()).setHeaders(headers).build();
      Response response = this.dataFetcher.post(session, request);

      List<Cookie> cookiesResponse = response.getCookies();

      for (Cookie cookieResponse : cookiesResponse) {
         if (cookieResponse.getName().equalsIgnoreCase("PHPSESSID")) {
            cookiePHPSESSID = cookieResponse.getValue();
         }
      }
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "PHPSESSID=" + cookiePHPSESSID);

      Request request = RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .build();

      return Jsoup.parse(new ApacheDataFetcher().get(session, request).getBody());
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

         JSONArray productsArray = getSkusList(productJson);

         for (Object obj : productsArray) {
            JSONObject skuJson = (JSONObject) obj;

            String internalId = crawlInternalId(skuJson);
            List<String> eans = Arrays.asList(internalId);
            String name = crawlName(skuJson);
            Offers offers = isAvailable(skuJson) ? scrapOffers(skuJson, doc) : new Offers();

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
               .setEans(eans)
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

   private String crawlInternalId(JSONObject skuJson) {
      String internalId = null;

      if (skuJson.has("sku") && !skuJson.isNull("sku")) {
         internalId = skuJson.get("sku").toString();
      }

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

      if (skuJson.has("name") && skuJson.get("name") instanceof String) {
         name = skuJson.getString("name");
      }

      return name;
   }

   private List<String> scrapImages(Document doc){
      List<String> images = new ArrayList<String>();
      Elements imageElements = doc.select("div .product-gallery-thumbnails button");

      if(!imageElements.isEmpty()){
         imageElements.forEach(e -> images.add(e.select("img").attr("src")));
      }

      return images;
   }

   private JSONArray getSkusList(JSONObject productJson) {
      JSONArray skus = new JSONArray();

      if (productJson.has("productSKUList") && productJson.get("productSKUList") instanceof JSONArray) {
         skus = productJson.getJSONArray("productSKUList");
      } else if (productJson.has("productSKUList") && productJson.get("productSKUList") instanceof JSONObject) {
         JSONObject skusJSON = productJson.getJSONObject("productSKUList");

         for (String key : skusJSON.keySet()) {
            skus.put(skusJSON.get(key));
         }
      }

      return skus;
   }

   private boolean isAvailable(JSONObject jsonObject) {
      return !jsonObject.optString("available").equals("no");
   }

   private Offers scrapOffers(JSONObject json, Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
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

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "old_price", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "price", false);

      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditcards(json, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditcards(JSONObject json, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(json, spotlightPrice);

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(JSONObject json, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      JSONObject installmentJson = json.optJSONObject("installment");

      if (installmentJson != null) {
         Integer installmentNumber = JSONUtils.getIntegerValueFromJSON(installmentJson, "count", 1);
         Double installmentPriceJson = JSONUtils.getDoubleValueFromJSON(installmentJson, "price", false);

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(installmentPriceJson)
            .build());

      } else {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      return installments;
   }
}
