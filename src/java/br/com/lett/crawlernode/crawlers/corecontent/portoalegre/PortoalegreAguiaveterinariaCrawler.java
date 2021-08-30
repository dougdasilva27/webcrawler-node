package br.com.lett.crawlernode.crawlers.corecontent.portoalegre;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PortoalegreAguiaveterinariaCrawler extends Crawler {

   private static final String MAIN_SELLER_NAME = "Águia Veterinária";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   public PortoalegreAguiaveterinariaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();


      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject script = CrawlerUtils.selectJsonFromHtml(doc, "script[type='application/ld+json']", null, null, false, false);
         if (script != null) {
            String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoId", "value");
            int n = 0;
            String name = script.optString("name");
            JSONArray image = script.optJSONArray("image");
            String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".informacao-abas#tp1"));
            JSONArray offersArray = script.optJSONArray("offers");
            if (!offersArray.isEmpty()) {
               for (Object o : offersArray) {
                  if (o instanceof JSONObject) {
                     JSONObject productInfo = (JSONObject) o;
                     String primaryImage = (String) image.get(n);
                     String internalId = productInfo.optString("mpn");
                     String availability = productInfo.optString("availability");
                     boolean available = availability != null && availability.contains("InStock");
                     Offers offers = available ? scrapOffers(productInfo) : new Offers();

                     // Creating the product
                     Product product = ProductBuilder.create()
                        .setUrl(session.getOriginalURL())
                        .setInternalId(internalId)
                        .setInternalPid(internalPid)
                        .setName(name)
                        .setPrimaryImage(primaryImage)
                        .setDescription(description)
                        .setOffers(offers)
                        .build();

                     products.add(product);
                  }
                  n++;
               }
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".main-product").isEmpty();
   }

   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(productInfo);

      if (pricing != null) {
         offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {
      String price = productInfo.optString("price");
      Double spotlightPrice = Double.parseDouble(price);
      Double priceFrom = null;
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

}
