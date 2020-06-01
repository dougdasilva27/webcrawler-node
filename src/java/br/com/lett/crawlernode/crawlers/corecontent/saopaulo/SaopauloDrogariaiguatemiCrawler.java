package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * Date: 07/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class SaopauloDrogariaiguatemiCrawler extends Crawler {

   private static final String HOME_PAGE = "https://drogariaiguatemi.com.br/";
   private static final String SELLER_FULL_NAME = "Drogaria Iguatemi";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public SaopauloDrogariaiguatemiCrawler(Session session) {
      super(session);
   }


   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.sku .value", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product .page-title", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".items li[class^=item category]");

         JSONArray images = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);
         String primaryImage = crawlPrimaryImage(images);
         String secondaryImages = crawlSecondaryImages(images, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(
               ".product.attribute.overview .value",
               ".product.data.items ul li:not(#tab-label-reviews)",
               ".product.data.items div.data.item:not(#reviews)"));
         Offers offers = doc.select(".product.alert.stock").isEmpty() ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-info-main").isEmpty();
   }


   private String crawlPrimaryImage(JSONArray images) {
      String primaryImage = null;

      for (int i = 0; i < images.length(); i++) {
         Object obj = images.get(0);

         if (obj instanceof JSONObject) {
            JSONObject jsonImage = (JSONObject) obj;

            if (jsonImage.has("isMain") && jsonImage.getBoolean("isMain") && jsonImage.has("full")) {
               primaryImage = jsonImage.getString("full");
               break;
            }
         } else if (obj instanceof String) {
            primaryImage = obj.toString();
         }
      }

      return primaryImage;
   }

   /**
    * 
    * @param doc
    * @return
    */
   private String crawlSecondaryImages(JSONArray images, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (int i = 0; i < images.length(); i++) {
         Object obj = images.get(0);

         if (obj instanceof JSONObject) {
            JSONObject jsonImage = (JSONObject) obj;

            if (jsonImage.has("isMain") && jsonImage.getBoolean("isMain") && jsonImage.has("full")) {
               String image = jsonImage.optString("full", null);
               if (image != null && !image.equalsIgnoreCase(primaryImage)) {
                  secondaryImagesArray.put(image);
               }
            }
         } else if (obj instanceof String) {
            String image = obj.toString();
            if (image != null && !image.equalsIgnoreCase(primaryImage)) {
               secondaryImagesArray.put(image);
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(new ArrayList<>())
            .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main .price-box .old-price .price", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main .price-box [data-price-type=\"finalPrice\"] .price", null, false, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }
}
