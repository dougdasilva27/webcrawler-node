package br.com.lett.crawlernode.crawlers.corecontent.mexico;

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
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MexicoFerrePatCrawler extends Crawler {
   public MexicoFerrePatCrawler(Session session) {
      super(session);
   }

   private final static String SELLER_FULL_NAME = "Ferre Pat";

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();
      Element productDetail = document.selectFirst(".product-detail");
      if (productDetail != null) {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(productDetail, ".cart-list-item-qty", "data-id");
         String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(productDetail, "input[name=producto_deseado]", "value");
         List<String> images = scrapImages(document);
         String primaryImage = images.size() > 0 ? images.remove(0) : null;
         String description = CrawlerUtils.scrapElementsDescription(document, List.of(".content-description"));
         Offers offers = crawlAvailability(productDetail) ? scrapOffers(productDetail) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();
         products.add(product);
      } else {
         Logging.printLogError(logger, session, "Not a product page" + this.session.getOriginalURL());
      }
      return products;
   }

   private Offers scrapOffers(Element product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
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

   private Pricing scrapPricing(Element product) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(product, ".grid-mov div .new-price", null, true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(product, ".grid-mov div .previous-price", null, true, '.', session);
      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
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

      Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private List<String> scrapImages(Document doc) {
      List<String> images = new ArrayList<>();
      Elements imagesHtml = doc.select(".info .gallery li img");
      for (Element element : imagesHtml) {
         String imagePath = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "img", "src");
         if (imagePath != null && !imagePath.isEmpty()) {
            images.add(imagePath);
         }
      }
      return images;
   }

   private boolean crawlAvailability(Element product) {
      String units = CrawlerUtils.scrapStringSimpleInfo(product, ".units-available span", true);
      if (units != null && !units.isEmpty()) {
         return !units.equals("0 pz.");
      }
      return false;
   }
}
