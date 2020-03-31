package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
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

public class BrasilAgrocidiCrawler extends Crawler {
   
   private String MAIN_SELLER_NAME = "Agro Cidi";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
         Card.MASTERCARD.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), 
         Card.AMEX.toString(), Card.ELO.toString());

   public BrasilAgrocidiCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".principal div[data-produto-id]", "data-produto-id");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=sku]", false);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".principal .nome-produto", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(:first-child) a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc,
               ".produto-thumbs:not(.thumbs-horizontal) #carouselImagem ul.miniaturas:first-child  a",
               Arrays.asList("data-imagem-grande"), "https:", "cdn.awsli.com.br");

         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
               ".produto-thumbs:not(.thumbs-horizontal) #carouselImagem ul.miniaturas:first-child  a",
               Arrays.asList("data-imagem-grande"), "https:", "cdn.awsli.com.br", primaryImage);

         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(
               ".row-fluid:not(#comentarios-container) > div.span12  .abas-custom .tab-content"));
         
         Offers offers = scrapOffers(doc);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
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
      
      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      
      return offers;
   }
   
   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .preco-produto .preco-promocional", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .preco-venda.titulo", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      
      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
            .build();
   }
   
   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      
      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      
      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".principal .preco-produto  .preco-parcela", doc, false, "x");
      if (!pair.isAnyValueNull()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(pair.getFirst())
               .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
               .build());
      }
      
      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(false)
               .setInstallments(installments)
               .build());
      }
      
      return creditCards;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".secao-principal .produto").isEmpty();
   }

}
