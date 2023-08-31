package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewImpl;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.drew.lang.annotations.NotNull;
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

public class ArgentinaHiperlibertadCrawler extends VTEXNewImpl {

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.CABAL.toString(), Card.NATIVA.toString(), Card.NARANJA.toString(), Card.AMEX.toString(), Card.CORDOBESA.toString());

   public ArgentinaHiperlibertadCrawler(@NotNull Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject skuInfo = scrapJsonFromHtml(doc);

      if (skuInfo != null && !skuInfo.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = skuInfo.optString("sku");
         String internalPid = skuInfo.optString("mpn");
         String name = skuInfo.optString("name");
         String description = skuInfo.optString("description");
         String primaryImage = skuInfo.optString("image");
         List<String> secondaryImages = getSecondaryImages(doc);
         String categories = skuInfo.optString("category");
         String isAvailable = JSONUtils.getValueRecursive(skuInfo, "offers.offers.0.availability", String.class, "");
         Offers offers = isAvailable != null && isAvailable.equalsIgnoreCase("http://schema.org/InStock") ? scrapOffers(skuInfo, doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(Collections.singleton(categories))
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imagesLi = doc.select("meta[property=\"og:image\"]");
      for (Element imageLi : imagesLi) {
         secondaryImages.add(imageLi.attr("content"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private JSONObject scrapJsonFromHtml(Document doc) {
      String jsonString = CrawlerUtils.scrapScriptFromHtml(doc, "script[type=\"application/ld+json\"]:containsData(sku)");
      if (jsonString != null && !jsonString.isEmpty()) {
         JSONArray array = CrawlerUtils.stringToJsonArray(jsonString);
         if (array != null) {
            return (JSONObject) array.get(0);
         }
      }
      return null;
   }

   private Offers scrapOffers(JSONObject skuInfo, Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(skuInfo, doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.getMainSellersNames().toString())
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject skuInfo, Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".vtex-product-price-1-x-currencyContainer--pdp-list-price", null, false, ',', session);
      Double spotlightPrice = JSONUtils.getValueRecursive(skuInfo, "offers.offers.0.price", Double.class, null);
      if (spotlightPrice == null) {
         Integer priceInt = JSONUtils.getValueRecursive(skuInfo, "offers.offers.0.price", Integer.class, null);
         if (priceInt != null) {
            spotlightPrice = priceInt.doubleValue();
         }
      }

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
