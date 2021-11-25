package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BrasilQualidocCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilQualidocCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();

      String path = session.getOriginalURL().replace("https://www.qualidoc.com.br/", "");
      path = URLEncoder.encode(path, StandardCharsets.UTF_8);

      String url = "https://www.qualidoc.com.br/ccstoreui/v1/pages/" + path + "?dataOnly=false&cacheableDataOnly=true&productTypesRequired=false";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.get(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      JSONObject jsonProduct = JSONUtils.getValueRecursive(json, "data.page.product", JSONObject.class);

      if (jsonProduct.has("id")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = jsonProduct.optString("id");
         String name = jsonProduct.optString("displayName");
         CategoryCollection categories = scrapCategories(jsonProduct);
         List<String> images = scrapImages(jsonProduct);
         String primaryImage = !images.isEmpty() ? images.remove(0) : "";
         String description = scrapDescription(jsonProduct);
         boolean availableToBuy = scrapAvailability(internalPid);

         JSONObject jsonOffers = JSONUtils.getValueRecursive(jsonProduct, "listPrices", JSONObject.class);
         Offers offers = availableToBuy ? scrapOffer(jsonProduct, jsonOffers) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
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

   protected CategoryCollection scrapCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoriesArray = json.optJSONArray("parentCategories");

      for (Object o : categoriesArray) {
         JSONObject categorie = (JSONObject) o;

         categories.add(categorie.optString("displayName"));
      }

      return categories;
   }

   protected List<String> scrapImages(JSONObject json) {
      List<String> imgs = new ArrayList<>();

      JSONArray arrayImages = json.optJSONArray("fullImageURLs");
      arrayImages.forEach(x -> imgs.add("https://www.qualidoc.com.br" + x.toString()));

      return imgs;
   }

   protected String scrapDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      description.append("Indicação:\n");
      description.append(json.optString("x_indicacao") + "\n");
      description.append("Modo de Usar:\n");
      description.append(json.optString("x_comoUsar"));

      return description.toString();
   }

   private Offers scrapOffer(JSONObject json, JSONObject jsonOffers) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(json, jsonOffers);

      if (!json.optString("x_valorDeCashback").equals("")) {
         sales.add(json.optString("x_valorDeCashback") + " on cashback");
      }

      if (pricing.getPriceFrom() != null) {
         sales.add(CrawlerUtils.calculateSales(pricing));
      }

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("qualidoc")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject json, JSONObject jsonOffers) throws MalformedPricingException {
      Double priceFrom =  JSONUtils.getDoubleValueFromJSON(jsonOffers, "precoAssociado", false);

      Double spotlightPrice = json.optDouble("salePrice", 0);

      if (spotlightPrice == 0) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

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

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   protected Boolean scrapAvailability(String internalPid) {
      Object stockJson = fetchJsonAvailability(internalPid);
      String stockStatus = JSONUtils.getValueRecursive(stockJson, "items.0.stockStatus", String.class);

      return stockStatus.equals("OUT_OF_STOCK") ? false : true;
   }

   protected Object fetchJsonAvailability(String id) {
      Map<String, String> headers = new HashMap<>();

      String url = "https://www.qualidoc.com.br/ccstoreui/v1/stockStatus?products=" + id;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.get(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }
}
