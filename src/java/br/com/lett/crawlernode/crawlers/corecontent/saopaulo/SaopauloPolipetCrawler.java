package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;


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
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SaopauloPolipetCrawler extends Crawler {

   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());


   public SaopauloPolipetCrawler(Session session) {
      super(session);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".polipeti--produto") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[property=\"product:retailer_item_id\"]", "content");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".fbits-produto-nome.prodTitle.title", false);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".conteudoAbasProduto .paddingbox", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div #zoomImagemProduto", Arrays.asList("data-zoom-image"), "http://", "polipet.fbitsstatic.net/img/p/");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "div #zoomImagemProduto", Arrays.asList("data-zoom-image"), "http://", "polipet.fbitsstatic.net/img/p/", primaryImage);
         boolean available = doc.selectFirst(".produto-comprar.acenter #produto-botao-comprar-" + internalId) != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
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

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Poli-Pet")
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precoPor.com-precoDe", null, false, ',', session);

      if (spotlightPrice != null) {
         Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoDe", null, false, ',', session);
         BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
         CreditCards creditCards = scrapCreditCards(spotlightPrice);

         return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
      }

      return null;
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;

   }
}
