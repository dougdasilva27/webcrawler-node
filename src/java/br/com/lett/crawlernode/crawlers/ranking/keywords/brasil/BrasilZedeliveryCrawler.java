package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.net.HttpHeaders;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class BrasilZedeliveryCrawler extends CrawlerRankingKeywords {

    private static final String API_URL = "https://api.ze.delivery/public-api";
    private static final String VISITOR_ID = "2d5a638d-fc7b-4143-9379-86ddb12832b5";

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

        JSONObject payload = new JSONObject();
        payload.put("operationName", "search");
        payload.put("variables", getVariables());
        payload.put("query", "query search($searchTerm: String!, $limit: Int) {\\n  search(searchTerm: $searchTerm) {\\n    items(limit: $limit) {\\n      id\\n      type\\n      displayName\\n      images\\n      applicableDiscount {\\n        presentedDiscountValue\\n        discountType\\n        finalValue\\n      }\\n      category {\\n        id\\n        displayName\\n      }\\n      brand {\\n        id\\n        displayName\\n      }\\n      price {\\n        min\\n        max\\n      }\\n    }\\n  }\\n}\\n");

        Request request = Request.RequestBuilder.create().setUrl(API_URL)
                .setPayload(payload.toString())
                .setCookies(cookies)
                .setHeaders(headers)
                .mustSendContentEncoding(true)
                .build();
        Response response = this.dataFetcher.get(session, request);
        return CrawlerUtils.stringToJson(response.getBody());
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.pageSize = 20;
        this.log("Página " + this.currentPage);

        String url = "https://api.ze.delivery/public-api";
        this.log("Link onde são feitos os crawlers: " + url);
        //this.currentDoc = fetchDocument(url);
        JSONObject doc = fetch();
        /*Elements products = this.currentDoc.select(".list-standart-products > li");

        if (!products.isEmpty()) {
            if (this.totalProducts == 0) {
                setTotalProducts();
            }

            for (Element product : products) {
                String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "input[name=\"ProdutoId\"]", "value");
                String internalPId = internalId;
                String productUrl = CrawlerUtils.scrapUrl(product, ".product > a", "href", "https", HOST_PAGE);
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
        } else {
            this.result = false;
            this.log("Keyword sem resultado!");
        }

        this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");*/
    }

    /*@Override
    protected void setTotalProducts() {
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".results > span:last-child", true, 0);
        this.log("Total: " + this.totalProducts);
    }*/
}
