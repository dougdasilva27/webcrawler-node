package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChileAritransCrawler extends Crawler {

   private static String SELLER_NAME = "Aritrans";
   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   public ChileAritransCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_title", false);
         String internalId = getInternalId(doc);
         String selectorPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "link[rel=\"shortlink\"]", "href");
         String internalPid = selectorPid != null && !selectorPid.isEmpty() ? CommonMethods.getLast(selectorPid.split("=")) : null;
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".woocommerce-Tabs-panel--description", false);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".summary.entry-summary > span > a");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[property=\"og:image\"]", "content");
         List<String> secondaryImages = getSecondaryImages(doc);
         boolean available = doc.selectFirst(".stock.out-of-stock") != null;
         Offers offers = !available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(productName)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
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
      return doc.selectFirst(".summary.entry-summary") != null;
   }

   private String getInternalId(Document document) {
      String internalId = CrawlerUtils.scrapStringSimpleInfo(document, ".sku", false);
      if (internalId == null) {
         internalId = CrawlerUtils.scrapScriptFromHtml(document, "body > script[type=\"application/ld+json\"]");
         if (internalId != null && !internalId.isEmpty()) {
            JSONArray script = new JSONArray(internalId);
            if (script != null && !script.isEmpty()) {
               JSONObject object = JSONUtils.getValueRecursive(script, "0", JSONObject.class);
               if (object != null && !object.isEmpty()) {
                  JSONArray array = JSONUtils.getJSONArrayValue(object, "@graph");
                  if (array != null && !array.isEmpty()) {
                     JSONObject itemList = JSONUtils.getValueRecursive(array, "1", JSONObject.class);
                     if (itemList != null && !itemList.isEmpty()) {
                        internalId = itemList.optString("sku");
                     }
                  }
               }
            }
         }
      }

      return internalId;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imagesLi = doc.select(".woocommerce-product-gallery__image > a > img");
      for (Element imageLi : imagesLi) {
         secondaryImages.add(imageLi.attr("src"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "p.price > span > ins > span > bdi", null, false, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.summary.entry-summary > p.price > span > span", null, false, ',', session);
      }
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "p.price > span > del > span > bdi", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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
