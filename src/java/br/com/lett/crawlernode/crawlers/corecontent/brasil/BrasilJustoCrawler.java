package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
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
import org.apache.http.HttpHeaders;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilJustoCrawler extends Crawler {
   private static final String HOME_PAGE = "https://soujusto.com.br/";
   private static final String SELLER_FULL_NAME = "Justo";
   private final String POSTAL_CODE = getPostalCode();

   public BrasilJustoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
      super.config.setFetcher(FetchMode.HTTPCLIENT);
   }

   private String getPostalCode() {
      return session.getOptions().getString("postal_code");
   }

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("postal_code", POSTAL_CODE);
      cookie.setDomain("soujusto.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      JSONObject data = new JSONObject();
      List<Product> products = new ArrayList<>();
      String jsonString = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
      JSONArray arr = JSONUtils.stringToJsonArray(jsonString);
      if (arr != null && !arr.isEmpty()) {
         String buildId = JSONUtils.getValueRecursive(arr, "0.props.pageProps.product.id", String.class);
         data = fetchJSON(buildId);
      }

      if (jsonString != null && !jsonString.isEmpty() && data != null && !data.isEmpty()) {
         JSONArray dataArr = JSONUtils.stringToJsonArray(jsonString);
         JSONObject productJson = JSONUtils.getValueRecursive(dataArr, "0.props.pageProps.product", JSONObject.class);
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = productJson.optString("sku");
         List<String> eans = new ArrayList<>();

         String name = productJson.optString("name");
         boolean isAvailable = crawlAvailability(productJson);
         CategoryCollection categories = crawlCategories(productJson);
         String primaryImage = JSONUtils.getValueRecursive(productJson, "images.0.url", String.class);
         List<String> secondaryImages = getSecondaryImages(productJson);
         String description = productJson.optString("description");
         Offers offers = isAvailable ? crawlOffers(data) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private JSONObject fetchJSON(String buildId) {
      String payload = "{\"operationName\":\"getProduct\",\"variables\":{\"productId\":\"" + buildId + "\",\"onlyEnabledVariants\":true},\"query\":\"fragment TaxedMoneyFragment on TaxedMoney {\\n  gross {\\n    amount\\n    currency\\n    localized\\n    __typename\\n  }\\n  __typename\\n}\\n\\nfragment CategoryFragment on Category {\\n  id\\n  name\\n  __typename\\n}\\n\\nfragment ShoppingListFragment on ShoppingList {\\n  id\\n  name\\n  __typename\\n}\\n\\nfragment MoneyFragment on Money {\\n  localized\\n  amount\\n  currency\\n  __typename\\n}\\n\\nfragment ProductFragment on Product {\\n  id\\n  name\\n  isAvailable\\n  url\\n  sku\\n  maxQuantityAllowed\\n  label\\n  labelFontColor\\n  labelBackgroundColor\\n  showPriceWeightUnit\\n  variants {\\n    id\\n    name\\n    stockQuantity\\n    weightUnit\\n    isPiece\\n    bundle {\\n      discountPrice {\\n        ...MoneyFragment\\n        __typename\\n      }\\n      discountMinQuantity\\n      discountLabel\\n      __typename\\n    }\\n    maturationOptions {\\n      description\\n      name\\n      type\\n      __typename\\n    }\\n    __typename\\n  }\\n  shoppingList {\\n    ...ShoppingListFragment\\n    __typename\\n  }\\n  category {\\n    ...CategoryFragment\\n    __typename\\n  }\\n  thumbnail {\\n    url\\n    __typename\\n  }\\n  availability {\\n    discountPercentage\\n    lineMaturationOptions\\n    quantityOnCheckout\\n    variantOnCheckout\\n    priceRange {\\n      start {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      stop {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      __typename\\n    }\\n    priceRangeUndiscounted {\\n      start {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      stop {\\n        ...TaxedMoneyFragment\\n        __typename\\n      }\\n      __typename\\n    }\\n    __typename\\n  }\\n  __typename\\n}\\n\\nquery getProduct($productId: ID!, $onlyEnabledVariants: Boolean) {\\n  product(id: $productId, onlyEnabledVariants: $onlyEnabledVariants) {\\n    ...ProductFragment\\n    showPriceWeightUnit\\n    __typename\\n  }\\n}\\n\"}";
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("authority", "soujusto.com.br");
      headers.put("origin", "https://soujusto.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://soujusto.com.br/graphql/")
         .setHeaders(headers)
         .setPayload(payload)
         .setCookies(this.cookies)
         .mustSendContentEncoding(false)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher()), session, "post");

      return CrawlerUtils.stringToJson(response.getBody());
   }

   private List<String> getSecondaryImages(JSONObject imageInfo) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray imagesList = JSONUtils.getJSONArrayValue(imageInfo, "images");

      if (imagesList != null && !imagesList.isEmpty()) {
         for (Object e : imagesList) {
            JSONObject obj = (JSONObject) e;
            String imageUrl = obj.optString("url");
            if (imageUrl != null && !imageUrl.isEmpty()) {
               secondaryImages.add(imageUrl);
            }
         }
         if (secondaryImages.size() > 0) {
            secondaryImages.remove(0);
         }
      }

      return secondaryImages;
   }

   private Offers crawlOffers(JSONObject obj) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(obj);
      List<String> sales = scrapSales(pricing);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject obj) throws MalformedPricingException {
      Double[] prices = scrapPrices(obj);
      Double priceFrom = null;
      Double spotlightPrice = null;
      if (prices.length >= 2) {
         priceFrom = prices[0];
         spotlightPrice = prices[1];
      }

      if (spotlightPrice != null) {
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
      } else {
         return null;
      }
   }

   private Double[] scrapPrices(JSONObject obj) {
      Double spotlightPrice = JSONUtils.getValueRecursive(obj, "data.product.availability.priceRange.start.gross.amount", Double.class);
      Double priceFrom = JSONUtils.getValueRecursive(obj, "data.product.availability.priceRangeUndiscounted.start.gross.amount", Double.class);

      if (Objects.equals(spotlightPrice, priceFrom)) {
         priceFrom = null;
      }

      return new Double[]{priceFrom, spotlightPrice};
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);
      sales.add(saleDiscount);

      return sales;
   }

   private boolean crawlAvailability(JSONObject obj) {
      return obj.optBoolean("isAvailable");
   }

   private CategoryCollection crawlCategories(JSONObject obj) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray jsonCategories = JSONUtils.getValueRecursive(obj, "category.ancestors.edges", JSONArray.class);
      categories.add(JSONUtils.getValueRecursive(obj, "category.name", String.class));

      for (Object o : jsonCategories) {
         JSONObject json = (JSONObject) o;
         categories.add(JSONUtils.getValueRecursive(json, "node.name", String.class));
      }

      return categories;
   }
}
