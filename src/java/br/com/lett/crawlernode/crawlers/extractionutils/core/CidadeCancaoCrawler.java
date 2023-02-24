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
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import software.amazon.awssdk.services.glue.model.Crawl;

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

      if (!session.getOriginalURL().contains(store_id)) {
         throw new Exception("URL não pertence a localidade correta");
      }

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = scrapInternalPid(doc);
         String internalId = String.valueOf(CrawlerUtils.scrapIntegerFromHtml(doc, ".sku-align", true, null));
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", false);
         List<String> secondaryImages = crawlListImages(doc);
         String primaryImage = !secondaryImages.isEmpty() ? secondaryImages.remove(0) : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.product):not(.home)", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".linha-descricao, .weight-view-info"));
         boolean availableToBuy = doc.selectFirst(".preco-prod") != null;
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
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
      List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".more-views img", List.of("src"), "https", store_id + ".cidadecancao.com/", null);
      for (int i = 0; i < images.size(); i++) {
         images.set(i, images.get(i).replace("thumbnail/80x", "image/855x635"));
      }
      return images;
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = null;
      String action = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-shop .product-view-form", "action");

      if (action != null && !action.isEmpty()) {
         String regex = "product/(.*)/form_key";
         Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
         final Matcher matcher = pattern.matcher(action);

         if (matcher.find()) {
            internalPid = matcher.group(1);
         }
      }

      return internalPid;
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-box .special-price .price", null, false, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-box .regular-price .price", null, false, ',', session);
      }

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-box .old-price .price", null, false, ',', session);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-shop") != null;
   }

}
