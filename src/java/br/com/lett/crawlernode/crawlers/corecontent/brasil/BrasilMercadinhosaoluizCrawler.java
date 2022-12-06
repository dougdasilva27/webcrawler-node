package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import org.jsoup.nodes.Document;
import models.pricing.CreditCards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilMercadinhosaoluizCrawler extends Crawler {
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());

   public BrasilMercadinhosaoluizCrawler(Session session) {
      super(session);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#___rc-p-id", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__head > h2 > .productName", false);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".infos-product > .shell > .desc_product > .productDescription", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image-main", Arrays.asList("src"), "http://", "www.mercadinhossaoluiz.com.br");
         boolean available = getAvaliability(doc);
         Offers offers = available ? scrapOffers(doc) : new Offers();


         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean getAvaliability(Document doc) {
      Double holder = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__price > div > .descricao-preco > .valor-de > .skuListPrice", null, true, ',', session);
      return holder != null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".section-product") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales =new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Mercadinhos São Luiz")
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__price > div > .descricao-preco > .valor-de > .skuListPrice", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__price > div > .descricao-preco > .valor-por > .skuBestPrice", null, true, ',', session);

      if (spotlightPrice == null || spotlightPrice == 0.0) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__price > div > .descricao-preco > .valor-de > .skuListPrice", null, true, ',', session);
      } else if (priceFrom == 0.0 || priceFrom == null) {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__price > div > .descricao-preco > .valor-por > .skuBestPrice", null, true, ',', session);
      }
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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

