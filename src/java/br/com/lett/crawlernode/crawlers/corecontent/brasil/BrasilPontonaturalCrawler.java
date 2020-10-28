package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPontonaturalCrawler extends Crawler {

   private static final String HOME_PAGE = "www.pontonatural.com.br";

   public BrasilPontonaturalCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-actions input[name=\"id_product\"]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.page-title", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".col li a span", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-lmage-large img", Arrays.asList("src"), "https", HOME_PAGE);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#product-images-thumbs .thumb-container:last-child img", Arrays.asList("src"), "https", HOME_PAGE, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".tabs.product-tabs #product-infos-tabs-content .rte-content p"));
         boolean available = doc.selectFirst(".add-to-cart") != null;
         Offers offers = available ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#product #content-wrapper") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Ponto Natural")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price_regular_precio", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price", "content", false, '.', session);
      CreditCards creditCards = scrapCreditCards(doc,spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice,null);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();


   }

   private CreditCards scrapCreditCards(Document doc,Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      Installments installments = scrapInstallments(doc);
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

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements installmentsAndValues = doc.select(".accordion-container.is-open .panel ul li");

      if (installmentsAndValues != null && !installmentsAndValues.isEmpty()) {

         for (Element eachInstallmentsAndValues: installmentsAndValues) {
            String eachInstallmentsAndValuesString = eachInstallmentsAndValues.text();

            String installmentString = eachInstallmentsAndValuesString.contains("x") ? eachInstallmentsAndValuesString.split("x")[0]: null;
            int installment = installmentString != null ? MathUtils.parseInt(installmentString):0;

            String valueString = eachInstallmentsAndValuesString.contains("R$")? eachInstallmentsAndValuesString.split("R")[1]: null;
            double value = valueString != null? MathUtils.parseDoubleWithComma(valueString): 0D;

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
         }
      }
      return installments;
   }
}
