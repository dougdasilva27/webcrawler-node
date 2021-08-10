package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MerqueoCrawler extends CrawlerRankingKeywords {

   public MerqueoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      String url = "https://merqueo.com/api/3.1/stores/281/search?q=+" + this.keywordEncoded + "&per_page=" + this.currentPage + "&per_page=50&zoneId="+ session.getOptions().optString("zoneId")+"&sort_by=relevance";
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject apiJson = fetchApiProducts(url);
      JSONArray productsArray = new JSONArray();

      if (apiJson.has("included") && !apiJson.isNull("included")) {
         productsArray = apiJson.getJSONArray("included");

      }

      if (productsArray.length() > 0) {
         if (this.totalProducts == 0) {
            this.totalProducts = productsArray.length();
         }

         for (Object object : productsArray) {
            JSONObject included = (JSONObject) object;

            if (included.optString("type").equals("slug")) {

               String internalId = included.has("id") ? included.get("id").toString() : null;
               String productUrl = assembleProductUrl(included);
               saveDataProduct(internalId, null, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
               if (this.arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   /*
    * Url exemple:
    * https://merqueo.com/bogota/despensa/condimentos-especias-y-adobos/canela-su-despensa-20-gr
    */
   private String assembleProductUrl(JSONObject included) {
      String productUrl = "";


      if (included.has("attributes") && !included.isNull("attributes")) {
         JSONObject attributes = included.getJSONObject("attributes");

         boolean hasFields = attributes.has("city")
            && attributes.has("department")
            && attributes.has("shelf")
            && attributes.has("product");

         boolean isFieldsNull =
            !attributes.isNull("city")
               && !attributes.isNull("department")
               && !attributes.isNull("shelf")
               && !attributes.isNull("product");

         if (hasFields && isFieldsNull) {
            productUrl = productUrl
               .concat("https://merqueo.com.br/")
               .concat(attributes.getString("city"))
               .concat("/")
               .concat("merqueo-sao-paulo")
               .concat("/")
               .concat(attributes.getString("department"))
               .concat("/")
               .concat(attributes.getString("shelf"))
               .concat("/")
               .concat(attributes.getString("product"));
         }
      }

      return productUrl;
   }

   private JSONObject fetchApiProducts(String url) {

      Request request = Request.RequestBuilder
         .create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
   }
}
