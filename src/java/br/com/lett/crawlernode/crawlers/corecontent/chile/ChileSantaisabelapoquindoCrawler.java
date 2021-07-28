package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChileSantaisabelapoquindoCrawler extends Crawler {

   public ChileSantaisabelapoquindoCrawler(Session session) {
      super(session);
   }
   private static final String SELLER_FULL_NAME = "Santa Isabel";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject json = scrapJSOMFromHTML(doc);

      if(!json.isEmpty()){

         String internalId = json.optString("sku");
         String internalPid = internalId;
         String name = json.optString("name");
         //CategoryCollection categories = crawlCategories(doc);
         String primaryImage = json.optString("image");
         //String secondaryImages = crawlSecondaryImages(doc);
         String description = json.optString("description");
         Integer stock = null;
         boolean availableToBuy = JSONUtils.getValueRecursive(json,"offers.availability", String.class).contains("InStock");
         Offers offers = availableToBuy? scrapOffer(json): new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);


      }else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;

   }

   private JSONObject scrapJSOMFromHTML(Document doc){
      JSONObject json = new JSONObject();
      Elements scripts = doc.select("script[type=\"application/ld+json\"]");

      for(Element s : scripts){
         String script = s.html();
         if(script.contains("Product")){
            json = CrawlerUtils.stringToJson(script);
         }
      }

      return json;
   }

   private Offers scrapOffer(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();

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


   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      String rawPrice = JSONUtils.getValueRecursive(json,"offers.price", String.class);

      Double spotlightPrice = rawPrice != null ? MathUtils.parseDoubleWithComma(rawPrice): null;
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
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
