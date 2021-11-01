package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ZedeliveryCrawlerRanking extends CrawlerRankingKeywords {

   private static final String API_URL = "https://api.ze.delivery/public-api";

   private String visitorId;

   public ZedeliveryCrawlerRanking(Session session) {
      super(session);
   }

   private JSONObject validateUUID() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      String initPayload = "{\n" +
         "  \"operationName\": \"setDeliveryOption\",\n" +
         "  \"variables\": {\n" +
         "    \"deliveryOption\": {\n" +
         "      \"address\": {\n" +
         "        \"latitude\": " + session.getOptions().optString("latitude")  + ",\n" +
         "        \"longitude\": " + session.getOptions().optString("longitude")  + ",\n" +
         "        \"zipcode\": \"" + session.getOptions().optString("zipCode") + "\",\n" +
         "        \"street\": \"" + session.getOptions().optString("street") + "\",\n" +
         "        \"neighborhood\": \"" + session.getOptions().optString("neighborhood") + "\",\n" +
         "        \"city\": \"" + session.getOptions().optString("city") + "\",\n" +
         "        \"province\": \"" + session.getOptions().optString("province") + "\",\n" +
         "        \"country\": \"BR\",\n" +
         "        \"number\": \"" + session.getOptions().optString("number") + "\",\n" +
         "        \"referencePoint\": \"\"\n" +
         "      },\n" +
         "      \"deliveryMethod\": \"DELIVERY\",\n" +
         "      \"schedule\": \"NOW\"\n" +
         "    },\n" +
         "    \"forceOverrideProducts\": false\n" +
         "  },\n" +
         "  \"query\": \"mutation setDeliveryOption($deliveryOption: DeliveryOptionInput, $forceOverrideProducts: Boolean) {\\n  manageCheckout(deliveryOption: $deliveryOption, forceOverrideProducts: $forceOverrideProducts) {\\n    messages {\\n      category\\n      target\\n      key\\n      args\\n      message\\n    }\\n    checkout {\\n      id\\n      deliveryOption {\\n        address {\\n          latitude\\n          longitude\\n          zipcode\\n          country\\n          province\\n          city\\n          neighborhood\\n          street\\n          number\\n          addressLine2\\n          referencePoint\\n        }\\n        deliveryMethod\\n        schedule\\n        scheduleDateTime\\n        pickupPoc {\\n          id\\n          tradingName\\n          address {\\n            latitude\\n            longitude\\n            zipcode\\n            country\\n            province\\n            city\\n            neighborhood\\n            street\\n            number\\n            addressLine2\\n            referencePoint\\n          }\\n        }\\n      }\\n      paymentMethod {\\n        id\\n        displayName\\n      }\\n    }\\n  }\\n}\\n\"\n" +
         "}";

      Integer contentLength = initPayload.length();
      headers.put("Content-Length", contentLength.toString());
      
      Request request = Request.RequestBuilder.create().setUrl(API_URL)
         .setPayload(initPayload)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      visitorId = response.getHeaders().get("x-visitorid");
      return CrawlerUtils.stringToJson(response.getBody());
   }

   protected JSONObject fetch() {
      validateUUID();

      Map<String, String> headers = new HashMap<>();
      headers.put("x-visitorid", visitorId);
      headers.put("Content-Type", "application/json");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");

      String payload =
         "{\"variables\":{\"searchTerm\":\"" + this.keywordEncoded
            + "\",\"limit\":"+20+"},\"query\":\"query search($searchTerm: String!, $limit: Int) {  search(searchTerm: $searchTerm) {    items(limit: $limit) "
            + "{      id      type      displayName      images      applicableDiscount {        presentedDiscountValue        discountType        finalValue      }"
            + "      category {        id        displayName      }      brand {        id        displayName      }      price {        min        max      }    }"
            + "  }}\",\"operationName\":\"search\"}";

      Request request = Request.RequestBuilder.create().setUrl(API_URL)
         .setPayload(payload)
         .setCookies(cookies)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response response = new JsoupDataFetcher().post(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;
      this.log("Página " + this.currentPage);

      this.log("Link onde são feitos os crawlers: " + API_URL);

      JSONObject json = fetch();
      JSONObject data = json.optJSONObject("data");
      if (data != null) {
         JSONObject search = data.optJSONObject("search");
         JSONArray items = search.optJSONArray("items");
         if(!items.isEmpty()){
            for (Object item : items) {
               JSONObject product = (JSONObject) item;

               if (product.optString("type").equals("PRODUCT")) {
                  String internalId = product.optString("id");
                  String internalPId = internalId;
                  String productUrl = scrapUrl(product, internalId);
                  saveDataProduct(internalId, internalPId, productUrl);

                  this.log(
                     "Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - InternalPid: " + internalPId +
                        " - Url: " + productUrl
                  );

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

   private String scrapUrl(JSONObject product, String id) {
      String displayName = product.optString("displayName").toLowerCase().replaceAll(" ", "-");
      return "https://www.ze.delivery/entrega-produto/" + id + "/" + displayName;
   }
}
