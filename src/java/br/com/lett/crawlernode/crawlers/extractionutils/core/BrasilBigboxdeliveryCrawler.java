package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilBigboxdeliveryCrawler extends Crawler {

   private static final String MAIN_SELLER_NAME = "Big-Box-Delivery";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());
   private String STORE_ID = session.getOptions().optString("store_id");


   public BrasilBigboxdeliveryCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject productInfo = scrapProductInfoFromAPI();

      if (!productInfo.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

         String internalId = productInfo.optString("id");
         String internalPid = internalId;
         String name = productInfo.optString("name");
         String primaryImage = "https://assets.instabuy.com.br/ib.item.image.big/b-" + scrapPrimaryImage(productInfo.optJSONArray("images"));
         List<String> secondaryImages = scrapSecondaryImages(productInfo.optJSONArray("images"));
         boolean availableStock = productInfo.optBoolean("available_stock");
         Offers offers = availableStock? scrapOffers(productInfo.optJSONArray("prices")): new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page: " + session.getOriginalURL());
      }
      return products;
   }

   private JSONObject scrapProductInfoFromAPI(){
      JSONObject prodcutInfo = new JSONObject();

      String slug = CommonMethods.getLast(session.getOriginalURL().split("p/"));
      String url = "https://www.bigboxdelivery.com.br/apiv3/item?slug=" + slug + "&store_id=" + STORE_ID;

      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String response = this.dataFetcher.get(session,request).getBody();
      JSONObject json = CrawlerUtils.stringToJson(response);
      JSONArray dataArr = json.optJSONArray("data");

      if(dataArr != null) {
         for (Object o : dataArr) {
            if (o instanceof JSONObject) {
               prodcutInfo = (JSONObject) o;
            }
         }
      }

      return prodcutInfo;
   }

   private String scrapPrimaryImage (JSONArray images){
      String primaryImage = "";

      if ( images != null){
         primaryImage = (String) images.get(0);
      }

      return primaryImage;
   }

   private List<String> scrapSecondaryImages(JSONArray images){
      List<String> secondaryImages = new ArrayList<>();

      if ( images != null){
         for(int i = 0 ; i > images.length(); i++) {
            images.remove(0);
         }
      }
      return secondaryImages;
   }

   private Offers scrapOffers(JSONArray prices) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      if(prices != null) {
         for (Object o : prices) {
            JSONObject price = (JSONObject) o;
            Pricing pricing = scrapPricing(price);

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
         }
      }
      return offers;
   }

   private Pricing scrapPricing(JSONObject price) throws MalformedPricingException {
      Double priceFrom =  price.has("promo_price")?price.optDouble("promo_price"): null;
      Double spotlightPrice = price.optDouble("price");

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();
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

}
