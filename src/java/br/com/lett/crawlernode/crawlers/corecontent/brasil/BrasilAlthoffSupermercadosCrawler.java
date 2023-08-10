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
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilAlthoffSupermercadosCrawler extends Crawler {
   public BrasilAlthoffSupermercadosCrawler(Session session) {
      super(session);
   }

   // We didn't find unavailable products, products with description or products with secondary image during the development of this crawler
   private Set<String> cards = Sets.newHashSet(Card.AMEX.toString(), Card.CABAL.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.HIPERCARD.toString(), Card.HIPER.toString(), Card.VISA.toString(), Card.DINERS.toString());
   private final String SELLER_NAME = "Althoff Supermercados";

   protected String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String cookieName = URLEncoder.encode("{\"id\":\"" + getStoreId() + "\",\"userSelected\":true}", StandardCharsets.UTF_8);
      BasicClientCookie cookie = new BasicClientCookie("st", cookieName);
      cookie.setDomain(".emcasa.althoff.com.br");
      cookie.setPath("/");

      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getUrlInternalId();
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".info > h5", true);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-image-gallery-active-image > img", "src");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product-renderer-category-path > a");
         boolean available = doc.selectFirst(".common-action-btn.item-button .icomoon-plus") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(null)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setCategories(categories)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-show-page") != null;
   }

   private String getUrlInternalId() {
      String regex = "produtos\\/([0-9]+)\\/";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-box .active-price-box", null, true, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-renderer-active-price-box", null, true, ',', session);
      }
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-box .price-value", null, true, ',', session);
      if (priceFrom == null) {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-renderer-normal-price-value", null, true, ',', session);
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setFinalPrice(price)
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .build());

      for (String flag : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(flag)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }
}
