package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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

import java.util.*;

public class BrasilDakiCrawler extends Crawler {
   public BrasilDakiCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private final String hubId = session.getOptions().optString("hubId");

   protected Response fetchResponse() {
      String skuProduct = session.getOriginalURL().split("__")[1].split("/")[0];

      String payload = "{\"operationName\":\"ProductDetails\",\"" +
         "variables\":{\"hubId\":\"" + hubId + "\",\"where\":{\"sku_in\":[\"" + skuProduct + "\"]}},\"" +
         "query\":\"query ProductDetails($where: ProductFilters!, $hubId: String!) {\\n  products(where: $where) {\\n    brand\\n    category {\\n      cmsMainCategory {\\n        title\\n        __typename\\n      }\\n      __typename\\n    }\\n    inventory(hubId: $hubId) {\\n      quantity\\n      showOutOfStock\\n      status\\n      maxQuantity\\n      __typename\\n    }\\n    long_description\\n    ui_content_1\\n    packshot1_front_grid {\\n      url\\n      __typename\\n    }\\n    product_status\\n    price(hubId: $hubId) {\\n      amount\\n      compareAtPrice\\n      discount\\n      id\\n      sku\\n      __typename\\n    }\\n    name\\n    title\\n    sku\\n    tags\\n    __typename\\n  }\\n}\"}";

      Response response = postApiRequest(payload);
      return response;
   }

   private Response postApiRequest(String payload) {
      HashMap<String, String> headers = new HashMap<>();

      headers.put("Content-Type", "application/json");
      headers.put("Content-Length", "<calculated when request is sent>");
      headers.put("Host", "<calculated when request is sent>");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api-prd-br.jokrtech.com/")
         .setPayload(payload)
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);
      return response;
   }

   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("data")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject productSku = JSONUtils.getValueRecursive(json, "data.products.0", JSONObject.class);
         Map<String, JSONObject> productSkuPrice = new HashMap<>();

         String internalPid = productSku.optString("sku");
         String description = productSku.optString("long_description");
         String primaryImage = JSONUtils.getValueRecursive(productSku, "packshot1_front_grid.url", String.class);
         String name = JSONUtils.getValueRecursive(productSku, "name", String.class);

         boolean isAvailable = JSONUtils.getValueRecursive(productSku, "inventory.quantity", Integer.class) > 0;

         Offers offers = isAvailable ? scrapOffers(productSku) : new Offers();

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


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private Offers scrapOffers(JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("daki")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getValueRecursive(json, "price.amount", Double.class);
      Double priceFrom = JSONUtils.getValueRecursive(json, "price.compareAtPrice", Double.class, 0d);
      if (priceFrom == 0) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlighPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlighPrice)
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
