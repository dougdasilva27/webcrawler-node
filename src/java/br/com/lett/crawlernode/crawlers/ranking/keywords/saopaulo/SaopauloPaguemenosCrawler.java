package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;

public class SaopauloPaguemenosCrawler extends CrawlerRankingKeywords {

    public SaopauloPaguemenosCrawler(Session session) {
        super(session);
    }

    private static final String HOME_PAGE = "https://www.paguemenos.com.br/";
    private static final int API_VERSION = 1;
    private static final String SENDER = "vtex.search@0.x";
    private static final String PROVIDER = "vtex.search@0.x";

    private String keySHA256 = "dcf550c27cd0bbf0e6899e3fa1f4b8c0b977330e321b9b8304cc23e2d2bad674";

    @Override
    protected void extractProductsFromCurrentPage() {
        this.log("Página " + this.currentPage);
        this.pageSize = 21;

        if (this.currentPage == 1) {
            this.keySHA256 = fetchSHA256Key();
        }

        JSONObject searchApi = fetchSearchApi();
        JSONArray products = searchApi.has("products") ? searchApi.getJSONArray("products") : new JSONArray();

        if (products.length() > 0) {

            if (this.totalProducts == 0) {
                setTotalProducts(searchApi);
            }

            for (Object object : products) {
                JSONObject product = (JSONObject) object;
                String productUrl = HOME_PAGE + (product.has("linkText") && !product.isNull("linkText") ? product.get("linkText").toString() : null) + "/p";
                String internalPid = product.optString("productId");

                saveDataProduct(null, internalPid, productUrl);

                this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);

                if (this.arrayProducts.size() == productsLimit) {
                    break;
                }
            }

        } else {
            this.result = false;
            this.log("Keyword sem resultado!");
        }

        this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    }

    private void setTotalProducts(JSONObject data) {
        this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(data, "total", 0);
        this.log("Total da busca: " + this.totalProducts);
    }

    private JSONObject fetchSearchApi() {
        JSONObject searchApi = new JSONObject();
        StringBuilder url = new StringBuilder();
        url.append("https://www.paguemenos.com.br/_v/segment/graphql/v1?");

        JSONObject extensions = new JSONObject();
        JSONObject persistedQuery = new JSONObject();

        persistedQuery.put("version", API_VERSION);
        persistedQuery.put("sha256Hash", this.keySHA256);
        persistedQuery.put("sender", SENDER);
        persistedQuery.put("provider", PROVIDER);

        extensions.put("variables", createVariablesBase64());
        extensions.put("persistedQuery", persistedQuery);

        StringBuilder payload = new StringBuilder();
        try {
            payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"));
            payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
        url.append(payload.toString());

        log("Link onde são feitos os crawlers:" + url);

        Request request = Request.RequestBuilder.create()
                .setUrl(url.toString())
                .setCookies(cookies)
                .setPayload(payload.toString())
                .build();

        JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

        if (response.has("data") && !response.isNull("data")) {
            JSONObject data = response.getJSONObject("data");

            if (data.has("searchResult") && !data.isNull("searchResult")) {
                searchApi = data.getJSONObject("searchResult");
            }
        }

        return searchApi;
    }

    private String createVariablesBase64() {
        JSONObject search = new JSONObject();
        search.put("productOrigin", "VTEX");
        search.put("indexingType", "API");
        search.put("query", keywordEncoded);
        search.put("page", currentPage);
        search.put("attributePath", "");
        search.put("sort", "");
        search.put("count", 12);
        search.put("leap", false);

        if (currentPage != 1) {
            int from = pageSize * (currentPage - 1);
            search.put("from", from);
            search.put("to", from + 11);
        }

        return Base64.getEncoder().encodeToString(search.toString().getBytes());
    }

    private String fetchSHA256Key() {
        // When sha256Hash is not found, this key below works (on 03/02/2020)
        String hash = "dcf550c27cd0bbf0e6899e3fa1f4b8c0b977330e321b9b8304cc23e2d2bad674";
        // When script with hash is not found, we use this url
        String url = "http://exitocol.vtexassets.com/_v/public/assets/v1/published/bundle/public/react/asset.min.js?v=1&files=vtex.search@0.6.4,0";

        String homePage = "https://www.exito.com/";

        Request requestHome = Request.RequestBuilder.create().setUrl(homePage).setCookies(cookies).mustSendContentEncoding(false).build();
        Document doc = Jsoup.parse(this.dataFetcher.get(session, requestHome).getBody());

        Elements scripts = doc.select("body > script[crossorigin]");
        for (Element e : scripts) {
            String scriptUrl = CrawlerUtils.scrapUrl(e, null, "src", "https", "exitocol.vtexassets.com");
            if (scriptUrl.contains("vtex.search@")) {
                url = scriptUrl;
                break;
            }
        }

        Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build();
        String response = this.dataFetcher.get(session, request).getBody().replace(" ", "");

        String searchProducts = CrawlerUtils.extractSpecificStringFromScript(response, "searchResult(", false, "',", false);
        String firstIndexString = "@runtimeMeta(hash:";
        if (searchProducts.contains(firstIndexString) && searchProducts.contains(")")) {
            int x = searchProducts.indexOf(firstIndexString) + firstIndexString.length();
            int y = searchProducts.indexOf(')', x);

            hash = searchProducts.substring(x, y).replace("\"", "");

        }

        return hash;
    }
}
