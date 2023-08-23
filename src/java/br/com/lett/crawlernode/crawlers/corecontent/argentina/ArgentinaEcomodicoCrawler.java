package br.com.lett.crawlernode.crawlers.corecontent.argentina;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgentinaEcomodicoCrawler extends Crawler {
   public ArgentinaEcomodicoCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.ecomodico.com.ar/";

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      if (isProductPage(doc)) {
         String internalId = regexInternalId(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".ui-pdp-title", true);
         List<String> secondaryImages = crawlImagesArray(doc);
         String primaryImage = !secondaryImages.isEmpty() ? secondaryImages.remove(0) : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ol.andes-breadcrumb a", false);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("ul.ui-pdp-features li", "p.ui-pdp-description__content"));

         boolean isAvailable = doc.selectFirst("#main_actions .andes-button--loud .andes-button__content") != null;
         Offers offers = isAvailable ? crawlOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
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

   private String regexInternalId(Document doc) {
      String regex = "\\/p\\/([A-Z0-9]+)";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         return matcher.group(1);
      }

      return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=\"item_id\"]", "value");
   }

   private List<String> crawlImagesArray(Document doc) {
      List<String> cleanImages = new ArrayList<>();
      List<String> arrayImages = CrawlerUtils.scrapSecondaryImages(doc, ".ui-pdp-gallery__column .ui-pdp-gallery__wrapper .ui-pdp-gallery__figure img", List.of("src"), "https", "http2.mlstatic.com", null);
      for (String img : arrayImages) {
         img = img.split("\\?")[0];
         cleanImages.add(img);
      }

      return cleanImages;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#ui-pdp-main-container") != null;
   }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("ecomodico")
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".ui-pdp-price__second-line .andes-money-amount__fraction", null, true, ',', session);

      if (spotlightPrice != null) {
         CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
         BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(null)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();

      } else {
         return null;
      }
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Integer installmentNumber = null;

      String installmentString = CrawlerUtils.scrapStringSimpleInfo(doc, "#pricing_subtitle", true);
      if (installmentString != null) {
         installmentString = installmentString.replace("x", "");
         installmentNumber = Integer.parseInt(installmentString);
      }
      Double installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#pricing_subtitle .andes-money-amount__fraction", null, true, ',', session);

      if (installmentNumber != null && installmentPrice != null) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(installmentPrice)
            .build());
      } else {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.NARANJA.toString(), Card.MAESTRO.toString(), Card.CABAL.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String saleDiscount = CrawlerUtils.calculateSales(pricing);
      sales.add(saleDiscount);
      return sales;
   }
}

