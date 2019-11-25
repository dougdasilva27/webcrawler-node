package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilTudodebichoCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "tudodebicho.com.br";

  public BrasilTudodebichoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    String url = "https://www.tudodebicho.com.br/busca?busca=" + this.keywordEncoded + "&pagina=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".items-shelf");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = scrapInternalPid(e);
        String productUrl = CrawlerUtils.scrapUrl(e, ".spotContent > a", Arrays.asList("href"), "https", HOME_PAGE);

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
  
  private String scrapInternalPid(Element doc) {
    String internalPid = null;
    Element pidElement = doc.selectFirst("[id*=produto-spot-item-]");
    
    if(pidElement != null) {
      String id = pidElement.id();
      
      if(id.startsWith("produto-spot-item-")) {
        internalPid = id.substring("produto-spot-item-".length());
      }
    }
    
    return internalPid;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".fbits-qtd-produtos-pagina", "", "", true, true, 0);
    this.log("Total de produtos: " + this.totalProducts);
  }
}
