package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class BrasilKabumCrawler extends CrawlerRankingKeywords {

   public BrasilKabumCrawler(Session session) {
      super(session);
   }


   public JSONObject crawlApi(String url) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 20;

      String url = "https://servicespub.prod.api.aws.grupokabum.com.br/catalog/v1/products?query=" + this.keywordEncoded + "&page_number=" + this.currentPage + "&page_size=" + this.productsLimit + "&facet_filters=&sort=most_searched&include=gift";

      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject jsonObject = crawlApi(url);

      JSONArray jsonArray = jsonObject.optJSONArray("data");

      if (!jsonArray.isEmpty()) {
         for (Object obj : jsonArray) {

            JSONObject json = (JSONObject) obj;

            String internalId = json.optString("id");
            String urlProduct = null;
            try {
               urlProduct = CrawlerUtils.completeUrl(internalId + getSlug(json), "https://", "kabum.com.br/produto");
            } catch (UnsupportedEncodingException e) {
               e.printStackTrace();
            }

            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
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

   private String getSlug(JSONObject data) throws UnsupportedEncodingException {

      String name = JSONUtils.getValueRecursive(data, "attributes.title", String.class);
      return "/" + name.replaceAll("[^0-9a-zA-Z]+", "-").toLowerCase(Locale.ROOT);

   }
}
