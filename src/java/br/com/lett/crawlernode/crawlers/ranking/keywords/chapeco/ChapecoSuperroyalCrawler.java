package br.com.lett.crawlernode.crawlers.ranking.keywords.chapeco;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChapecoSuperroyalCrawler extends CrawlerRankingKeywords {
    public ChapecoSuperroyalCrawler(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.pageSize = 20;
        log("Página " + this.currentPage);

        String url = "https://superroyal.com.br/busca/" + this.keywordEncoded;
        log("Link onde são feitos os crawlers: " + url);

        JSONObject productsJSON = fetchJson();
        JSONArray items = productsJSON.optJSONArray("hits");
        if (items != null & !items.isEmpty()) {

            if (this.totalProducts == 0) {
                setTotalProducts(productsJSON);
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject productJson = items.optJSONObject(i);

                String internalPid = productJson.optString("objectID");
                String internalId = internalPid;
                String productUrl = "https://www.superroyal.com.br/produtos/" + internalPid + "/" + productJson.optString("slug");

                saveDataProduct(internalId, internalPid, productUrl);

                this.log("Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl);

                if (arrayProducts.size() == productsLimit) {
                    break;
                }
            }
        } else {
            result = false;
            log("Keyword sem resultado!");
        }
        log("Finalizando Crawler de produtos da página " + this.currentPage + " até agora " + this.arrayProducts.size() + " produtos crawleados");

    }

    protected void setTotalProducts(JSONObject json) {
        this.totalProducts = json.optInt("nbHits", 0);
        this.log("Total de produtos: " + this.totalProducts);
    }

    private JSONObject fetchJson() {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Host", "su0fwwvvoi-3.algolianet.com");

        JSONObject payload = new JSONObject();
        payload.put("query", this.keywordEncoded);
        payload.put("filters", "_tags:account-8 AND _tags:show-on-store-18");
        payload.put("page", this.currentPage - 1);

        String urlApi = "https://su0fwwvvoi-3.algolianet.com/1/indexes/ecommerce_products_production/query?x-algolia-api-key=196f2ac6b0ce299ac2a625682c134007&x-algolia-application-id=SU0FWWVVOI";

        Request request = Request.RequestBuilder.create()
                .setUrl(urlApi)
                .setHeaders(headers)
                .mustSendContentEncoding(false)
                .setPayload(payload.toString())
                .setCookies(cookies)
                .build();

        String page = new FetcherDataFetcher().post(session, request).getBody();
        return CrawlerUtils.stringToJson(page);
    }
}
