package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

import java.util.Collections;

public class BrasilHomerefillCrawler extends CrawlerRankingKeywords {

  public BrasilHomerefillCrawler(Session session) {
    super(session);
  }

  private String redirectUrl;

  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    this.pageSize = 40;
    this.log("Página " + this.currentPage);
    String url = "https://www.homerefill.com.br/shopping/search?search=" + this.keywordEncoded;

    if (this.currentPage == 1) {
      this.currentDoc = fetchDocument(url);
      this.redirectUrl = session.getRedirectedToURL(url) != null ? session.getRedirectedToURL(url) : url;
    } else {
      this.currentDoc = fetchDocument(this.redirectUrl + "&page=" + this.currentPage);
    }
    this.log("Link onde são feitos os crawlers: " + this.redirectUrl + "&page=" + this.currentPage);

    Elements products = this.currentDoc.select(".organism-product div[data-product-sku]");
    Element emptySearch = this.currentDoc.selectFirst(".page-department__suggests");

    if (!products.isEmpty() && emptySearch == null) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = crawlInternalPid();
        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

         String name = CrawlerUtils.scrapStringSimpleInfo(e, "h3", true);
         String imgUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".atom-product-image img", "src");
         Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".molecule-new-product-card__price", null, false, ',', session, 0);
         boolean isAvailable = price != 0;

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setImageUrl(imgUrl)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build();

         saveDataProduct(productRanking);

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
  protected boolean hasNextPage() {
    return this.arrayProducts.size() < this.totalProducts;
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("h2.page-search__header__title").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }

      this.log("Total da busca: " + this.totalProducts);
    }
  }

  private String crawlInternalId(Element e) {
    return e.attr("data-product-sku");
  }

  private String crawlInternalPid() {
    return null;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;
    Element eUrl = e.select("a[href]").first();

    if (eUrl != null) {
      productUrl = eUrl.attr("href");
    }

    return productUrl;
  }
}
