package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilCentauroCrawler extends CrawlerRankingKeywords {

  public BrasilCentauroCrawler(Session session) {
    super(session);
  }

  private boolean isCategory;
  private String urlCategory;

  @Override
  protected void extractProductsFromCurrentPage() {
    this.log("Página " + this.currentPage);

    String url =
        "https://esportes.centauro.com.br/search?p=Q&lbc=centauro&uid=305597365&ts=custom&w=" + this.keywordWithoutAccents.replace(" ", "%20")
            + "&srt=" + this.arrayProducts.size() + "&isort=globalpop&method=and&view=grid&sli_jump=1&af=";

    if (this.currentPage > 1 && isCategory) {
      String token = CommonMethods.getLast(this.urlCategory.split("/"));
      url = this.urlCategory.replace(token, "") + this.arrayProducts.size();
    }

    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url);

    Elements products = this.currentDoc.select(".sli_grid_container[data-sku] > div");

    if (this.currentPage == 1) {
      String redirectUrl = this.session.getRedirectedToURL(url);
      if (redirectUrl != null && !redirectUrl.equals(url)) {
        isCategory = true;
        this.urlCategory = redirectUrl;
      } else {
        isCategory = false;
      }
    }

    this.pageSize = 48;

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalPid = e.attr("data-product-id");
        String productUrl = crawlProductUrl(e);

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
    return !this.currentDoc.select(".control-next").isEmpty();
  }

  @Override
  protected void setTotalProducts() {
    Element totalElement = null;
    totalElement = this.currentDoc.select(".sli_bct_total_records").first();

    if (totalElement != null) {
      String text = totalElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        this.totalProducts = Integer.parseInt(text);
      }
    }
    this.log("Total da busca: " + this.totalProducts);
  }

  private String crawlProductUrl(Element e) {
    String urlProduct = e.attr("data-url");

    if (urlProduct.contains("search")) {
      String[] tokens = urlProduct.split("&");

      for (String token : tokens) {
        if (token.startsWith("url=")) {
          String encodedUrl = token.replace("url=", "");

          try {
            urlProduct = URLDecoder.decode(encodedUrl, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
            this.logError("Error on decode url.", ex);
          }
          break;
        }
      }
    }

    return urlProduct;
  }
}
