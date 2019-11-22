package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilBitcaoCrawler extends CrawlerRankingKeywords {
  
  private static final String HOME_PAGE = "www.bitcao.com.br";

  public BrasilBitcaoCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 16;
    this.log("Página " + this.currentPage);

    String url = "https://www.bitcao.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".category-products > ul > .item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        String internalPid = scrapInternalPid(e);
        String productUrl = CrawlerUtils.scrapUrl(e, "a.product-image", Arrays.asList("href"), "https", HOME_PAGE);

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


  private String scrapInternalPid(Element e) {
    String internalPid = null;
    Element pidElement = e.selectFirst("[id*=product-price-]");
    
    if(pidElement != null && !pidElement.id().isEmpty() && pidElement.id().startsWith("product-price-")) {
      internalPid = pidElement.id().substring("product-price-".length());
    }

    return internalPid;
  }
  
  @Override
  protected void setTotalProducts() {
    
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pager .amount", 
        "do", "total", false, false, 0);
    
    this.log("Total da busca: " + this.totalProducts);    
  }
}
