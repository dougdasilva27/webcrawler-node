package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilTintasverginiaCrawler extends CrawlerRankingKeywords {
  
  private static final String HOST = "www.tintasverginia.com.br";

  public BrasilTintasverginiaCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 40;
    this.log("Página " + this.currentPage);

    String url = "https://www.tintasverginia.com.br/catalogsearch/result/index/?p=" + this.currentPage + "&q=" + this.keywordEncoded;
    
    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url);
    Elements products = this.currentDoc.select(".products-grid:not(.historico) > .item");

    if (!products.isEmpty()) {
      if(this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[product-id]", "product-id");
        String internalPid = internalId;
        String productUrl = CrawlerUtils.scrapUrl(e, ".product-image", "href", "https", HOST);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log(
            "Position: " + this.position + 
            " - InternalId: " + internalId +
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
  protected void setTotalProducts() {    
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".amount > strong:last-child", "", "", true, false, 0);
    
    this.log("Total products: " + this.totalProducts);
  }
}
