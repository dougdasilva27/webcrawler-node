package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLilianiCrawler extends CrawlerRankingKeywords {

  public BrasilLilianiCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    // número de produtos por página do market
    this.pageSize = 12;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://search.smarthint.co/v1/Search/GetPrimarySearch?shcode=SH-275477&term="  + this.keywordEncoded + "&from=" + (this.currentPage -1) * 12 + "&size=12&searchSort=0";
    this.log("Link onde são feitos os crawlers: " + url);
//https://search.smarthint.co/v1/Search/GetPrimarySearch?shcode=SH-275477&term=geladeira&from=24&size=12&searchSort=0
    // chama função de pegar o html
    JSONObject json = fetchJSONObject(url);

     JSONArray products = json.optJSONArray("Products");

    if (!products.isEmpty()) {

      for (Object o : products) {
         if (o instanceof JSONObject){
            JSONObject product = (JSONObject) o;

        String internalId = product.optString("ProductId");
        String productUrl = CrawlerUtils.completeUrl(product.optString("Link"), "https", "wwww.liliani.com.br");

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }}
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
