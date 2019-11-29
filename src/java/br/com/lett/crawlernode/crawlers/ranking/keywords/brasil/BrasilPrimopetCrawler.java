package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.Arrays;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilPrimopetCrawler extends CrawlerRankingKeywords {

  private static final String HOME_PAGE = "www.primopet.com.br";
  private String keywordKey;

  public BrasilPrimopetCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    this.currentDoc = fetchPage();
    
    Elements products = this.currentDoc.select(".n12colunas > ul > li:not(.helperComplement)");
    Elements helper = this.currentDoc.select(".n12colunas > ul > li.helperComplement");

    if (!products.isEmpty()) {     
      for(int i = 0; i < products.size() && i < helper.size(); i++) {
        Element prod = products.get(i);
        Element help = helper.get(i);

        String productPid = scrapInternalPid(help);
        String productUrl = CrawlerUtils.scrapUrl(prod, ".product-image > a", Arrays.asList("href"), "https", HOME_PAGE);

        saveDataProduct(null, productPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + productPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;

      }
    } else if(this.arrayProducts.isEmpty()) {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

  private Document fetchPage() {
    Document doc = new Document("");

    if (this.currentPage == 1) {
      String url = "https://www.primopet.com.br/" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);

      Elements scripts = doc.select("script[type=text/javascript]");
      String token = "/busca?fq=";

      for (Element e : scripts) {
        String html = e.html();

        if (html.contains(token)) {
          this.keywordKey = CrawlerUtils.extractSpecificStringFromScript(html, "fq=", false, "&", false);
          break;

        }
      }
    } else if (this.keywordKey != null) {
      String url = "https://www.primopet.com.br/buscapagina?fq=" + this.keywordKey
          + "&PS=12&sl=d1ff7f21-5d18-4b77-8d05-05f9f58adab1&cc=12&sm=0&PageNumber=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);
      doc = fetchDocument(url);
    }

    return doc;
  }
  
  private String scrapInternalPid(Element e) {
    String internalPid = null;
    
    if(e != null && !e.id().isEmpty() && e.id().startsWith("helperComplement_")) {
      internalPid = e.id().substring("helperComplement_".length());
    }
    
    return internalPid;
  }
  
  @Override
  protected boolean hasNextPage() {
    return !(this.currentDoc.select(".n12colunas > ul > li:not(.helperComplement)").size() < this.pageSize);
  }
}
