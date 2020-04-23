package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilViannapetCrawler extends Crawler {

   public BrasilViannapetCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_FULL_NAME = "Vienna Pet";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#itemId", "value");
         String internalId = internalPid;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product_name", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(.firstItem):not(.lastItem) a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#product_image li a", Arrays.asList("href"), "https",
               "static.mercadoshops.com");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#product_image li a", Arrays.asList("href"), "https",
               "static.mercadoshops.com", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product_description"));
         Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "#itemStock", "value", 0);
         boolean available = stock > 0;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setStock(stock)
               .setOffers(offers)
               .build();

         Elements variations = doc.select("#product-select option");
         if (!variations.isEmpty()) {
            for (Element e : variations) {
               Product clone = product.clone();
               clone.setInternalId(e.val());
               clone.setName(clone.getName() + " " + e.ownText());

               products.add(clone);
            }
         } else {
            Product clone = product.clone();
            clone.setInternalId(internalPid);

            products.add(clone);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#itemId") != null;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();
      Pricing pricing = scrapPricing(doc);

      offers.add(OfferBuilder.create()
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product_price .money", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice);

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
   }


   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice)
         throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc, spotlightPrice);
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create().setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice).build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create().setBrand(card)
               .setInstallments(installments).setIsShopCard(false).build());
      }

      return creditCards;
   }


   private Installments scrapInstallments(Document doc, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      Request request = RequestBuilder.create()
            .setUrl("https://www.viannapet.com/paymentmethods?price=" + spotlightPrice)
            .setCookies(cookies)
            .build();

      JSONArray priceApiArray = JSONUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      for (Object o : priceApiArray) {
         JSONObject paymentMethodJson = o instanceof JSONObject ? (JSONObject) o : new JSONObject();

         JSONArray installmentsArray = JSONUtils.getJSONArrayValue(paymentMethodJson, "payer_costs");
         for (Object obj : installmentsArray) {
            JSONObject installmentJson = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();

            Integer installmentNumber = JSONUtils.getIntegerValueFromJSON(installmentJson, "installments", null);
            if (installmentNumber != null) {
               Double interestFee = JSONUtils.getDoubleValueFromJSON(installmentJson, "installment_rate", true);
               Double installmentFinalPrice = spotlightPrice;

               if (interestFee != null && interestFee > 0) {
                  installmentFinalPrice = MathUtils.normalizeTwoDecimalPlaces(spotlightPrice + (spotlightPrice * (interestFee / 100d)));


                  if (installmentFinalPrice != null) {

                     Double installmentPrice = installmentFinalPrice / installmentNumber;



                     installments.add(InstallmentBuilder.create()
                           .setInstallmentNumber(installmentNumber)
                           .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(installmentPrice))
                           .build());
                  }
               }
            }
         }
      }

      return installments;

   }


}
