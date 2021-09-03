package br.com.lett.crawlernode.crawlers.corecontent.panama;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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

import java.util.*;

public class PanamaSuperxtraCrawler extends Crawler {


   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public PanamaSuperxtraCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected JSONObject fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authority", "deadpool.instaleap.io");
      headers.put("accept", "*/*");
      headers.put("origin", "https://domicilio.superxtra.com");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");

      String productSlug = CommonMethods.getLast(session.getOriginalURL().split("/"));
      String payload = "{\"variables\":{\"storeId\":\"22\",\"filter\":{\"sku\":{\"eq\":\"" + productSlug + "\"}}},\"query\":\"query( $pagination: paginationInput $search: SearchInput $storeId: ID! $categoryId: ID $onlyThisCategory: Boolean $filter: ProductsFilterInput $orderBy: productsSortInput ) { getProducts( pagination: $pagination search: $search storeId: $storeId categoryId: $categoryId onlyThisCategory: $onlyThisCategory filter: $filter orderBy: $orderBy ) { redirectTo products { id name photosUrls sku unit price specialPrice promotion { description type isActive conditions __typename } stock nutritionalDetails clickMultiplier subQty subUnit maxQty minQty specialMaxQty ean boost showSubUnit isActive slug categories { id name __typename } __typename } paginator { pages page __typename } __typename } } \"}";

      String url = "https://deadpool.instaleap.io/api/v2";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .setCookies(cookies)
         .build();

      Response response = this.dataFetcher.post(session, request);
      return JSONUtils.stringToJson(response.getBody());
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
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         Offers offers = scrapOffers(productJson);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
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

      for (Object o : categoriesArray) {
         JSONObject category = (JSONObject) o;
         categories.add(category.optString("name"));
      }

      return categories;
   }

   protected List<String> scrapImages(JSONObject json) {
      List<String> images = new ArrayList<>();

      JSONArray categoriesArray = json.optJSONArray("photosUrls");
      categoriesArray.forEach(x -> images.add(x.toString()));

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
