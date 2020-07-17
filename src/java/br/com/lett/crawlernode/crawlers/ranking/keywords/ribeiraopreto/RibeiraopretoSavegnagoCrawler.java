package br.com.lett.crawlernode.crawlers.ranking.keywords.ribeiraopreto;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RibeiraopretoSavegnagoCrawler extends CrawlerRankingKeywords {

    public RibeiraopretoSavegnagoCrawler(Session session) {
        super(session);
    }

    /**
     * Código das cidades: Ribeirão Preto - 2 Sertãozinho - 6 Jardianópolis - 11 Jaboticabal - 7
     * Franca - 3 Barretos - 10 Bebedouro - 9 Monte Alto - 12 Araraquara - 4 São carlos - 5 Matão - 8
     */
    // private static final int cityCode = ControllerKeywords.codeCity;
    private static final int cityCode = 2;
    private static final String API_KEY = "savegnago";
    private static final int SALES_CHANNEL = 1;

    @Override
    protected void extractProductsFromCurrentPage() {
        this.pageSize = 32;
        this.log("Página " + this.currentPage);

        JSONObject json = loadJson();
        JSONArray products = json.optJSONArray("products");

        if (products != null && !products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts(json);
            }
            for (Object productObj : products) {
                JSONObject product = (JSONObject) productObj;
                String internalId = product.optString("id");
                String internalPid = internalId;
                String productUrl = product.optString("url").replace("//", "");
                saveDataProduct(internalId, internalPid, productUrl);

                log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
                if (arrayProducts.size() == productsLimit) {
                    break;
                }
            }
        } else {
            result = false;
            log("Keyword sem resultado!");
        }
        log("Finalizando Crawler de produtos da página $currentPage até agora ${arrayProducts.size} produtos crawleados");
    }

    private void setTotalProducts(JSONObject json) {
        this.totalProducts = json.optInt("size", 0);
        this.log("Total de produtos: " + this.totalProducts);
    }

    private JSONObject loadJson() {
        String url = "https://api.linximpulse.com/engage/search/v3/search/?salesChannel=" + SALES_CHANNEL + "&terms=" + this.keywordEncoded + "&resultsPerPage=32&page=" + this.currentPage + "&apiKey=" + API_KEY;

        this.log("Link onde são feitos os crawlers: " + url);

        Map<String, String> headers = new HashMap<>();
        headers.put("origin", "https://www.savegnago.com.br");
        headers.put("content-type:", "application/json");

        Request request = Request.RequestBuilder.create().setUrl(url)
                .setCookies(cookies)
                .setHeaders(headers)
                .mustSendContentEncoding(false)
                .build();
        Response response = this.dataFetcher.get(session, request);
        return CrawlerUtils.stringToJson(response.getBody());
    }

}
