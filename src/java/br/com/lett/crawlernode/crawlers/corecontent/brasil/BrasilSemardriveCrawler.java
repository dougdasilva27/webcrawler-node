package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
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
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class BrasilSemardriveCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(),  Card.AMEX.toString(), Card.AURA.toString(), Card.CABAL.toString(), Card.ELO.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(), Card.SOROCRED.toString());
   private static final String SELLER_NAME = "Semar";
   public BrasilSemardriveCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      String script = CrawlerUtils.scrapScriptFromHtml(document, "#CC-schema-org-server");
      JSONArray scriptJsonArray = CrawlerUtils.stringToJsonArray(script);
      String productInternalId  = scriptJsonArray.optQuery("/0/offers/0/itemOffered/productID").toString();
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
   private String getDescription(JSONObject productDataJson){
      String description = "";
      String productDescription1 = productDataJson.optString("x_descricaoLonga1");
      String productDescription2 = productDataJson.optString("x_descricaoLonga2");
      String productDescription3 = productDataJson.optString("x_descrioLonga3");
      if(productDescription1 != ""){
         description = description + productDescription1;
      }
      if(productDescription2 != ""){
         description = description + productDescription2;
      }
      if(productDescription3 != ""){
         description = description + productDescription3;
      }
      return description;
   }
   private Offers scrapOffers(JSONObject productDataJson, String productInternalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Boolean stock = isAvailable(productInternalId);
      if(stock == true){
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

      Double price = JSONUtils.getDoubleValueFromJSON(productDataJson,"salePrice", true);
      JSONObject priceFromJson = productDataJson.getJSONObject("listPrices");
      Double priceFrom = priceFromJson.optDouble("10_default_price", 0.0);

      if (priceFrom == 0.0) {
         priceFrom = priceFromJson.optDouble("29_default_price", 0.0);
      }

      if (price == null){
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

   protected Boolean isAvailable(String code){
      String url = "https://www.semarentrega.com.br/ccstore/v1/inventories?fields=skuId,locationInventoryInfo,stockLevel";
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      JSONObject payload = new JSONObject();
      payload.put("ids", code);
      payload.put("locationIds", session.getOptions().optString("storeId"));
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();
      String response = new JavanetDataFetcher().post(session, request).getBody();
      JSONObject reponseJson = CrawlerUtils.stringToJson(response);
      Object objStock = reponseJson.optQuery("/items/0/locationInventoryInfo/0/availabilityStatusMsg");
      String stock = objStock.toString();
      return stock.equals("inStock");
   }

   protected String captureData(String code){
      String url = "https://www.semarentrega.com.br/ccstore/v1/products/"+code;
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      String response = new JavanetDataFetcher().get(session, request).getBody();
      return  response;
   }
}
