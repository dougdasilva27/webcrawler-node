package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ZedeliveryCrawlerRanking extends CrawlerRankingKeywords {

   private static final String API_URL = "https://api.ze.delivery/public-api";

   private String visitorId;

   public ZedeliveryCrawlerRanking(Session session) {
      super(session);
   }

   private void validateUUID() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      int attemp = 0;

      String initPayload = "{\"" +
         "operationName\":\"setDeliveryOption\",\"" +
         "variables\":{\"" +
         "deliveryOption\":{\"" +
         "address\":{\"" +
         "latitude\":" + session.getOptions().optString("latitude") + ",\"" +
         "longitude\":" + session.getOptions().optString("longitude") + ",\"" +
         "zipcode\":\"" + session.getOptions().optString("zipCode") + "\",\"" +
         "street\":\"" + session.getOptions().optString("street") + "\",\"" +
         "neighborhood\":\"" + session.getOptions().optString("neighborhood") + "\",\"" +
         "city\":\"" + session.getOptions().optString("city") + "\",\"" +
         "province\":\"" + session.getOptions().optString("province") + "\",\"" +
         "country\":\"BR\",\"" +
         "number\":\"" + session.getOptions().optString("number") + "\"" +
         "},\"" +
         "deliveryMethod\":\"DELIVERY\"," +
         "\"schedule\":\"NOW\"}," +
         "\"forceOverrideProducts\":false}," +
         "\"query\":\"mutation setDeliveryOption($deliveryOption: DeliveryOptionInput, $forceOverrideProducts: Boolean) {\\n  manageCheckout(deliveryOption: $deliveryOption, forceOverrideProducts: $forceOverrideProducts) {\\n    messages {\\n      category\\n      target\\n      key\\n      args\\n      message\\n    }\\n    checkout {\\n      id\\n      deliveryOption {\\n        address {\\n          latitude\\n          longitude\\n          zipcode\\n          country\\n          province\\n          city\\n          neighborhood\\n          street\\n          number\\n          addressLine2\\n          referencePoint\\n        }\\n        deliveryMethod\\n        schedule\\n        scheduleDateTime\\n        pickupPoc {\\n          id\\n          tradingName\\n          address {\\n            latitude\\n            longitude\\n            zipcode\\n            country\\n            province\\n            city\\n            neighborhood\\n            street\\n            number\\n            addressLine2\\n            referencePoint\\n          }\\n        }\\n      }\\n      paymentMethod {\\n        id\\n        displayName\\n      }\\n    }\\n  }\\n}\\n\"}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
         .setPayload(initPayload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .mustSendContentEncoding(false)
         .build();

      do {
         Response response = new JsoupDataFetcher().post(session, request);
         visitorId = response.getHeaders().get("x-visitorid");
         attemp++;
      } while (visitorId == null || visitorId.isEmpty() && attemp < 3);


      if (visitorId.isEmpty()) {
         Logging.printLogError(logger, "FAILED TO GET VISITOR ID");
      }

   }

   protected JSONObject fetch() {
      validateUUID();

      Map<String, String> headers = new HashMap<>();
      headers.put("x-visitorid", visitorId);
      headers.put("Content-Type", "application/json");
      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Connection", "keep-alive");

      String payload =
         "{\"operationName\":\"newSearch\",\"variables\":{\"searchTerm\":\"" + this.keywordWithoutAccents + "\",\"limit\":20},\"query\":\"query newSearch($searchTerm: String!, $limit: Int) {\\n  newSearch(searchTerm: $searchTerm) {\\n    items(limit: $limit) {\\n      id\\n      type\\n      displayName\\n      images\\n      applicableDiscount {\\n        presentedDiscountValue\\n        discountType\\n        finalValue\\n      }\\n      category {\\n        id\\n        displayName\\n      }\\n      brand {\\n        id\\n        displayName\\n      }\\n      price {\\n        min\\n        max\\n      }\\n    }\\n  }\\n}\\n\"}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
         .setPayload(payload)
         .setCookies(cookies)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      this.log("Link onde são feitos os crawlers: " + API_URL);

      JSONObject json = fetch();
      JSONObject data = json.optJSONObject("data");
      if (data != null) {
         JSONObject search = data.optJSONObject("newSearch");
         JSONArray items = search.optJSONArray("items");
         if (!items.isEmpty()) {
            for (Object item : items) {
               JSONObject product = (JSONObject) item;

               if (product.optString("type").equals("PRODUCT")) {
                  String internalId = product.optString("id");
                  String internalPId = internalId;
                  String productUrl = scrapUrl(product, internalId);
                  String name = product.optString("displayName");
                  JSONArray images = product.getJSONArray("images");
                  String image = images.length() > 0 ? images.getString(0) : "";
                  int priceInCents = scrapPrice(product);
                  boolean isAvailable = priceInCents != 0;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setInternalId(internalId)
                     .setInternalPid(internalPId)
                     .setName(name)
                     .setUrl(productUrl)
                     .setImageUrl(image)
                     .setAvailability(isAvailable)
                     .setPriceInCents(priceInCents)
                     .build();

                  saveDataProduct(productRanking);

                  if (this.arrayProducts.size() == productsLimit) {
                     break;
                  }
               }
            }
         } else {
            this.result = false;
            this.log("Keyword sem resultado!");
         }
         this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
      }
   }

   private int scrapPrice(JSONObject product) {
      JSONObject applicableDiscount = product.optJSONObject("applicableDiscount");
      int priceInCents = 0;

      if (applicableDiscount != null) {
         Double price = JSONUtils.getDoubleValueFromJSON(applicableDiscount, "finalValue", true);
         priceInCents = (int) Math.round(100 * price);
      } else {
         JSONObject price = product.optJSONObject("price");
         if (price != null) {
            Double min = JSONUtils.getDoubleValueFromJSON(price, "min", true);
            priceInCents = (int) Math.round(100 * min);
         }
      }
      return priceInCents;
   }

   private String scrapUrl(JSONObject product, String id) {
      String displayName = product.optString("displayName").toLowerCase().replaceAll(" ", "-");
      return "https://www.ze.delivery/entrega-produto/" + id + "/" + displayName;
   }
}
