package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FerreiracostaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "ferreira costa";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public FerreiracostaCrawler(Session session) {
      super(session);
   }



   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("eco_lo", "8");
      cookie.setDomain(".ferreiracosta.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".detalhe-produto") != null) {
         Logging
            .printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

         JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "span script[type=\"application/ld+json\"]","", null, false, false);
         JSONObject offersInfo = jsonInfo.optJSONObject("offers");
         System.err.println(jsonInfo);

         String name = jsonInfo.optString("name");
         String internalId = jsonInfo.optString("productID");
         String description = jsonInfo.optString("description");
         String primaryImage = jsonInfo.optString("image");
         boolean available = offersInfo.optString("availability").contains("InStock");

         Offers offers = available ? scrapOffers(offersInfo): new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setOffers(offers)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setDescription(description)
                  .build();

               products.add(product);

      } else {

      }

      return products;
   }


   private Offers scrapOffers(JSONObject offersInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(offersInfo);
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


   private Pricing scrapPricing(JSONObject offersInfo) throws MalformedPricingException {
      Double spotlightPrice = offersInfo.optDouble("price");
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
