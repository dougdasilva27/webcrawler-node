package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.lett.crawlernode.util.CommonMethods;
import org.apache.http.cookie.Cookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ArgentinaDiaCrawler extends CrawlerRankingKeywords {

  public ArgentinaDiaCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    this.pageSize = 26;

    String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");
    String url = "https://diaonline.supermercadosdia.com.ar/" + keyword + "?PageNumber=" + this.currentPage;

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select(".prateleira.vitrine ul .perfumeria .box-item");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0)
        setTotalProducts();

      for (Element e : products) {
        String internalPid = e.attr("id").contains("product-")? CommonMethods.getLast(e.attr("id").split("product-")): null;
        String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-image", "href");

        saveDataProduct(null, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit)
          break;
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
  }

   @Override
   protected boolean hasNextPage() {
      return this.currentDoc.selectFirst(".viewMoreProds") != null;
   }
}
