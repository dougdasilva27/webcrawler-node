package br.com.lett.crawlernode.crawlers.corecontent.panama;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PanamaRibasmithCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Riba Smith";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   public PanamaRibasmithCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document document) throws Exception {

      List<Product> products = new ArrayList<>();

      if (document.selectFirst(".product-info-main") != null) {

         String name = CrawlerUtils.scrapStringSimpleInfo(document,".base",true);
         String internalId = CrawlerUtils.scrapStringSimpleInfo(document, ".value[itemprop=\"sku\"]", true);
         String internalPId = CrawlerUtils.scrapStringSimpleInfo(document, "input[name=\"product\"]", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(document, ".breadcrumbs ul li", true);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".gallery-placeholder__image", "src");
         String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList("#description", "#additional"));
         String disponibilidade = CrawlerUtils.scrapStringSimpleInfo(document, ".stock.available span", true);
         boolean available = disponibilidade != null && disponibilidade.equals("Disponible");
         Offers offers = available ? scrapOffers(document) : new Offers();

         products.add(ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setInternalId(internalId)
            .setInternalPid(internalPId)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build());

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);

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

   private Pricing scrapPricing(Document document) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document,".price",null,true,',',session);

      CreditCards creditCards = new CreditCards();

      Installments regularCard = new Installments();
         regularCard.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());


      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(regularCard)
            .build());
      }

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build())
         .build();
   }
}
