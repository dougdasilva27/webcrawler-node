package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
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
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilSempreemcasaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Sempre em casa brasil";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilSempreemcasaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject productInfo = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null,null, false, false);
      JSONObject data = JSONUtils.getValueRecursive(productInfo, "props.pageProps.data", JSONObject.class);

      if (!data.isEmpty()) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productCode = data.optString("ambev_product_code");
         String primaryImage = data.optString("image");
         String name = data.optString("name");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".title__unities"));
         JSONArray variations = data.optJSONArray("packs");

         for (Object o : variations) {

            JSONObject variation = (JSONObject) o;

            String internalId =  productCode + "-" + variation.optString("id");
            int qtd = variation.optInt("unities");
            String variationName = name + " - " +  qtd;

            Offers offer = scrapOffer(variation);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(variationName)
               .setDescription(description)
               .setPrimaryImage(primaryImage)
               .setOffers(offer)
               .build();

            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
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
      Double spotlightPrice = json.optDouble("current_price");
      Double priceFrom = json.optDouble("original_price") != 0d? json.optDouble("original_price"): null;

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
