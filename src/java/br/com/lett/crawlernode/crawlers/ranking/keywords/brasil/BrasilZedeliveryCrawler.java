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

public class BrasilZedeliveryCrawler extends CrawlerRankingKeywords {

    private static final String API_URL = "https://api.ze.delivery/public-api";
    private static final String VISITOR_ID = "4004b948-7568-4474-91c1-3e9b463f135e";

    public BrasilZedeliveryCrawler(Session session) {
        super(session);
    }

    private JSONObject getVariables() {
        JSONObject variables = new JSONObject();
        variables.put("searchTerm", this.keywordEncoded);
        variables.put("limit", 20);
        return variables;
    }

    protected JSONObject fetch() {

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
        System.err.println(CrawlerUtils.stringToJson(response.getBody()));
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
