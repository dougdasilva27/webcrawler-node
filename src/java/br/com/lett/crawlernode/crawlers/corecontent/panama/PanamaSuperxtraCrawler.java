package br.com.lett.crawlernode.crawlers.corecontent.panama;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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

import java.util.*;

public class PanamaSuperxtraCrawler extends Crawler {


   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public PanamaSuperxtraCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authority", "deadpool.instaleap.io");
      headers.put("accept", "*/*");
      headers.put("content-type", "application/json");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("origin", "https://domicilio.superxtra.com");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("referer", session.getOriginalURL());

      String productSlug = CommonMethods.getLast(session.getOriginalURL().split("/"));
      String payload = "{\"operationName\":\"GetProducts\",\"variables\":{\"storeId\":\"70\",\"filter\":{\"sku\":{\"eq\":\"" + productSlug + "\"}},\"variants\":false},\"query\":\"query GetProducts($pagination: paginationInput, $search: SearchInput, $storeId: ID!, $categoryId: ID, $onlyThisCategory: Boolean, $filter: ProductsFilterInput, $orderBy: productsSortInput, $variants: Boolean) {\\n  getProducts(pagination: $pagination, search: $search, storeId: $storeId, categoryId: $categoryId, onlyThisCategory: $onlyThisCategory, filter: $filter, orderBy: $orderBy, variants: $variants) {\\n    redirectTo\\n    products {\\n      id\\n      description\\n      name\\n      photosUrls\\n      sku\\n      unit\\n      price\\n      specialPrice\\n      promotion {\\n        description\\n        type\\n        isActive\\n        conditions\\n        __typename\\n      }\\n      variants {\\n        selectors\\n        productModifications\\n        __typename\\n      }\\n      stock\\n      nutritionalDetails\\n      clickMultiplier\\n      subQty\\n      subUnit\\n      maxQty\\n      minQty\\n      specialMaxQty\\n      ean\\n      boost\\n      showSubUnit\\n      isActive\\n      slug\\n      categories {\\n        id\\n        name\\n        __typename\\n      }\\n      formats {\\n        format\\n        equivalence\\n        unitEquivalence\\n        __typename\\n      }\\n      __typename\\n    }\\n    paginator {\\n      pages\\n      page\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";
      String url = "https://deadpool.instaleap.io/api/v2";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
            ))
         .setSendUserAgent(false)
         .build();

      Response response = this.dataFetcher.post(session, request);

      if (!response.isSuccess()){
         response = new FetcherDataFetcher().post(session, request);
      }

      return response;

   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      JSONArray arrayProducts = JSONUtils.getValueRecursive(json, "data.getProducts.products", JSONArray.class);

      if (arrayProducts != null && !arrayProducts.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject productJson = arrayProducts.optJSONObject(0);

         String internalPid = productJson.optString("sku");
         String internalId = productJson.optString("id");
         String name = productJson.optString("name");
         CategoryCollection categories = scrapCategories(productJson);
         List<String> images = scrapImages(productJson);
         String description = productJson.optString("description");
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         Offers offers = scrapOffers(productJson);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setDescription(description)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   protected CategoryCollection scrapCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      JSONArray categoriesArray = json.optJSONArray("categories");

      if (categoriesArray != null) {
         for (Object o : categoriesArray) {
            JSONObject category = (JSONObject) o;
            categories.add(category.optString("name"));
         }
      }

      return categories;
   }

   protected List<String> scrapImages(JSONObject json) {
      List<String> images = new ArrayList<>();

      JSONArray imagesArray = json.optJSONArray("photosUrls");

      if (imagesArray != null) {
         imagesArray.forEach(x -> images.add(x.toString()));
      }

      return images;
   }

   private Offers scrapOffers(JSONObject jsonObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(jsonObject);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("superxtra")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "specialPrice", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "price", false);

      if (spotlightPrice == 0d) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
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
}
