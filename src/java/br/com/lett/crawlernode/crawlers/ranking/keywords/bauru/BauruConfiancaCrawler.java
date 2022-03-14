package br.com.lett.crawlernode.crawlers.ranking.keywords.bauru;

import java.util.*;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BauruConfiancaCrawler extends CrawlerRankingKeywords {

  public BauruConfiancaCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();
  final private String cityName = "bauru";

  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 18;

    this.log("Página " + this.currentPage);

    JSONObject catalogJson = crawlCatalogApi();
    List<String> productIds = crawlProductsIdsFromCatalog(catalogJson);

    if (this.totalProducts == 0) {
       setTotalProducts(catalogJson);
    }

    JSONArray productsJson = crawlProductsApi(productIds);
    List<Boolean> productAvailability = crawlStockStatusApi(productIds);

    if (productsJson != null && productsJson.length() > 0) {

      for (int i = 0; i < productsJson.length(); i++) {
        JSONObject product = productsJson.optJSONObject(i);

        String productUrl = crawlProductUrl(product);
        String internalId = product.optString("id");
        String internalPid = JSONUtils.getValueRecursive(product, "childSKUs/0/barcode", "/", String.class, null);
        String imagePath = product.optString("primaryFullImageURL");
        String image = imagePath != null ? "https://www.confianca.com.br" + imagePath : null;
        String name = product.optString("displayName");
        Double priceDouble = JSONUtils.getValueRecursive(product, "childSKUs/0/salePrice", "/", Double.class, null);
        if (priceDouble == null) {
           priceDouble = JSONUtils.getValueRecursive(product, "childSKUs/0/listPrice", "/", Double.class, null);
        }
        Integer priceInCents = CommonMethods.doublePriceToIntegerPrice(priceDouble, 0);
        boolean isAvailable = productAvailability.get(i);

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setImageUrl(image)
            .setName(name)
            .setPriceInCents(priceInCents)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);

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

  private String crawlProductUrl(JSONObject product) {
    String urlProduct = null;

    String urlPath = product.optString("route");
    if (urlPath != null) {
      urlProduct = "https://www.confianca.com.br/" + this.cityName + urlPath;
    }
    return urlProduct;
  }

  private JSONObject crawlCatalogApi() {
     String urlCatalog = "https://www.confianca.com.br/ccstore/v1/assembler/pages/Default/osf/catalog?No=" + (this.currentPage - 1) * this.pageSize + "&Nrpp=" + this.pageSize + "&Ntt=" + this.keywordEncoded;

     Map<String, String> headers = new HashMap<>();
     headers.put("Content-Type", "application/json");
     headers.put("X-CCProfileType", "storefrontUI");
     headers.put("X-CC-MeteringMode", "CC-NonMetered");
     headers.put("X-CCSite", "Confianca");
     headers.put("X-CCPriceListGroup", "DefaultPriceListFlex");
     headers.put("X-CCAsset-Language", "pt-BR");
     headers.put("X-CC-Frontend-Forwarded-Url", "www.confianca.com.br/" + this.cityName + "/search?Ntt=" + this.keywordEncoded);

     Request request = RequestBuilder.create().setUrl(urlCatalog).setCookies(cookies).setHeaders(headers).build();

     return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
  }

  private List<String> crawlProductsIdsFromCatalog(JSONObject catalog) {
     List<String> productsIds = new ArrayList<>();

     JSONArray records = JSONUtils.getValueRecursive(catalog, "results/records", "/", JSONArray.class, new JSONArray());
     for (int i = 0; i < records.length(); i++) {
        String skuId = JSONUtils.getValueRecursive(records.optJSONObject(i), "attributes/sku.listingId/0", "/", String.class, null);
        productsIds.add(skuId);
     }

     return productsIds;
  }

  private JSONArray crawlProductsApi(List<String> productsIds) {
     StringBuilder productsIdsFormatted = new StringBuilder();
     for(String productId : productsIds) {
        productsIdsFormatted.append(productId).append(",");
     }

    String url = "https://www.confianca.com.br/ccstore/v1/products?productIds=" + productsIdsFormatted + "&continueOnMissingProduct=true";
    this.log("Link onde são feitos os crawlers: " + url);

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("X-CCProfileType", "storefrontUI");
    headers.put("X-CC-MeteringMode", "CC-NonMetered");
    headers.put("X-CCSite", "Confianca");
    headers.put("X-CCPriceListGroup", "DefaultPriceListFlex");
    headers.put("X-CCAsset-Language", "pt-BR");
    headers.put("X-CC-Frontend-Forwarded-Url", "www.confianca.com.br/" + this.cityName + "/search?Ntt=" + this.keywordEncoded);


    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
    return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody()).optJSONArray("items");
  }

  private List<Boolean> crawlStockStatusApi(List<String> productsIds) {
     List<Boolean> productAvailability = new ArrayList<>();

     StringBuilder productsIdsFormatted = new StringBuilder();
     for(String productId : productsIds) {
        productsIdsFormatted.append(productId).append(":").append(productId).append(",");
     }

     Map<String, String> headers = new HashMap<>();
     headers.put("Referer", session.getOriginalURL());

     String url = "https://www.confianca.com.br/ccstore/v1/stockStatus?actualStockStatus=true&locationIds=Confianca&products=" + productsIdsFormatted;

     Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
     JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
     if (json != null) {
        JSONArray items = JSONUtils.getValueRecursive(json, "items", "/", JSONArray.class, new JSONArray());
        for (int i = 0; i < items.length(); i++) {
           String stockStatus = items.optJSONObject(i).optString("stockStatus");
           if (stockStatus != null && stockStatus.equals("IN_STOCK")) {
              productAvailability.add(true);
           } else {
              productAvailability.add(false);
           }
        }
     }

     return productAvailability;
  }

   protected void setTotalProducts(JSONObject catalog) {
      Integer totalProducts = JSONUtils.getValueRecursive(catalog, "results/totalNumRecs", "/", Integer.class, 0);
      this.totalProducts = totalProducts != null ? totalProducts : 0;
      this.log("Total da busca: " + this.totalProducts);
   }
}
