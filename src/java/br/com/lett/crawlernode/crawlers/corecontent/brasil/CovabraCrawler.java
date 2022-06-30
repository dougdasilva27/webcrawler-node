package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;

import java.util.*;

public class CovabraCrawler extends Crawler {
   private final String  SELLER_FULL_NAME = "Covabra";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());

   public CovabraCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String store = session.getOptions().optString("website", "");
      BasicClientCookie cookie = new BasicClientCookie("website", store);
      cookie.setDomain("www.covabra.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".price-final_price", "data-product-id");
         String internalId = internalPid;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder__image", Arrays.asList("src"), "https", "www.covabra.com.br");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".category", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("#description"));
         List<String> eans = Collections.singletonList(CrawlerUtils.scrapStringSimpleInfo(doc, ".sku", false).replaceAll("[^0-9]", ""));
         boolean availableToBuy = doc.selectFirst(".estimaste-body") != null;

         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[data-price-type=finalPrice]", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[data-price-type=oldPrice]", null, false, ',', session);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }
}
