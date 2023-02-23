package br.com.lett.crawlernode.crawlers.corecontent.costarica;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
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
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CostaricaAutomercadoCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public CostaricaAutomercadoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   private static final String SELLER_NAME = "automercado";
   private final String STORE_ID = session.getOptions().optString("store_id");

   @Override
   protected JSONObject fetch() {
      String internalId = getProductId();
      String API = "https://fu5xfx7knl-3.algolianet.com/1/indexes/*/queries?x-algolia-agent=Algolia for JavaScript (4.12.0); Browser (lite)&x-algolia-api-key=113941a18a90ae0f17d602acd16f91b2&x-algolia-application-id=FU5XFX7KNL";

      String payload = "{\"requests\":[{\"indexName\":\"Product_CatalogueV2\",\"params\":\"facetFilters=%5B%22productID%3A" + internalId + "%22%2C%5B%22storeDetail." + STORE_ID + ".storeid%3A" + STORE_ID + "%22%5D%5D&facets=%5B%22marca%22%2C%22addedSugarFree%22%2C%22fiberSource%22%2C%22lactoseFree%22%2C%22lfGlutemFree%22%2C%22lfOrganic%22%2C%22lfVegan%22%2C%22lowFat%22%2C%22lowSodium%22%2C%22preservativeFree%22%2C%22sweetenersFree%22%2C%22parentProductid%22%2C%22parentProductid2%22%2C%22parentProductid_URL%22%2C%22catecom%22%5D\"}]}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY
         ))
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().mustUseMovingAverage(true).build())
         .mustSendContentEncoding(false)
         .setSendUserAgent(true)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   private String getProductId() {
      String internalId = null;
      Pattern pattern = Pattern.compile("id\\/(.*)\\?");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         internalId = matcher.group(1);
      }
      if (internalId == null) {
         return CommonMethods.getLast(session.getOriginalURL().split("id/"));
      }
      return internalId;

   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();
      JSONObject productData = JSONUtils.getValueRecursive(json, "results.0.hits.0", ".", JSONObject.class, new JSONObject());

      if (productData != null && !productData.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getProductId();
         String internalPid = productData.optString("productNumber");
         Boolean available = JSONUtils.getValueRecursive(productData, "storeDetail." + STORE_ID + ".productAvailable", ".", Boolean.class, false);
         String name = productData.optString("ecomDescription");
         String primaryImage = productData.optString("imageUrl") != null ? productData.optString("imageUrl").replace(".jpg", "_1.jpg") : null;
         List<String> secondaryImages = scrapSecondaryImages();
         String description = productData.optString("descriptiveParagraph");
         Offers offers = available ? scrapOffers(productData) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
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

   private List<String> scrapSecondaryImages() {
      Map<String,String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT,"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
      headers.put(HttpHeaders.ACCEPT_LANGUAGE,"pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put(HttpHeaders.CACHE_CONTROL,"max-age=0");
      headers.put(HttpHeaders.CONNECTION,"keep-alive");
      headers.put(HttpHeaders.USER_AGENT,"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");

      List<String> secondaryImages = new ArrayList<>();
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.SMART_PROXY_CL_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY
         ))
         .setSendUserAgent(true)
         .build();
      int attempt = 0;
      boolean productPage = false;
      List<DataFetcher> dataFetcherList = List.of(this.dataFetcher, new JsoupDataFetcher(), new FetcherDataFetcher(), new ApacheDataFetcher());
      do {
         Response response = CrawlerUtils.retryRequest(request, session, dataFetcherList.get(attempt), true);
         if (response.isSuccess() && !response.getBody().isEmpty()) {
            Document doc = Jsoup.parse(response.getBody());
            productPage = doc.selectFirst(".ng-star-inserted > .container.mb-5.mt-4") != null;
            if (productPage) {
               Elements divImages = doc.select("li > .img-fluid");
               for (Element e : divImages) {
                  String image = e.attr("src");
                  if (image != null && !image.isEmpty()) {
                     secondaryImages.add(image);
                  }
               }
               if (secondaryImages.size() > 0) {
                  secondaryImages.remove(0);
               }
            }
         }
         attempt++;
      } while (attempt < dataFetcherList.size() && !productPage);
      if (!productPage) {
         Logging.printLogDebug(logger, session, "Request for HTML failed" + this.session.getOriginalURL());
      }
      return secondaryImages;
   }

   private Offers scrapOffers(JSONObject productData) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productData);
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

   private Pricing scrapPricing(JSONObject productData) throws MalformedPricingException {

      Double spotlightPrice = JSONUtils.getValueRecursive(productData, "storeDetail." + STORE_ID + ".amount", ".", Double.class, null);
      Double priceFrom = JSONUtils.getValueRecursive(productData, "storeDetail." + STORE_ID + ".basePrice", ".", Double.class, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();


      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom != null && !priceFrom.equals(spotlightPrice) ? priceFrom : null)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);

      sales.add(sale);
      return sales;
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
