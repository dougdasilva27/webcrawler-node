package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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

import java.util.*;

public class BrasilSuperbomemcasaCrawler extends CrawlerRankingKeywords {

   public BrasilSuperbomemcasaCrawler(Session session) {
      super(session);
   }

   private JSONObject requestGet(String path) {
      String url = "https://sb.superbomemcasa.com.br/"+ path;

      Map<String, String> headers = new HashMap<>();

      headers.put("Accept", "*/*");
      headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Referer", "https://superbomemcasa.com.br/");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      return JSONUtils.stringToJson(response.getBody());
   }

   private JSONObject requestJsonId() {

      String path = "rest/default/V1/search" +
         "?searchCriteria[currentPage]=" + this.currentPage +
         "&searchCriteria[requestName]=quick_search_container" +
         "&searchCriteria[filterGroups][0][filters][0][field]=search_term" +
         "&searchCriteria[filterGroups][0][filters][0][conditionType]=" +
         "&searchCriteria[filterGroups][0][filters][0][value]=" + this.keywordEncoded +
         "&searchCriteria[filterGroups][1][filters][0][field]=mostrar" +
         "&searchCriteria[filterGroups][1][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][1][filters][0][value]=0" +
         "&searchCriteria[filterGroups][2][filters][0][field]=status" +
         "&searchCriteria[filterGroups][2][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][2][filters][0][value]=1";

      return requestGet(path);
   }

   private JSONObject requestProducts(List<String> ids) {

      String path = "rest/default/V1/products/" +
         "?searchCriteria[currentPage]=1&searchCriteria[pageSize]=" + ids.size() +
         "&searchCriteria[filterGroups][0][filters][0][field]=entity_id" +
         "&searchCriteria[filterGroups][0][filters][0][conditionType]=in" +
         "&searchCriteria[filterGroups][0][filters][0][value]=" + String.join(",", ids) +
         "&searchCriteria[filterGroups][1][filters][0][field]=mostrar" +
         "&searchCriteria[filterGroups][1][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][1][filters][0][value]=0" +
         "&searchCriteria[filterGroups][2][filters][0][field]=status" +
         "&searchCriteria[filterGroups][2][filters][0][conditionType]=eq" +
         "&searchCriteria[filterGroups][2][filters][0][value]=1";

      return requestGet(path);
   }

   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {

      JSONObject jsonIds = requestJsonId();

      List<String> ids = new ArrayList();
      JSONUtils.getJSONArrayValue(jsonIds, "items").forEach(item -> {
         if (item instanceof JSONObject) {
            ids.add(((JSONObject) item).optString("id"));
         }
      });

      JSONObject jsonProducts = requestProducts(ids);

      JSONArray products = JSONUtils.getJSONArrayValue(jsonProducts, "items");

      Map<String, JSONObject> productsMap = new HashMap<>();

      JSONUtils.getJSONArrayValue(jsonProducts, "items").forEach(item -> {
         if (item instanceof JSONObject) {
            productsMap.put(((JSONObject) item).optString("id"), (JSONObject) item);
         }
      });

      totalProducts = products.length();

      for (String internalId : ids) {
         JSONObject product = productsMap.get(internalId);
         if (product != null) {
            String internalPid = product.optString("sku");
            JSONArray jsonAtt = JSONUtils.getJSONArrayValue(product, "custom_attributes");
            String pathUrl = scrapAttributes(jsonAtt);
            String name = product.optString("name");
            Integer price = product.optDouble("price", -1) == -1 ? null : (int) (product.optDouble("price") * 100);
            String imageIncomplete = (String) product.optQuery("/media_gallery_entries/0/file");
            String imageUrl = "https://superbom.s3-sa-east-1.amazonaws.com/catalog/product" + imageIncomplete;



            if (!internalPid.isEmpty()) {
               String productUrl = "https://superbomemcasa.com.br/" + pathUrl + "." + internalPid + "p";

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(price != null)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);
            }
         }
      }
   }

   private String scrapAttributes(JSONArray jsonArray) {
      for (Object o : jsonArray) {
         if (o instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) o;
            if (Objects.equals(jsonObject.optString("attribute_code"), "url_key")) {
               return jsonObject.optString("value");
            }
         }
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }
}
