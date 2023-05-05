package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilAtacadaomarketplaceCrawler extends Crawler {

   private String cityId = session.getOptions().optJSONObject("cookies").optString("cb_user_city_id");

   private String userType = session.getOptions().optJSONObject("cookies").optString("cb_user_type");
   private final String SELLER_NAME = session.getOptions().optString("store");

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public BrasilAtacadaomarketplaceCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      JSONObject cookiesObject = session.getOptions() != null ? session.getOptions().optJSONObject("cookies") : null;
      if (cookiesObject != null) {
         Map<String, Object> cookiesMap = cookiesObject.toMap();
         for (Map.Entry<String, Object> entry : cookiesMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            cookies.add(new BasicClientCookie(key, value.toString()));
         }
      }
   }

   @Override
   protected Object fetch() {
      String url = this.session.getOriginalURL();
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "*/*");
      headers.put("Cookie", CommonMethods.cookiesToString(cookies));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.LUMINATI_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY
            )
         )
         .setSendUserAgent(true)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher, true);

      return response;
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".container > h1.h1", false);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".card div[data-pk]", "data-pk");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".card > div > div.row:not(:first-child)"));
//         List<String> categoriesArray = Arrays.asList(CrawlerUtils.scrapStringSimpleInfo(doc, ".breadcrumb-item:not(:first-child) a", false).split("/"));
//         if (categoriesArray.size() > 3) {
//            categoriesArray = categoriesArray.subList(0, 3);
//         }
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".card .product-image-box > img[src]", "src");
         JSONObject skuInfo = requestFromApi(internalId);
         JSONArray price = JSONUtils.getValueRecursive(skuInfo, "offers", JSONArray.class);
         boolean available = doc.select(".js-btn-add-product") != null;
         Offers offers = available ? scrapOffers(price) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            //.setCategories(categoriesArray)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private JSONObject requestFromApi(String internalId) {
      Map<String, String> headers = new HashMap<>();
      headers.put("x-requested-with", "XMLHttpRequest");

      String url = "https://www.atacadao.com.br/dashboard/sku/api/" + internalId + "/?city_id=" + this.cityId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.BUY_HAPROXY,
               ProxyCollection.LUMINATI_SERVER_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY
            )
         )
         .setSendUserAgent(true)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, dataFetcher, true);

      return CrawlerUtils.stringToJson(response.getBody());
   }


   private Offers scrapOffers(JSONArray prices) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      for (Object p : prices) {
         JSONObject price = (JSONObject) p;
         String seller = scrapSeller(price);
         Pricing pricing = scrapPricing(price);
         List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

         offers.add(new Offer.OfferBuilder()
            .setIsBuybox(false)
            .setPricing(pricing)
            .setSales(sales)
            .setSellerFullName(seller)
            .setIsMainRetailer(seller.equalsIgnoreCase(SELLER_NAME))
            .setUseSlugNameAsInternalSellerId(true)
            .build());

      }
      return offers;
   }

   private String scrapSeller(JSONObject price) {
      return JSONUtils.getValueRecursive(price, "distributor.name", String.class);
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = data.optDouble("unit_price");
      Double priceFrom = null;

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".box-product-information") != null;
   }

}
