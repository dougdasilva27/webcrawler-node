package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MexicoDespensabodegaaurreraCrawler extends CrawlerRankingKeywords {

    private final static String API_URL = "https://deadpool.instaleap.io/api/v2";
    private final static String STORE_ID = "565";

    public MexicoDespensabodegaaurreraCrawler(Session session) {
        super(session);
    }

    protected JSONObject fetchJSON() {
        Map<String, String> headers = new HashMap<>();

        headers.put("authority", "deadpool.instaleap.io");
        headers.put("content-type", "application/json");

        String body = "{\"variables\":{\"pagination\":{\"pageSize\":100,\"currentPage\":" + this.currentPage + "},\"search\":{\"text\":\"" + this.keywordEncoded + "\",\"language\":\"ES\"},\"storeId\":\"565\"},\"query\":\"query ($pagination: paginationInput, $search: SearchInput, $storeId: ID!, $categoryId: ID, $onlyThisCategory: Boolean, $filter: ProductsFilterInput, $orderBy: productsSortInput) {  getProducts(pagination: $pagination, search: $search, storeId: $storeId, categoryId: $categoryId, onlyThisCategory: $onlyThisCategory, filter: $filter, orderBy: $orderBy) {    redirectTo    products {      id      description      name      photosUrls      sku      unit      price      specialPrice      promotion {        description        type        isActive        conditions        __typename      }      stock      nutritionalDetails      clickMultiplier      subQty      subUnit      maxQty      minQty      specialMaxQty      ean      boost      showSubUnit      isActive      slug      categories {        id        name        __typename      }      __typename    }    paginator {      pages      page      __typename    }    __typename  }}\"}";

        Request request = Request.RequestBuilder.create()
                .setUrl(API_URL)
                .setPayload(body)
                .setHeaders(headers)
                .build();

        Response response = new JsoupDataFetcher().post(session, request);
        return JSONUtils.stringToJson(response.getBody());
    }

    @Override
    protected void extractProductsFromCurrentPage() throws MalformedProductException {
        JSONObject json = fetchJSON();

        JSONArray results = JSONUtils.getValueRecursive(json, "data.getProducts.products", JSONArray.class);

        if (results != null && !results.isEmpty()) {
            for (Object prod : results) {
                JSONObject product = (JSONObject) prod;

                String internalPid = product.optString("sku");
                String internalId = product.optString("id");
                String productUrl = "https://despensa.bodegaaurrera.com.mx/p/" + product.optString("slug");
                String name = product.optString("name");
                int price = product.optInt("price", 0) == 0 ? product.optInt("specialPrice", 0) : product.optInt("price", 0);
                boolean isAvailable = product.getInt("stock") != 0;

                //New way to send products to save data product
                RankingProduct productRanking = RankingProductBuilder.create()
                        .setUrl(productUrl)
                        .setInternalId(internalId)
                        .setInternalPid(internalPid)
                        .setName(name)
                        .setPriceInCents(price)
                        .setAvailability(isAvailable)
                        .build();

                saveDataProduct(productRanking);
                this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
                if (this.arrayProducts.size() == productsLimit)
                    break;
            }
        } else {
            this.result = false;
            this.log("Keyword sem resultado!");
        }

        this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    }

    @Override
    protected boolean hasNextPage(){
        return true;
    }

}
