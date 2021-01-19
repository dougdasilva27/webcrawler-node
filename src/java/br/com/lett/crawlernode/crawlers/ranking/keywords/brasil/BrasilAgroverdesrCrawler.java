package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.util.CommonMethods;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilAgroverdesrCrawler extends CrawlerRankingKeywords {

  private String keywordKey;

  public BrasilAgroverdesrCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 24;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchPage();
    Elements products = this.currentDoc.select("div.fbits-item-lista-spot ");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String productPid = ScrapProductPid(e);
        String productUrl = ScraoUrl(e);

        saveDataProduct(null, productPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + productPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

   private String ScraoUrl(Element e) {
     String urlfinal = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".spot-parte-um","href");
     return CrawlerUtils.completeUrl(urlfinal,"https:","www.agroverdesr.com.br");
   }

   private String ScrapProductPid(Element e) {
     String idAttr = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".spot","id");
     return CommonMethods.getLast(idAttr.split("-"));
   }

   private Document fetchPage() {
    Document doc = new Document("");

    if (this.currentPage == 1) {
      String url = "https://www.agroverdesr.com.br/busca?busca=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);

    } else {
      String url = "https://www.agroverdesr.com.br/busca?busca=" + this.keywordEncoded
          + "&pagina=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);
    }

    return doc;
  }

  @Override
  protected void setTotalProducts() {
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".fbits-qtd-produtos-pagina", null, null, false, false, 0);
    this.log("Total: " + this.totalProducts);

  }

}
