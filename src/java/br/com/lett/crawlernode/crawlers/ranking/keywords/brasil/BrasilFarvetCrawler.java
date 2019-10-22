package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.net.URI;
import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilFarvetCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "farvet.com.br";

  public BrasilFarvetCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    String url = "http://" + HOME_PAGE + "/busca.asp?palavrachave="
        + this.keywordEncoded + "&idpage=" + this.currentPage;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".produtosCat > ul > li.foto");

    if (!products.isEmpty()) {

      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String productUrl = CrawlerUtils.scrapUrl(e, "a.fotoProduto", Arrays.asList("href"), "https:", HOME_PAGE);
        String internalId = crawlInternalId(productUrl);

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


  private String crawlInternalId(String url) {
    String internalId = null;
    
    URI uri = URI.create(url);
    String path = uri.getPath();
    String[] splitPath = path.split("/");
    
    for(int i = 0; i < splitPath.length; i++) {
      String subPath = splitPath[i];
    
      if(subPath.equals("produto") && (i+1) < splitPath.length) {
        internalId = splitPath[i+1];
        break;
      }
    }

    return internalId;
  }
  
  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.selectFirst(".btns_proximo") != null;
  }
}
