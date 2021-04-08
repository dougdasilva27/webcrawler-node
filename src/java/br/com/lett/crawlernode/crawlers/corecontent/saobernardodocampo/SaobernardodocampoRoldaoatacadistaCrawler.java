package br.com.lett.crawlernode.crawlers.corecontent.saobernardodocampo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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

public class SaobernardodocampoRoldaoatacadistaCrawler extends Crawler {

   private static final String SELLER_NAME = "Roldao Atacadista";
   private static final String STORE_ID = "34";

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public SaobernardodocampoRoldaoatacadistaCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, "script#__NEXT_DATA__", null, null, false, true);
      JSONObject pageProps = jsonObject != null ? JSONUtils.getValueRecursive(jsonObject, "props.pageProps", JSONObject.class) : null;
      if (pageProps != null && pageProps.has("product")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject productInfo = pageProps.optJSONObject("product");
         if (productInfo != null) {
            JSONArray photos = JSONUtils.getValueRecursive(productInfo, "photos", JSONArray.class);

            String internalId = productInfo.optString("sku");
            String internalPid = photos != null ? JSONUtils.getValueRecursive(photos, "0.name", String.class) : null;
            String name = productInfo.optString("name");
            String categories = JSONUtils.getValueRecursive(productInfo, "department.description", String.class);
            List<String> images = photos != null ? CrawlerUtils.scrapImagesListFromJSONArray(photos, "url", null, "https", "api.roldao.com.br/fotos/", session) : null;
            String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
            String description = productInfo.optString("description");
            int stock = getStock(productInfo);
            boolean available = stock > 0;
            Offers offers = available ? scrapOffers(productInfo) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setCategories(Collections.singleton(categories))
               .setStock(stock)
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = scrapSales(product);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private int getStock(JSONObject product) {
      JSONArray inventory = product.optJSONArray("inventory");
      for (Object obj : inventory) {
         if (obj instanceof JSONObject) {
            JSONObject storeJson = (JSONObject) obj;
            String store = storeJson.optString("branchId");
            if (store.equals(STORE_ID)) {
               return storeJson.optInt("totalAvailable");
            }
         }
      }
      return 0;
   }

   private List<String> scrapSales(JSONObject product) {
      List<String> sales = new ArrayList<>();
      StringBuilder stringBuilder = new StringBuilder();
      JSONArray prices = product.optJSONArray("prices");
      for (Object obj : prices) {
         if (obj instanceof JSONObject) {
            JSONObject price = (JSONObject) obj;
            String description = price.optString("description");
            if (description.equals("ATACADO")) {
               int qty = price.optInt("minQuantity");
               double value = price.optDouble("price");
               stringBuilder.append("compre").append(qty).append(" unidades e pague R$");
               stringBuilder.append(value).append(" cada");
               sales.add(stringBuilder.toString());
               return sales;


            }
         }
      }


      return sales;
   }

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double spotlightPrice = product.optDouble("price");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      //Site hasn't any product with old price

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
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

   //Site hasn't rating


}
