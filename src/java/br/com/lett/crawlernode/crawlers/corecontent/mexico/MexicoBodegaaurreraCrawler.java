package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import jdk.jfr.Category;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class MexicoBodegaaurreraCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public MexicoBodegaaurreraCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetch() {
      Map<String, String> headers = new HashMap<>();
      String id = CommonMethods.getLast(session.getOriginalURL().split("_"));

      String url = "https://www.bodegaaurrera.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getProduct?id=" + id;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("product")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject jsonProduct = json.optJSONObject("product");
         JSONObject jsonSku = JSONUtils.getValueRecursive(jsonProduct, "childSKUs.0", JSONObject.class);

         String internalPid = jsonSku.optString("id");
         String name = jsonProduct.optString("displayName");
         CategoryCollection categories = scrapCategories(jsonProduct);
         String primaryImage = "https://www.bodegaaurrera.com.mx" + JSONUtils.getValueRecursive(jsonSku, "images.large", String.class);
         List<String> secondaryImages = scrapSecondaryImages(jsonSku);
         String description = scrapDescription(jsonSku);
         Offers offers = scrapOffers(jsonSku);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
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

      JSONObject categoriesJson = json.optJSONObject("breadcrumb");
      if (categoriesJson != null) {
         categories.add(categoriesJson.optString("departmentName"));
         categories.add(categoriesJson.optString("familyName"));
         categories.add(categoriesJson.optString("fineLineName"));
      }

      return categories;
   }

   protected List<String> scrapSecondaryImages(JSONObject json) {
      List<String> imgsList = new ArrayList<>();

      JSONArray imgsArray = json.optJSONArray("secondaryImages");

      if (imgsArray != null && !imgsArray.isEmpty()) {
         for (Object o : imgsArray) {
            JSONObject imgJson = (JSONObject) o;
            imgsList.add(imgJson.optString("large"));
         }
      }

      return imgsList;
   }

   protected String scrapDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      description.append("Descripción\n");
      description.append(json.optString("seoDescription"));
      description.append("\nCaracterísticas\n");

      JSONObject longDescriptionJson = json.optJSONObject("dynamicFacets");

      Set<String> jsonKeySet = longDescriptionJson.keySet();

      if (!jsonKeySet.isEmpty()) {
         for (String key : jsonKeySet) {
            JSONObject attr = longDescriptionJson.optJSONObject(key);

            description.append(attr.optString("attrDesc") + ": ");
            description.append(attr.optString("value") + "\n");
         }
      }

      return description.toString();
   }

   private Offers scrapOffers(JSONObject jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      JSONArray offersList = jsonObject.optJSONArray("offerList");

      for (Object o : offersList) {
         JSONObject offerJson = (JSONObject) o;

         if (offerJson.optBoolean("isInvAvailable")) {
            List<String> sales = new ArrayList<>();
            Pricing pricing = scrapPricing(offerJson);
            sales.add(CrawlerUtils.calculateSales(pricing));

            boolean isMainSeller = offerJson.optString("offerType").equals("1P");
            String sellerName = offerJson.optString("sellerName");

            offers.add(Offer.OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(true)
               .setSellerFullName(sellerName)
               .setMainPagePosition(1)
               .setIsBuybox(true)
               .setIsMainRetailer(isMainSeller)
               .setPricing(pricing)
               .setSales(sales)
               .build());
         }
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      JSONObject jsonPrices = json.optJSONObject("priceInfo");

      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(jsonPrices, "specialPrice", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(jsonPrices, "originalPrice", false);

      if(priceFrom.equals(spotlightPrice)){
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
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

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
