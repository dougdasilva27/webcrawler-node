package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilMafachaCrawler extends Crawler {
   public BrasilMafachaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.MIRANHA);
   }
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString());
   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();
      if (!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }
      // Get all product information
      String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".h1.page-title span", false);
      String productInternalId = CrawlerUtils.scrapStringSimpleInfo(document, "#product-details > div.product-reference > span", false);
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(document, "#main-product-wrapper > div.tabs.product-tabs.product-sections > section.product-description-section.block-section > div > div > div", false);
      String productPrimaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, "#product-images-large > div.swiper-wrapper > div > img", Arrays.asList("data-image-large-src"), "", "");
      List<String> productSecondaryImages = CrawlerUtils.scrapSecondaryImages(document, "#product-images-thumbs > div.swiper-wrapper div img", Arrays.asList("data-image-large-src"), "", "", productPrimaryImage);
      Offers offers = isValid(document) ? scrapOffers(document, productInternalId) : new Offers();
      List<String> categories =CrawlerUtils.crawlCategories(document, "#wrapper > div:nth-child(1) > nav > div > div:nth-child(1) > ol li a span");
      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(productInternalId)
         .setName(productName)
         .setCategories(categories)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages)
         .setDescription(productDescription)
         .setOffers(offers)
         .build();
      products.add(product);
      return products;
   }

   private Offers scrapOffers(Document document, String productInternalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document, productInternalId);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
      offers.add(new Offer.OfferBuilder()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName("mafacha")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .setSales(sales)
         .build()
      );
      return offers;
   }

   private Pricing scrapPricing(Document document, String id) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, "span.current-price >span", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, "span.product-discount > span", null, false, ',', session);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst("#product-preloader") != null;
   }
   private boolean isValid(Document document) {
      return document.selectFirst(".product-unavailable") == null && document.selectFirst(".product-price") != null;
   }
}
