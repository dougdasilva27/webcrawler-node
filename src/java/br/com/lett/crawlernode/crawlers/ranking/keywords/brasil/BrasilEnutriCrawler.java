package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilEnutriCrawler extends CrawlerRankingKeywords {

  public BrasilEnutriCrawler(Session session) {
    super(session);
  }

  private List<Cookie> cookies = new ArrayList<>();

  @Override
  public void processBeforeFetch() {
    this.log("Adding cookie...");
    BasicClientCookie cookie = new BasicClientCookie("loja", "base");
    cookie.setDomain("www.enutri.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 32;
    this.log("Página " + this.currentPage);

    String url = "https://www.enutri.com.br/loja/busca.php?loja=1061635&palavra_busca=" + this.keywordEncoded +"&pg=" +  this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select(".showcase-catalog li");

    if (!products.isEmpty()) {
      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product a", "href");
        String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-name", true);
        String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".image img", Collections.singletonList("data-src"), "https", "images.tcdn.com.br");
        Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".price-off", null, true, ',', session, 0);
        boolean isAvailable = price != 0;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(null)
            .setImageUrl(imgUrl)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
        if (this.arrayProducts.size() == productsLimit) {
          break;
        }

      }
    } else {
       this.result = false;
       this.log("Keyword sem resultados!");
    }

     this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

   private String crawlInternalId(Element e) {
     String urlWithId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".botaoCatalog", "href");
      return CommonMethods.getLast(urlWithId.split("IdProd="));
   }

   @Override
   protected boolean hasNextPage() {
     return !this.currentDoc.select(".page-next").isEmpty();
  }
}
