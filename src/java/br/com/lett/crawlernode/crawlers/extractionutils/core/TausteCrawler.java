package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TausteCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Tauste";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   private final String location = getLocation();

   protected String getLocation() {
      return session.getOptions().optString("LOCATION");
   }

   public TausteCrawler(Session session) {
      super(session);
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      boolean validUrl = isUrlCurrentLocation(session.getOriginalURL(), location);

      if (isProductPage(doc) && validUrl) {
         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".sku .value", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title .base", true);

         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "img.gallery-placeholder__image", Arrays.asList("src"), "https", "tauste.com.br");

         boolean availableToBuy = doc.select(".action.primary.tocart") != null;
         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isUrlCurrentLocation(String originalURL, String location) {
      String regex = "tauste.com.br\\/(.*)\\/";
      String urlLocation = "";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(originalURL);

      if (matcher.find()) {
         urlLocation = matcher.group(1);
      }

      return urlLocation.equals(location);
   }

   private boolean isProductPage(Document doc) {
      return doc.select(".product-info-main") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;

   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".first_price_discount_container");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price > span > span > span.price", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".special-price > span > span > span.price", null, true, ',', session);

      if(spotlightPrice == null){
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-final_price > span > span > span", null, true, ',', session);
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlighPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlighPrice)
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
