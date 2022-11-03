package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilMadridSupermercados extends Crawler {
   public BrasilMadridSupermercados(Session session) {
      super(session);
   }
   private static final String HOME_PAGE = "https://www.madrid.com.br/";
   private static final String SELLER_NAME = "madridsupermercados";
   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      super.extractInformation(doc);
      Element product = doc.selectFirst(".pdp");
      if (product != null) {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"#ctl00_ContentPlaceHolder1_h_pfid","value");
         String name = CrawlerUtils.scrapStringSimpleInfo(product,".detalheProduto > h1",true);
         String primaryImage =CrawlerUtils.scrapSimplePrimaryImage(product,"#ImgProdDesk > a img", List.of("src"),"","");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc,"#ulThumbs li a img",List.of("src"),"","",primaryImage);
         List<String> categories = CrawlerUtils.crawlCategories(doc,".breadcrumb a",true);
         String description = CrawlerUtils.scrapSimpleDescription(doc,List.of(".desc p"));
         Offers offers = scrapOffers(product);
         Product newProduct = ProductBuilder.create()
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setOffers(new Offers())
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();
         products.add(newProduct);
      } else {

         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }
   private Offers scrapOffers(Element product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         //.setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());
      return offers;
   }
   private Pricing scrapPricing(Element product) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(product,".valor",null,true,',',session);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(product,".de s",null,true,',',session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(price)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }
   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet( Card.MASTERCARD.toString(),Card.VISA.toString(),
         Card.AMEX.toString(), Card.DINERS.toString());

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
