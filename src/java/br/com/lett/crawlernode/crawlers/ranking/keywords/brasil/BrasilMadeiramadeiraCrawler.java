package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilMadeiramadeiraCrawler extends CrawlerRankingKeywords {

  public BrasilMadeiramadeiraCrawler(Session session) {
    super(session);
  }

   private JSONArray extractJSONFromHTML(){
      JSONArray jsonProducts = new JSONArray();

      Elements scripts = this.currentDoc.select("head script");
      for (Element e: scripts){
         if(e.html() != null && e.html().contains("window.segment_data =")){
            String json = e.html().split("window.segment_data =")[1];
            JSONObject rawJson = CrawlerUtils.stringToJson(json.replace(";",""));
            jsonProducts = rawJson.optJSONArray("products");
         }
      }
      return jsonProducts;
   }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 36;

    this.log("Página " + this.currentPage);
    String url = "https://www.madeiramadeira.com.br/busca?page="+ this.currentPage + "&q=" + this.keywordEncoded;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);

    JSONArray products = extractJSONFromHTML();

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Object o : products) {
         JSONObject product = (JSONObject) o;

        String internalPid = product.optString("product_id");
        String productUrl = CrawlerUtils.completeUrl(product.optString("url"), "https:", "www.madeiramadeira.com.br");

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: "
            + internalPid + " - Url: " + productUrl);

        if (this.arrayProducts.size() == productsLimit) {
          break;
        }
      }

    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst("#pagination-progress-page") != null;
   }
}
