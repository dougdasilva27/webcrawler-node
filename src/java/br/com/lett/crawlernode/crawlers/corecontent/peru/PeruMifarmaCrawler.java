package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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

import java.util.*;

public class PeruMifarmaCrawler extends Crawler {

   protected String DRUGSTORE_STOCK;

   public PeruMifarmaCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.JSOUP);
      DRUGSTORE_STOCK = this.session.getOptions().optString("drugstore-stock");
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("drugstore-stock", DRUGSTORE_STOCK);

      String internalId = CommonMethods.getLast(session.getOriginalURL().split("/"));

      String url = "https://td2fvf3nfk.execute-api.us-east-1.amazonaws.com/MFPRD/product/" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response apiResponse = this.dataFetcher.get(session, request);

      return JSONUtils.stringToJson(apiResponse.getBody());
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("id")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = json.optString("id");
         String name = json.optString("name");
         List<String> images = scrapImages(json);
         String primaryImage = !images.isEmpty() ? images.remove(0) : "";
         String description = scrapDescription(json);
         CategoryCollection categories = scrapCategories(json);

         boolean available = json.optString("productStatus").equals("AVAILABLE");
         Offers offers = available ? scrapOffers(json) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapImages(JSONObject json){
      List<String> images = new ArrayList<>();

      JSONArray arr = json.optJSONArray("imageList");

      if(!arr.isEmpty()){
         //The last item in the array contains the large image url
         JSONObject imagesObj = (JSONObject) arr.get(arr.length() - 1);
         images.add(imagesObj.optString("url"));
         JSONArray imagesArray = imagesObj.optJSONArray("thumbnails");
         imagesArray.forEach(x -> images.add(x.toString()));
      }

      return images;
   }

   private CategoryCollection scrapCategories(JSONObject json){
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoriesArray = json.optJSONArray("categoryList");

      if(!categoriesArray.isEmpty()){
         for (Object obj : categoriesArray) {
            JSONObject category = (JSONObject) obj;
            categories.add(category.optString("name"));
         }
      }

      return categories;
   }

   private String scrapDescription(JSONObject json){
      StringBuilder description = new StringBuilder();

      JSONArray descriptionArray = json.optJSONArray("details");

      if(!descriptionArray.isEmpty()){
         for (Object obj : descriptionArray) {
            JSONObject item = (JSONObject) obj;
            description.append(item.optString("title"));
            description.append(":\n");
            description.append(item.optString("content"));
            description.append("\n");
         }
      }

      return description.toString();
   }

   protected Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();

      if(pricing.getPriceFrom() != null){
         sales.add(CrawlerUtils.calculateSales(pricing));
      }

      if(json.has("priceWithpaymentMethod") && json.optDouble("priceWithpaymentMethod") != 0d){
         sales.add("Price with especial payment method: " + json.optDouble("priceWithpaymentMethod"));
      }

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("mi farma")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "priceAllPaymentMethod", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "price", false);

      if(spotlightPrice.equals(0d)){
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlighPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlighPrice)
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
