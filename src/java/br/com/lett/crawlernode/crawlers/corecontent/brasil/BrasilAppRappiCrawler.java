package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import cdjd.com.google.common.net.HttpHeaders;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BrasilAppRappiCrawler extends Crawler{
   private static final String SELLER_FULL_NAME = "rappi app";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());
   private static final String baseImageUrl = "https://images.rappi.com.br/products/";

   public BrasilAppRappiCrawler(Session session) {
      super(session);
      config.setParser(Parser.JSON);
   }

   private JSONObject fetchDocument() {

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json; charset=UTF-8");
      headers.put("Authorization", "Bearer ft.gAAAAABkXTUqW4WZs3GQ4TwL7iXdRd_6_So4YqGDeJTbK_pLq25-mj1SYmoVpEcqC2FiRufXyE8VA265rUbYNUTK_hn7TV6Su13OyZEXjobm9Dxs1VshF1tbUPEFW_H0qhre7e4YnnZ5j1wamsRb228mzuAz2PCdjit0576uNDTGDJd-vXET3g0Qe1h_d86gIk6fFCmiHRA0fdHQ-jjS3DndmVFBo36_H4-HKISZ9Pohe1NcOPgr-CvO6cqJWj2zp6qqpENxwA2rbFpfzx2L4ESo-kVOk8Adpgr2sAM5WKW8etCVrhBMcbZVE75pSZHifxXIyMDITaxhdgDzWtRMlmlHvo5KVAZcrH_CoCeTTGHZ0Bo7NHzk9GI=");
      headers.put("Host", "services.rappi.com.br");
      headers.put("app-version-name", "7.48.20230420-72418");
      headers.put(HttpHeaders.ACCEPT, "*/*");

      String payload = "{\"context\":\"product_detail\",\"stores\":[900595776],\"offset\":0,\"limit\":5,\"state\":{\"store_type\":\"atacadao\",\"parent_store_type\":\"market\",\"product_id: " + session.getOriginalURL() + ",\"sessions\":\"0\",\"is_prime\":\"false\",\"zone_ids\":\"[]\",\"unlimited_shipping\":\"false\",\"lat\":\"-27.57929014867\",\"lng\":\"-48.588943853974\"},\"store_type\":\"atacadao\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://services.rappi.com.br/api/dynamic/context/content")
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.BUY_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);

      return JSONUtils.stringToJson(response.getBody());
   }


   private List<String> getSecondaryImages(JSONObject product){
      List<String> secondaryImages = new ArrayList<>();
      JSONArray images = JSONUtils.getJSONArrayValue(product, "images");
      for (Integer i=0; i<JSONUtils.getJSONArrayValue(product, "images").length(); i++){
         secondaryImages.add(baseImageUrl+images.getString(i));
      }
      return secondaryImages;
   }
   public List<Product> extractInformation(Document doc) throws MalformedProductException, OfferException, MalformedPricingException {
      JSONObject json = fetchDocument();
      List<Product> products = new ArrayList<>();
      if (json != null && !json.isEmpty()) {
         JSONObject jsonProduct = JSONUtils.getValueRecursive(json, "data.components.resource.product", JSONObject.class, new JSONObject());
         String internalId = Integer.toString(JSONUtils.getIntegerValueFromJSON(jsonProduct, "master_product_id", 0));
         String name = JSONUtils.getStringValue(jsonProduct, "name");
         String category = JSONUtils.getStringValue(jsonProduct, "category_name");
         String primaryImage = baseImageUrl+JSONUtils.getStringValue(jsonProduct, "image");
         List<String> secondaryImage = getSecondaryImages(jsonProduct);
         String description = JSONUtils.getStringValue(jsonProduct, "description");
         boolean availableToBuy = JSONUtils.getStringValue(jsonProduct, "in_stock") == "true";
         Offers offers = availableToBuy ? scrapOffers(jsonProduct, null) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(category)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".column.main") != null;
   }

   private Offers scrapOffers(JSONObject jsonProduct, Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonProduct);
     // List<String> sales = scrapSales(jsonProduct);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         //.setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject jsonProduct) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(jsonProduct, "price", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(jsonProduct, "real_price", true);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   /*private List<String> scrapSales(JSONObject jsonProduct) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".special-price .price");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }*/


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



   private Double getTotalAvgRating(Document doc) {
      Integer avgRatingInteger = CrawlerUtils.scrapIntegerFromHtml(doc,
         ".amstars-rating-container .amstars-stars .hidden", true, 0);
      Double avgRating = (avgRatingInteger/100)*5.0;
      return avgRating;
   }


}

