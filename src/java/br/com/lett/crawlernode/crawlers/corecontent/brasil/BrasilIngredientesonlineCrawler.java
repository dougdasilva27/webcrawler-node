package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BrasilIngredientesonlineCrawler extends Crawler {
   private final String  SELLER_FULL_NAME = "Ingredientes Online";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());


   public BrasilIngredientesonlineCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);

      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute.sku > div", true);
         String internalId = internalPid;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title", true);
         List<String> images = scrapImages(doc);
         String primaryImage = images.isEmpty() ? "" : images.remove(0);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".product-info-description"));
         RatingsReviews ratingsReviews = scrapRating(doc);
         boolean availableToBuy = doc.selectFirst(".box-tocart") != null;
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .build();
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main span[data-price-type=finalPrice]", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main span[data-price-type=oldPrice]", null, false, ',', session);

      if(Objects.equals(priceFrom, spotlightPrice)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(scrapCreditCards(doc, spotlightPrice))
         .setBankSlip(scrapBankSlip(doc, spotlightPrice))
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc, spotlightPrice);
      for (String card : cards) {
         creditCards.add(
            CreditCard.CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false).build());
      }
      return creditCards;
   }

   private Installments scrapInstallments(Document doc, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();
      installments.add(
         Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice).build());

      if (doc.selectFirst(".installments") != null) {
         int parcels = CrawlerUtils.scrapIntegerFromHtml(doc, ".installment_period", true, 0);
         Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".installment_value", null, true, ',', session);
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(parcels)
            .setInstallmentPrice(price)
            .build());
      }
      return installments;
   }

   private BankSlip scrapBankSlip(Document doc, Double spotlightPrice) throws MalformedPricingException {
      if (doc.selectFirst(".bankslip_excerpt") != null) {
         Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".bankslip_excerpt .price", null, true, ',', session);
         return BankSlip.BankSlipBuilder.create().setFinalPrice(price).build();
      }
      return BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build();
   }

   private List<String> scrapImages(Document doc) {
      String imagesScript = CrawlerUtils.scrapScriptFromHtml(doc, ".product.media script");
      JSONArray imagesArray = CrawlerUtils.stringToJsonArray(imagesScript);
      JSONObject imagesJson = imagesArray.isEmpty() ? null : imagesArray.getJSONObject(0);
      if(imagesJson != null) {
         JSONArray imagesData = (JSONArray) imagesJson.optQuery("/[data-gallery-role=gallery-placeholder]/mage~1gallery~1gallery/data");
         if(imagesData != null && !imagesData.isEmpty()) {
            return IntStream.range(0, imagesData.length()).mapToObj(i -> imagesData.getJSONObject(i).optString("full"))
               .collect(Collectors.toList());
         }
      }
      return Collections.emptyList();
   }

   private RatingsReviews scrapRating(Document doc) {
      String productTrustVoxId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".price-box.price-final_price", "data-product-id");
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "111754", logger);
      return trustVox.extractRatingAndReviews(productTrustVoxId, doc, dataFetcher);
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }
}
