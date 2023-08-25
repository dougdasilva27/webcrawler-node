package br.com.lett.crawlernode.crawlers.corecontent.argentina;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ArgentinaCentraloesteCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Central Oeste Argentina";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());


   public ArgentinaCentraloesteCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".gallery-wrapper script[type=\"text/x-magento-init\"]", null, " ", false, false);

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".price-box", "data-product-id");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute.sku .value", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.page-title .base", false);
         String primaryImage = JSONUtils.getValueRecursive(json, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data.0.img", String.class);
         List<String> productSecondaryImages = ImageCapture(json, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product.attribute.description .value"));
         String stock = CrawlerUtils.scrapStringSimpleInfo(doc, ".stock.available span", true);
         boolean available = stock != null && stock.contains("En stock");
         Offers offers = available ? scrapOffers(doc) : new Offers();
         List<String> eans = scrapEan(doc);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(productSecondaryImages)
            .setEans(eans)
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
      return doc.selectFirst(".product.attribute.sku") != null;
   }

   private List<String> ImageCapture (JSONObject json, String productPrimaryImage) throws Exception {
      List<String> productSecondaryImagesList = new ArrayList<>();
      try {
         JSONArray arrayImage = (JSONArray) json.optQuery("/[data-gallery-role=gallery-placeholder]/mage~1gallery~1gallery/data");
         for (Object objImage : arrayImage){

            String obj = objImage.toString();
            JSONObject jobj = JSONUtils.stringToJson(obj);
            String newUrlImage = jobj.getString("img");
            if(!newUrlImage.equals(productPrimaryImage)){
               productSecondaryImagesList.add(newUrlImage);
            }

         }
      }catch (NullPointerException n){
         productSecondaryImagesList = null;
      }

      return productSecondaryImagesList;
   }


   protected List<String> scrapEan(Document doc) {
      List<String> eans = new ArrayList<>();
      String ean = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute.sku .value", true);
      if (ean != null) {
         eans.add(ean);
      }
      return eans;

   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String salesOnPrice = CrawlerUtils.calculateSales(pricing);
      if (salesOnPrice != null) {
         sales.add(salesOnPrice);
      }
      return sales;
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, true, ',', session);
      Double spotlightPrice = getPrice(doc);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private Double getPrice(Document doc) {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".special-price .price", null, true, ',', session);
      if (price == null) {
         price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-price .price", null, true, ',', session);

      }
      return price;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
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

   //site hasn't rating

}
