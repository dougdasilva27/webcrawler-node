package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public abstract class TottusCrawler extends Crawler {

   protected String homePage;
   protected String salerName;


   public TottusCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type='application/ld+json']", "", null, false, false);
      String internalId = jsonInfo.optString("sku", null);

      if (internalId != null) {

         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.title", true) + " " + CrawlerUtils.scrapStringSimpleInfo(doc, ".subtitle-container", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".Breadcrumbs .link.small");
         String primaryImage = crawlImage(doc);
         List<String> secondaryImages = crawlSecondaryImage(doc);
         String description = scrapDescription(doc);
         Offers offers = doc.selectFirst(".column-right-content .price.medium") != null ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      }

      return products;

   }

   private String crawlImage(Document doc) {
      String primary = null;
      String image = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-gallery-mobile-thumbnails img", "src");

      if (image != null) {
         primary = image.replace("thumb", "large");
      }

      return CrawlerUtils.completeUrl(primary, "https", homePage);

   }

   private List<String> crawlSecondaryImage(Document doc) {
      List<String> secondaryImagesList = new ArrayList<>();
      Elements secondaryImages = doc.select(".product-gallery-mobile-nav img");
      for (Element e : secondaryImages) {
         String image = e.attr("src");
         if (image != null) {
            secondaryImagesList.add(image.replace("thumb", "large"));

         }
      }

      if (!secondaryImagesList.isEmpty()) {
         secondaryImagesList.remove(0);
      }
      return secondaryImagesList;
   }

   private String scrapDescription(Document doc) {
      List<String> selectors = new ArrayList<>();
      selectors.add(".description-content");
      selectors.add("div.tabs-container");

      return CrawlerUtils.scrapElementsDescription(doc, selectors);
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(salerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price.medium.cmrPrice", null, false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price.medium.currentPrice", null, false, '.', session);
      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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


      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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
