package br.com.lett.crawlernode.crawlers.ranking.keywords.australia;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;

public class AustraliaCatchCrawler extends CrawlerRankingKeywords {
    String BASE_URL = "https://www.catch.com.au";

    public AustraliaCatchCrawler(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.log("Página " + this.currentPage);

        JSONObject search = crawlSearchApi(getUrlForCurrentPage());
        JSONObject metadata = search.optJSONObject("metadata");

        this.pageSize = metadata.optInt("limit");
        this.totalProducts = metadata.optInt("hits");
        String internalId;
        String url;

        for (Object o : search.optJSONArray("results")) {
            JSONObject productJsonInformation = (JSONObject) o;

            if (!productJsonInformation.optString("type").equals("product")) {
                continue;
            }
            JSONObject productJson = productJsonInformation.optJSONObject("product");

            internalId = productJson.optString("id");
            url = BASE_URL + productJson.optString("url");

            saveDataProduct(internalId, null, url);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + url);
        }
        this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    }

    private String getUrlForCurrentPage() {
        return BASE_URL + "/search.json?query=" + this.keywordEncoded + "&search_src=topbar&page=" + this.currentPage + "&sseq=1";
    }

    private JSONObject crawlSearchApi(String url) {
        JSONObject json = new JSONObject();
        Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
        String response = dataFetcher.get(session, request).getBody();
        JSONObject jsonObject = JSONUtils.stringToJson(response);
        if (jsonObject != null)
            json = jsonObject.optJSONObject("payload");
        return json;
    }
}
