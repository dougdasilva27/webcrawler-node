package br.com.lett.crawlernode.crawlers.corecontent.guatemala;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class GuatemalaPaizCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());
   private static final String SELLER_NAME = "paiz";

   public GuatemalaPaizCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", session.getOptions().getString("cookie"));


      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setHeaders(headers).build();
      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());

   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (doc != null) {
         JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, "}", false, true);

         if (!productJson.isEmpty()) {

            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalId = productJson.optString("sku");
            String internalPid = productJson.optString("mpn");
            String name = productJson.optString("name");
            String primaryImage = productJson.optString("image");
            JSONObject offersJson = productJson.optJSONObject("offers");
            Offers offers = offersJson != null && !offersJson.isEmpty() ? scrapOffers(offersJson) : new Offers();
            List<String> eans = new ArrayList<>();
            String ean = CrawlerUtils.scrapStringSimpleInfo(doc, ".vtex-product-identifier-0-x-product-identifier__value", true);
            if (ean != null) {
               eans.add(ean);

            }

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setOffers(offers)
               .setEans(eans)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String salesOnJson = CrawlerUtils.calculateSales(pricing);
      if (salesOnJson != null) {
         sales.add(salesOnJson);

      }
      return sales;
   }

   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productInfo, "lowPrice", true);
      Double priceFrom = JSONUtils.getValueRecursive(productInfo, "highPrice", Double.class);
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

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

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

}
