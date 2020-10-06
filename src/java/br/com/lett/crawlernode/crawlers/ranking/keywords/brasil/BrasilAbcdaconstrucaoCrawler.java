package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilAbcdaconstrucaoCrawler extends CrawlerRankingKeywords {

  public BrasilAbcdaconstrucaoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {

    this.pageSize = 24;

    this.log("Página " + this.currentPage);

    String url = "https://www.abcdaconstrucao.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);


    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".spotContent");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        //scrap internalId
        String rawInternal = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".lista-desejos-spot a", "id");
        String internalId = rawInternal.contains("produto-")? rawInternal.split("produto-")[1]: null;

        String productUrl = CrawlerUtils.scrapUrl(e, ".spot-parte-um", "href", "https:", "www.abcdaconstrucao.com.br");

        saveDataProduct(internalId, null, productUrl);

        this.log(
                "Position: " + this.position +
                        " - InternalId: " + internalId +
                        " - InternalPid: " + null +
                        " - Url: " + productUrl
        );

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

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".fbits-qtd-produtos-pagina", true, 0);
    this.log("Total: " + this.totalProducts);
  }

}
