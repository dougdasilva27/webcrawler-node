package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
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
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilSumireCrawler extends Crawler {

   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString());
   protected JSONArray jsonVariations;

   public BrasilSumireCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      jsonVariations = variations(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#product_addtocart_form > input[type=hidden]:first-child", "value");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info-main > div.product.attribute.overview > div > p", false);
         JSONObject objectVariations = jsonVariations != null ? JSONUtils.getValueRecursive(jsonVariations, "0.[data-role=swatch-options].Magento_Swatches/js/swatch-renderer.jsonConfig.images", JSONObject.class) : new JSONObject();
         if (objectVariations != null && !objectVariations.isEmpty()) {
            for (Iterator<String> it = objectVariations.keys(); it.hasNext(); ) {
               String internalId = it.next();
               String name = JSONUtils.getValueRecursive(objectVariations, internalId + ".0.caption", String.class);
               String primaryImage = JSONUtils.getValueRecursive(objectVariations, internalId + ".0.full", String.class);
               Offers offers = variationOffers(internalId);

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setDescription(description)
                  .setOffers(offers)
                  .build();
               products.add(product);
            }
         } else {
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".title-sku-wrapper > div.page-title-wrapper.product > h1 > span", false);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product.media > div.gallery-placeholder._block-content-loading > img", Arrays.asList("src"), "https", "://www.perfumariasumire.com.br/");
            boolean available = doc.selectFirst(".product-info-main > div.product.alert.stock > a") == null;
            Offers offers = available ? scrapOffers(doc, internalPid) : new Offers();
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .setOffers(offers)
               .build();
            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".title-sku-wrapper > div.page-title-wrapper.product > h1") != null;
   }

   private Offers scrapOffers(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, internalId);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("sumire")
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc, String internalId) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main > div.product-info-price > div.price-box.price-final_price > span > span > meta:nth-child(odd)", "content", false, '.', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-price-" + internalId, "data-price-amount", false, '.', session);
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;

   }

   private JSONArray variations(Document doc) {
      String variantHolder = CrawlerUtils.scrapScriptFromHtml(doc, "#product-options-wrapper > div > script:nth-child(even)");
      if (variantHolder != null && !variantHolder.isEmpty()) {
         return JSONUtils.stringToJsonArray(variantHolder);
      }
      return null;
   }

   private Offers variationOffers(String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapVariationspricing(internalId);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("sumire")
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapVariationspricing(String internalId) throws MalformedPricingException {
      JSONObject objectVariations = JSONUtils.getValueRecursive(jsonVariations, "0.[data-role=swatch-options].Magento_Swatches/js/swatch-renderer.jsonConfig.optionPrices.", JSONObject.class);
      Double spotlightPrice = JSONUtils.getValueRecursive(objectVariations, internalId + ".finalPrice.amount", Double.class);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }
}
