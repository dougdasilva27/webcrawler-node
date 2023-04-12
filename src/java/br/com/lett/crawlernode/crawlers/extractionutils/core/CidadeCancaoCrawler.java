package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CidadeCancaoCrawler extends Crawler {
   private final String SELLER_FULL_NAME = "Cidade Cancao";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.ELO.toString(), Card.JCB.toString());

   public CidadeCancaoCrawler(Session session) {
      super(session);
   }

   private final String store_id = session.getOptions().optString("storeId", "toledo");

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("storeSelected", "https://" + store_id + ".cidadecancao.com");
      cookie.setDomain(store_id + ".cidadecancao.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=\"product\"]", "value");
         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".value[itemprop=\"sku\"]", true);
         String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[property=\"og:url\"]", "content");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title > span", false);
         List<String> secondaryImages = crawlListImages(doc);
         String primaryImage = !secondaryImages.isEmpty() ? secondaryImages.remove(0) : null;
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("#description"));
         boolean availableToBuy = doc.selectFirst(".title-out-stock") == null;
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(url)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(new CategoryCollection())
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> crawlListImages(Document doc) {
      JSONObject galleryJson = CrawlerUtils.selectJsonFromHtml(doc, "script:containsData(mage/gallery/gallery)", null, " ", false, false);
      JSONArray imagesJson = JSONUtils.getValueRecursive(galleryJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", ".", JSONArray.class, new JSONArray());

      return JSONUtils.jsonArrayToStringList(imagesJson, "full");
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[id^=product-price] > .price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[id^=old-price] > .price", null, false, ',', session);

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".column.main > .product-info-main") != null;
   }

}
