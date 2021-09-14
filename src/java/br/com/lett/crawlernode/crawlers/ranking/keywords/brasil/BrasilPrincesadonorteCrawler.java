package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPrincesadonorteCrawler extends CrawlerRankingKeywords {

  public BrasilPrincesadonorteCrawler(Session session) {
    super(session);
    super.fetchMode = FetchMode.FETCHER;
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    String url = "https://www.princesadonorteonline.com.br/catalogsearch/result?p="+this.currentPage+"&q="+keywordEncoded;
    //https://www.princesadonorteonline.com.br/catalogsearch/result/?q=Esmalte

    this.pageSize = 9;
    this.log("Página " + this.currentPage);

    Document doc = fetchDocument(url);
    Elements products = doc.select("ol.products li");

    if (!products.isEmpty()) {
       if(totalProducts==0){
          setTotalProducts(doc);
       }

      for (Element product : products) {

        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product,"div[data-product-id]","data-product-id");
        String internalId = internalPid;
        String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product,"a","href");

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

   private void setTotalProducts(Document doc) {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(doc, "#toolbar-amount > span:last-child", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }
}
