package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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

import java.util.*;

/**
 * 1) Only one sku per page.
 * <p>
 * Price crawling notes: 1) This market needs some cookies for request product pages. 2) There is no
 * bank slip (boleto bancario) payment option. 3) There is no installments for card payment. So we
 * only have 1x payment, and to this value we use the cash price crawled from the sku page. (nao
 * existe divisao no cartao de credito).
 *
 * @author Gabriel Dornelas
 */
public class MexicoSorianasuperCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Soriana";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public MexicoSorianasuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-wrapper [data-pid]", "data-pid");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-detail .d-none", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".carousel-item.active img", Arrays.asList("src"), "https", "www.soriana.com");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".carousel-item img", Arrays.asList("src"), "https", "www.soriana.com", primaryImage);
         boolean available = doc.selectFirst(".btn-add-to-cart") != null;
         Offers offers = available? scrapOffers(doc) : new Offers();


         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
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
      Double spotlightPrice = scrapSpotlightPrice(doc);
      Double priceFrom = scrapPriceFrom(doc, spotlightPrice);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards( Double spotlightPrice) throws MalformedPricingException {
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

   private Double scrapSpotlightPrice(Document doc) {
      Double price = null;
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".strike-through .product-price", null, false, ',', session);
      Double priceDiscount = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sales .product-price", null, false, '.', session);
      if (priceDiscount != null) {
         price = priceDiscount;
      } else {
         price = spotlightPrice;
      }
      return price;
   }

   private Double scrapPriceFrom(Document doc, Double spotlightPrice) {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price_regular_precio", null, false, '.', session);
      if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }
      return priceFrom;
   }



   private boolean isProductPage(Document doc) {
      return doc.select(".product-detail").first() != null;
   }


}
