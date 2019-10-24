package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPethereCrawler extends CrawlerRankingKeywords {

  private static final String HOME_PAGE = "pethere.com.br";
  
  public BrasilPethereCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 40;
    this.log("Página " + this.currentPage);

    String url = "https://www.pethere.com.br/buscar?q=" + this.keywordEncoded + "&pagina=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select("#listagemProdutos .listagem-linha > ul > li");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalPid = CrawlerUtils.scrapStringSimpleInfo(e, ".produto-sku", true);
        String productUrl = CrawlerUtils.scrapUrl(e, ".info-produto a", Arrays.asList("href"), "https:", HOME_PAGE);

        saveDataProduct(null, internalPid, productUrl);

        this.log(
            "Position: " + this.position + 
            " - InternalId: " + null +
            " - InternalPid: " + internalPid + 
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
  
  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.selectFirst(".disabled [rel=\"next\"]") == null;
  }
}
