package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

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


public class RiodejaneiroDikamercadoCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Dika Mercado Rio de Janeiro";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public RiodejaneiroDikamercadoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getProductId();
         String internalPid = getProductPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.mb-3", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs > li:not(:first-child):not(:last-child) > a");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".owl-lazy", "data-src");
         //Site hasn't secondary images
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".row .col-md-12 .bg-secondary "));
         boolean available = !doc.select(".coluna-quantidade-carrinho").isEmpty(); //I didn't find any product unavailable to test
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".pt-1.mb-4 > span:nth-child(1)") != null;
   }

   private String getProductId() {
      String[] url = session.getOriginalURL().split("-");
      return url[url.length - 1].split("/")[0];
   }

   private String getProductPid(Document doc) {
      String extractPid = doc.selectFirst("div.pt-1.mb-4").text();
      String[] internalPid = extractPid.split("Código: #");
      return internalPid[internalPid.length - 1].split("\\|")[0];

   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      //Site hasn't any sale

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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-final", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      //Site hasn't any product with old price

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   //Site hasn't rating

}
