package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilUninaturalCrawler extends CrawlerRankingKeywords {
  public BrasilUninaturalCrawler(Session session) {
    super(session);
  }

  @Override protected void extractProductsFromCurrentPage() {
    {
      this.pageSize = 0;
      this.log("Página " + this.currentPage);

      String url = "https://www.uninatural.com.br/ListaProdutos.asp?IDLoja=28284&Digitada=True&Texto=" + this.keywordWithoutAccents + "&ok=Digite+o+que+voc%EA+procura";

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".ajustaLista #ProdDestLista");

      if (!products.isEmpty()) {
        for (Element e : products) {
          String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href").split("IDProduto=")[1];
          String productUrl = CrawlerUtils.completeUrl(CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a", "href"), "https://", "www.uninatural.com.br");

          saveDataProduct(internalId, null, productUrl);

          this.log(
                "Position: " + this.position +
                      " - InternalId: " + internalId +
                      " - InternalPid: " + null +
                      " - Url: " + productUrl);
          if (this.arrayProducts.size() == productsLimit)
            break;
        }

      } else {
        this.result = false;
        this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
            + this.arrayProducts.size() + " produtos crawleados");

    }
  }


  @Override protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapSimpleInteger(this.currentDoc, "#idFoundFC b", false);
    this.log("Total da busca: " + this.totalProducts);
  }
}
