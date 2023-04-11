package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.models.*;
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
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class BrasilLojaStarbucksAtHomeCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "starbucksathome";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());

   public BrasilLojaStarbucksAtHomeCrawler(Session session) {
      super(session);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-info-price .price-box.price-final_price[data-product-id]", "data-product-id");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-info-price .price-box.price-final_price[data-product-id]", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title-wrapper h1 span", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".items li", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder__image", Collections.singletonList("src"), "https:", "www.starbucksathome.com");
         List<String> secondaryImage = getSecondaryImages(doc);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-info-content .value p"));
         boolean availableToBuy = doc.select(".stock.unavailable").isEmpty();
         Offers offers = availableToBuy ? scrapOffer(doc, internalId) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }
   private List<String> getSecondaryImages(Document doc){
      List<String> secondaryImages = new ArrayList<>();
      String img1 = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".tasting_notes .attribute-image img[src]", "src");
      String img2 = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".roast_type .attribute-image img[src]", "src");
      String img3 = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".content-box img[src]", "src");
      if (img1 != null){
         secondaryImages.add(img1);
      }
      if (img2 != null){
         secondaryImages.add(img2);
      }
      if (img3 != null){
         secondaryImages.add(img3);
      }
      return secondaryImages;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".column.main") != null;
   }

   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(internalId, doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(String internalId, Document doc) throws MalformedPricingException {
      Boolean isOnSale = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main .special-price .price", null, false, ',', session) == null;
      Double spotlightPrice = isOnSale ? CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main .price-container .price", null, false, ',', session) : CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main .special-price .price", null, false, ',', session);
      Double priceFrom = isOnSale ? null : CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main .old-price .price", null, false, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".special-price .price");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
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


}
