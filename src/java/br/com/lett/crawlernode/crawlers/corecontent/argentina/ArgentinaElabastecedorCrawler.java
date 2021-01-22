package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ArgentinaElabastecedorCrawler extends Crawler {

   private static final String SELLER_FULL_NAME= "El Abastecedor";
   private static final Set<Card> CARDS = Sets.newHashSet(Card.VISA,Card.MASTERCARD,Card.AMEX);

   public ArgentinaElabastecedorCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if(isProductPage(session.getOriginalURL())){

         String internalId = CommonMethods.getLast(session.getOriginalURL().split("="));
         String internalPid= internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(document,".product-name h1",true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document,"#product-zoom", Arrays.asList("src"),"https:","www.elabastecedor.com.ar");
         Boolean available = true; //nao foi encontrado produto indisponivel
         Offers offers = available? ScrapOffers(document):new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);

      }

      return  products;
   }

   private Offers ScrapOffers(Document document) throws OfferException, MalformedPricingException {

      Offers offers = new Offers();

      Pricing pricing = ScrapPricing(document);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing ScrapPricing(Document document) throws MalformedPricingException {
      Double priceFrom;
      Double price;

      if(document.selectFirst(".price-box h2")!= null){
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document,"#product-price-48",null,true,'.',session);
         price = CrawlerUtils.scrapDoublePriceFromHtml(document,".price-box h2",null,true,'.',session);
      }else {
         priceFrom = null;
         price = CrawlerUtils.scrapDoublePriceFromHtml(document,"#product-price-48",null,true,'.',session);
      }

      CreditCards creditCards = ScrapCreditcard(price);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(price)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards ScrapCreditcard(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build());

      for (Card card :CARDS) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }
      return creditCards;
   }

   private boolean isProductPage(String originalURL) {
      return originalURL.contains("producto");
   }
}
