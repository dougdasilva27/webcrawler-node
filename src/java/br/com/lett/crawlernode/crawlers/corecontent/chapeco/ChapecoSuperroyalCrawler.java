package br.com.lett.crawlernode.crawlers.corecontent.chapeco;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.commons.lang.WordUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class ChapecoSuperroyalCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.superroyal.com.br/";
   private static final String MAIN_SELLER_NAME = "super royal";
   private static final String STORE_ID = "18"; // at the moment this crawlers was made i've founded only this storeId
   private static final String API_URL = "https://api.superroyal.com.br/graphql";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ChapecoSuperroyalCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) && href.contains("product/"));
   }

   private JSONObject fetchApi() {
      String id = scrapId();

      String payload = "{\"operationName\":\"ProductDetailQuery\",\"variables\":{\"storeId\":\"" + STORE_ID + "\",\"productId\":\"" + id + "\"},"
         + "\"query\":\"query ProductDetailQuery($storeId: ID!, $productId: ID!) {\\n  publicViewer {\\n    id\\n    "
         + "product(id: $productId) {\\n      id\\n      name\\n      description\\n      content\\n      saleUnit\\n      "
         + "contentUnit\\n      type\\n      slug\\n      tags\\n      brand {\\n        id\\n        name\\n        __typename\\n"
         + "      }\\n      image {\\n        url\\n        __typename\\n      }\\n      quantity(storeId: $storeId) {\\n        "
         + "min\\n        max\\n        fraction\\n        inStock\\n        __typename\\n      }\\n      pricing(storeId: $storeId) "
         + "{\\n        id\\n        promotion\\n        price\\n        promotionalPrice\\n        __typename\\n      }\\n      "
         + "personas(storeId: $storeId) {\\n        personaPrice\\n        personaId\\n        __typename\\n      }\\n      "
         + "level1Category(storeId: $storeId) {\\n        name\\n        slug\\n        __typename\\n      }\\n      "
         + "level2Category(storeId: $storeId) {\\n        id\\n        name\\n        slug\\n        parent {\\n          "
         + "id\\n          name\\n          slug\\n          __typename\\n        }\\n        __typename\\n      }\\n      "
         + "level3Category(storeId: $storeId) {\\n        id\\n        name\\n        slug\\n        level2Category: parent {\\n         "
         + " level1Category: parent {\\n            id\\n            name\\n            slug\\n            __typename\\n          }\\n "
         + "         id\\n          name\\n          slug\\n          __typename\\n        }\\n        __typename\\n      }\\n      "
         + "similar(storeId: $storeId) {\\n        id\\n        name\\n        slug\\n        image {\\n          url\\n          "
         + "__typename\\n        }\\n        brand {\\n          name\\n          __typename\\n        }\\n      "
         + "  pricing(storeId: $storeId) {\\n          id\\n          promotion\\n          price\\n          promotionalPrice\\n"
         + "          __typename\\n        }\\n        level1Category(storeId: $storeId) {\\n          name\\n          __typename\\n"
         + "        }\\n        __typename\\n      }\\n      __typename\\n    }\\n    store(id: $storeId) {\\n      id\\n"
         + "      productConfig {\\n        id\\n        displayPercentageOfDiscount\\n        displayNormalPrice\\n        "
         + "displayUnitContent\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n}\\n\"}";

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("origin", "https://www.superroyal.com.br");
      headers.put("referer", "https://www.superroyal.com.br/");
      headers.put("authority", "api.superroyal.com.br");

      Request request = RequestBuilder.create()
         .setUrl(API_URL)
         .setCookies(cookies)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY))
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), new ApacheDataFetcher()), session, "post");
      JSONObject jsonObject = JSONUtils.stringToJson(response.getBody());

      return jsonObject != null ? jsonObject : new JSONObject();
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject json = fetchApi();

      JSONObject productObject = JSONUtils.getValueRecursive(json, "data*publicViewer*product", "*", JSONObject.class, new JSONObject());

      if (!productObject.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productObject.optString("id");
         String internalPid = internalId;
         String name = WordUtils.capitalize(productObject.optString("name"));
         CategoryCollection categories = scrapCategories(productObject);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".active-image img", Arrays.asList("src"), "https", "");
         String description = productObject.optString("description");

         Offers offers = scrapOffers(productObject);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
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


   private String scrapId() {
      return this.session.getOriginalURL().split("/")[4];
   }

   private CategoryCollection scrapCategories(JSONObject productObject) {
      CategoryCollection categories = new CategoryCollection();

      for (int i = 1; i < 4; i++) {
         JSONObject catJson = productObject.optJSONObject("level" + i + "Category");

         if (catJson != null) {
            categories.add(catJson.optString("name", ""));
         }
      }

      return categories;
   }

   private Offers scrapOffers(JSONObject productObject) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productObject);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject productObject) throws MalformedPricingException {
      JSONObject pricing = productObject.optJSONObject("pricing");

      if (pricing != null) {
         Double spotlightPrice = pricing.optDouble("price");

         if (spotlightPrice != null && spotlightPrice > 0d) {
            Double priceFrom = null;
            CreditCards creditCards = scrapCreditCards(spotlightPrice);

            return PricingBuilder.create()
               .setSpotlightPrice(spotlightPrice)
               .setPriceFrom(priceFrom)
               .setCreditCards(creditCards)
               .build();
         }
      }

      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }
}
