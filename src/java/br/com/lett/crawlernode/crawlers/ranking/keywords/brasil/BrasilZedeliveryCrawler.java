package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BrasilZedeliveryCrawler extends CrawlerRankingKeywords {

    private static final String API_URL = "https://api.ze.delivery/public-api";
    private String VISITOR_ID;

    public BrasilZedeliveryCrawler(Session session) {
        super(session);
    }

    /**
     * Making a call to an address in the 05426-100 postal code
     * Address is hard coded in payload
     */
    private void validateUUID() {
        VISITOR_ID = UUID.randomUUID().toString();

        Map<String, String> headers = new HashMap<>();
        headers.put("x-visitorid", VISITOR_ID);
        headers.put("content-type:", "application/json");

        String payload =
                "{\"operationName\":\"setDeliveryOption\",\"variables\":{\"deliveryOption\":{\"address\":{\"latitude\":-23.5674273,\"longitude\":-46.6931558,\"zipcode\":null,\"street\":\"Avenida Brigadeiro Faria Lima\",\"neighborhood\":\"Pinheiros\",\"city\":\"S\\u00e3o Paulo\",\"province\":\"SP\",\"country\":\"BR\",\"number\":\"1\"},\"deliveryMethod\":\"DELIVERY\",\"schedule\":\"NOW\"},\"forceOverrideProducts\":false},\"query\":\"mutation setDeliveryOption($deliveryOption: DeliveryOptionInput, $forceOverrideProducts: Boolean) {\\n  manageCheckout(deliveryOption: $deliveryOption, forceOverrideProducts: $forceOverrideProducts) {\\n    messages {\\n      category\\n      target\\n      key\\n      args\\n      message\\n    }\\n    checkout {\\n      id\\n      deliveryOption {\\n        address {\\n          latitude\\n          longitude\\n          zipcode\\n          country\\n          province\\n          city\\n          neighborhood\\n          street\\n          number\\n          addressLine2\\n        }\\n        deliveryMethod\\n        schedule\\n        scheduleDateTime\\n        pickupPoc {\\n          id\\n          tradingName\\n          address {\\n            latitude\\n            longitude\\n            zipcode\\n            country\\n            province\\n            city\\n            neighborhood\\n            street\\n            number\\n            addressLine2\\n          }\\n        }\\n      }\\n      paymentMethod {\\n        id\\n        displayName\\n      }\\n    }\\n  }\\n}\\n\"}";

        Request request = Request.RequestBuilder.create().setUrl(API_URL)
                .setPayload(payload)
                .setCookies(cookies)
                .setHeaders(headers)
                .mustSendContentEncoding(false)
                .build();
        this.dataFetcher.post(session, request);
    }


    protected JSONObject fetch() {
        validateUUID();

        Map<String, String> headers = new HashMap<>();
        headers.put("x-visitorid", VISITOR_ID);
        headers.put("content-type:", "application/json");

        String payload =
                "{\"variables\":{\"searchTerm\":\"" + this.keywordEncoded + "\",\"limit\":\"20\"},\"query\":\"query search($searchTerm: String!, $limit: Int) {  search(searchTerm: $searchTerm) {    items(limit: $limit) {      id      type      displayName      images      applicableDiscount {        presentedDiscountValue        discountType        finalValue      }      category {        id        displayName      }      brand {        id        displayName      }      price {        min        max      }    }  }}\",\"operationName\":\"search\"}";

        Request request = Request.RequestBuilder.create().setUrl(API_URL)
                .setPayload(payload)
                .setCookies(cookies)
                .setHeaders(headers)
                .mustSendContentEncoding(false)
                .build();
        Response response = this.dataFetcher.post(session, request);
        return CrawlerUtils.stringToJson(response.getBody());
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.pageSize = 20;
        this.log("Página " + this.currentPage);

        String url = "https://api.ze.delivery/public-api";
        this.log("Link onde são feitos os crawlers: " + url);

        JSONObject json = fetch();
        JSONObject data = json.optJSONObject("data");

        if (data != null) {
            JSONObject search = data.optJSONObject("search");
            JSONArray items = search.optJSONArray("items");

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
                } else {
                    this.result = false;
                    this.log("Keyword sem resultado!");
                }
            }
            this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
        }
    }

    private String scrapUrl(JSONObject product, String id) {
        String displayName = product.optString("displayName").toLowerCase().replaceAll(" ", "-");
        return "https://www.ze.delivery/entrega-produto/" + id + "/" + displayName;
    }
}
