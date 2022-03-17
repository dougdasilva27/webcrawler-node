package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class BrasilSemardriveCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.AMEX.toString(), Card.AURA.toString(), Card.CABAL.toString(), Card.ELO.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(), Card.SOROCRED.toString());
   private static final String SELLER_NAME = "Semar";

   public BrasilSemardriveCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      String productInternalId = crawlId(document);
      List<Product> products = new ArrayList<>();
      String productData = captureData(productInternalId);
      JSONObject productDataJson = CrawlerUtils.stringToJson(productData);
      String productName = productDataJson.getString("displayName");
      String productPrimaryImage = "https://www.semarentrega.com.br" + productDataJson.optString("primaryFullImageURL");
      String productDescription = getDescription(productDataJson);

      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(productInternalId)
         .setInternalPid(productInternalId)
         .setName(productName)
         .setPrimaryImage(productPrimaryImage)
         .setDescription(productDescription)
         .setOffers(scrapOffers(productDataJson, productInternalId))
         .build();
      products.add(product);
      return products;


   }

   private String getDescription(JSONObject productDataJson) {
      String description = "";
      String productDescription1 = productDataJson.optString("x_descricaoLonga1");
      String productDescription2 = productDataJson.optString("x_descricaoLonga2");
      String productDescription3 = productDataJson.optString("x_descrioLonga3");
      if (productDescription1 != "") {
         description = description + productDescription1;
      }
      if (productDescription2 != "") {
         description = description + productDescription2;
      }
      if (productDescription3 != "") {
         description = description + productDescription3;
      }
      return description;
   }

   private Offers scrapOffers(JSONObject productDataJson, String productInternalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Boolean stock = isAvailable(productInternalId);
      if (stock == true) {
         Pricing pricing = scrapPricing(productDataJson, productInternalId);
         List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
         offers.add(new Offer.OfferBuilder()
            .setIsBuybox(false)
            .setPricing(pricing)
            .setSellerFullName(SELLER_NAME)
            .setIsMainRetailer(true)
            .setUseSlugNameAsInternalSellerId(true)
            .setSales(sales)
            .build()
         );
      }
      return offers;
   }

   private Pricing scrapPricing(JSONObject productDataJson, String id) throws MalformedPricingException {

      Double price = JSONUtils.getDoubleValueFromJSON(productDataJson, "salePrice", true);
      JSONObject priceFromJson = productDataJson.getJSONObject("listPrices");
      Double priceFrom = priceFromJson.optDouble("10_default_price", 0.0);

      if (priceFrom == 0.0) {
         priceFrom = priceFromJson.optDouble("29_default_price", 0.0);
      }

      if (price == null) {
         price = priceFrom;
         priceFrom = null;
      }

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(price, cards);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   protected Boolean isAvailable(String code) {
      int objStock = 0;
      String url = "https://www.semarentrega.com.br/ccstore/v1/stockStatus?products=" + code;
      Map<String, String> headers = new HashMap<>();
      headers.put("products", code);
      JSONObject payload = new JSONObject();
      payload.put("products", code);
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.BUY_HAPROXY))
         .build();
      String response = this.dataFetcher.get(session, request).getBody();
      JSONObject reponseJson = CrawlerUtils.stringToJson(response);
      Object pointer = reponseJson.optQuery("/items/0/productSkuInventoryStatus/" + code);
      if (pointer != null) {
         objStock = Integer.parseInt(String.valueOf(pointer));
      }
      return objStock != 0;
   }

   protected String captureData(String code) {
      String url = "https://www.semarentrega.com.br/ccstore/v1/products/" + code;
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.BUY_HAPROXY))
         .build();
      String response = this.dataFetcher.get(session, request).getBody();
      return response;
   }

   protected String crawlId(Document document) throws Exception {
      String internalID = null;
      String scrapId = CrawlerUtils.scrapScriptFromHtml(document, "body > script:nth-child(9)");
      String parseToJson = scrapId.substring(15, scrapId.length() - 2);
      JSONObject scriptJsonObj = CrawlerUtils.stringToJSONObject(parseToJson);
      if (scrapId != null) {
         internalID = scriptJsonObj.optQuery("/config/ccNavState/pageContext").toString();
      }

      return internalID;
   }
}
