package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

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
import org.jsoup.nodes.Element;

import java.util.*;

public class SaopauloAraujoCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.araujo.com.br/";
   private static final String SELLER = "araujo";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.ELO.toString(), Card.MAESTRO.toString(), Card.MASTERCARD.toString(), Card.DINERS.toString(), Card.DISCOVER.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public SaopauloAraujoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = doc.select(".product-detail").attr("data-pid");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info-name h1", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadCrumb__content div a", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".productDetails__images__principal__img", Collections.singletonList("src"), "https:", "www.araujo.com.br");
         List<String> secondaryImage = getSecondaryImages(doc, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".pdInfo")) + CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".pdSpecs"));
         boolean availableToBuy = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prices .productPrice__price", null, false, ',', session) != null;
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-detail") != null;
   }

   private List<String> getSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImagesToChange = CrawlerUtils.scrapSecondaryImages(doc, ".images__indicator__button img", Arrays.asList("src"), "https", "www.araujo.com.br", primaryImage);
      List<String> secondaryImages = new ArrayList<String>();
      for (String imageUrl : secondaryImagesToChange) {
         String newImageUrl = imageUrl.split("\\?sw=")[0];
         secondaryImages.add(newImageUrl);
      }
      if (!secondaryImages.isEmpty()) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc, pricing);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prices .productPrice__price", null, false, ',', session);

      if (spotlightPrice != null) {
         Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".prices .productPrice__lineThrough", null, false, ',', session);
         CreditCards creditCards = scrapCreditCards(spotlightPrice);

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .build();
      }

      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   protected List<String> scrapSales(Document doc, Pricing pricing) {
      List<String> sales = new ArrayList<>();

      Element economize = doc.selectFirst(".productDetails__buy_more_for_less-economize");
      if (economize instanceof Element) {
         Integer quantity = CrawlerUtils.scrapSimpleInteger(economize, ".productDetails__buy_more_for_less-economize .productDetails__kit-price-info span", true);
         if (quantity instanceof Integer) {
            int salesQuantity = quantity;
            Double salesPrice = CrawlerUtils.scrapDoublePriceFromHtml(economize, ".productDetails__buy_more_for_less-economize .productDetails__kit-price-info .kit-price", null, false, ',', session);
            if (salesPrice != null && salesPrice > 1) {
               sales.add("Leve " + salesQuantity + " e pague R$ " + salesPrice + " cada unidade");
            }
         }
      }
      return sales;
   }
}
