package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

import java.util.Arrays;

public class BrasilDafitiCrawler extends CrawlerRankingKeywords {

  public BrasilDafitiCrawler(Session session) {
    super(session);
  }

  @Override
  protected void extractProductsFromCurrentPage() throws MalformedProductException {
    // número de produtos por página do market
    this.pageSize = 48;

    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url = "https://www.dafiti.com.br/catalog/?q=" + this.keywordEncoded + "&page=" + this.currentPage;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select("div.product-box");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0)
        setTotalProducts();

      for (Element e : products) {
        // seta o id da classe pai com o id retirado do elements
        String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "div[id]", "id");
        String internalId = null;

        // monta a url
        Element urlElement = e.select("div[id] > a").first();
        String productUrl = urlElement.attr("href");
        String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-box-title", true);
        Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".product-box-price-to", null, true, ',', session,null);
        Boolean isAvailable = price != null;
        String imageUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".product-image", Arrays.asList("src"), "https", "t-static.dafiti.com.br");

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .setImageUrl(imageUrl)
            .build();

         saveDataProduct(productRanking);

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
  protected void setTotalProducts() {
    Element totalElement = this.currentDoc.select("div.items-products.select-options-item span").first();

    try {
      if (totalElement != null)
        this.totalProducts = Integer.parseInt(totalElement.text());
    } catch (Exception e) {
      this.logError(e.getMessage());
    }

    this.log("Total da busca: " + this.totalProducts);
  }

}
