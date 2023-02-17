package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
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
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BrasilSvicenteCrawler extends Crawler {
   private final HashMap<String, String> headers = new HashMap<>();

   public BrasilSvicenteCrawler(Session session) {

      super(session);
      super.config.setParser(Parser.JSON);
      setHeaders();
   }

   private void setHeaders() {
      this.headers.put(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01");
      this.headers.put(HttpHeaders.REFERER, "https://www.svicente.com.br/pagina-inicial");
      this.headers.put("authority", "www.svicente.com.br");
   }

   private final static String SELLER_FULL_NAME = "SVicente";

   private String getId() {
      String split = CommonMethods.getLast(session.getOriginalURL().split("-"));
      if (split != null && !split.isEmpty()) {
         return CommonMethods.substring(split, "", ".", true);
      }
      return null;
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String store = session.getOptions().optString("store");
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.svicente.com.br/on/demandware.store/Sites-SaoVicente-Site/pt_BR/Stores-SelectStore?storeId=" + store)
         .setHeaders(this.headers)
         .build();
      Response response = new ApacheDataFetcher().get(session, request);
      if (response.getCookies() != null && !response.getCookies().isEmpty()) {
         for (Cookie cookie : response.getCookies()) {
            BasicClientCookie cookieAdd = new BasicClientCookie(cookie.getName(), cookie.getValue());
            cookieAdd.setDomain("www.svicente.com.br");
            cookieAdd.setPath("/");
            this.cookies.add(cookieAdd);
         }

      }
   }

   @Override
   protected Response fetchResponse() {

      Request reqForProduct = Request.RequestBuilder.create()
         .setUrl("https://www.svicente.com.br/on/demandware.store/Sites-SaoVicente-Site/pt_BR/Product-ShowQuickView?pid=" + getId())
         .setHeaders(this.headers)
         .setCookies(this.cookies)
         .build();
      return new ApacheDataFetcher().get(session, reqForProduct);
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      if (json != null && json.has("product")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = getId();
         JSONObject productObject = json.optJSONObject("product");
         String name = productObject.optString("productName");
         List<String> images = scrapImages(productObject);
         String primaryImage = images.size() > 0 ? images.remove(0) : null;
         String description = scrapDescription(productObject);
         boolean available = productObject.optBoolean("available");
         Offers offers = available ? scrapOffer(productObject) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(scrapCategories())
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private List<String> scrapImages(JSONObject json) {
      List<String> images = new ArrayList<>();
      JSONArray objectsImages = JSONUtils.getValueRecursive(json, "images.large", JSONArray.class, new JSONArray());
      for (Object o : objectsImages) {
         JSONObject objImage = (JSONObject) o;
         String imagePath = JSONUtils.getValueRecursive(objImage, "src.disUrl", String.class, null);
         if (imagePath != null && !imagePath.isEmpty()) {
            images.add(imagePath);
         }
      }
      return images;
   }

   protected String scrapDescription(JSONObject json) throws UnsupportedEncodingException {
      StringBuilder description = new StringBuilder();
      description.append("<div>");
      String longDescription = json.optString("longDescription");
      if (longDescription != null && !longDescription.isEmpty()) {
         description.append("<p>" + longDescription + "</p>");
      }
      description.append("</div>");
      return description.toString();
   }

   private Offers scrapOffer(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      String saleDiscount = JSONUtils.getValueRecursive(json, "price.priceDiff.formatted", String.class, null);
      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setSales(saleDiscount != null ? List.of(saleDiscount) : null)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getValueRecursive(json, "price.sales.value", Double.class, null);
      Double priceFrom = JSONUtils.getValueRecursive(json, "price.list.value", Double.class, null);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.GOOD_CARD.toString(), Card.ELO.toString(), Card.HIPERCARD.toString(), Card.MASTERCARD.toString(),
         Card.DINERS.toString(), Card.AMEX.toString(), Card.ALELO.toString(), Card.VR_CARD.toString(), Card.FACIL.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private CategoryCollection scrapCategories() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(this.headers)
         .setCookies(this.cookies)
         .build();
      Response response = new ApacheDataFetcher().get(session, request);
      Document doc = Jsoup.parse(response.getBody());
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadCrumb__option a");
      if (categories != null && !categories.isEmpty()) {
         return categories;
      }
      return new CategoryCollection();
   }
}
