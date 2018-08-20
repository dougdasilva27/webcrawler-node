package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.ArrayList;
import java.util.List;
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
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 36;
    this.log("Página " + this.currentPage);

    String url = "https://www.enutri.com.br/catalogsearch/result/index/?limit=36&p=" + this.currentPage + "&q=" + this.keywordEncoded;
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = fetchDocument(url, cookies);

    Elements products = this.currentDoc.select(".category-products .item-area");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = crawlInternalId(e);
        String productUrl = crawlProductUrl(e);

        saveDataProduct(internalId, null, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
  protected void setTotalProducts() {
    JSONObject infoJson = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "script[type=\"text/javascript\"]", "vardigitalDataTemp=", ";", true);

    if (infoJson.has("listing")) {
      JSONObject listing = infoJson.getJSONObject("listing");

      if (listing.has("resultCount")) {
        String text = listing.get("resultCount").toString().replaceAll("[^0-9]", "").trim();

        if (!text.isEmpty()) {
          this.totalProducts = Integer.parseInt(text);
          this.log("Total da busca: " + this.totalProducts);
        }
      }
    }
  }

  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.select(".i-next").first() != null;
  }

  private String crawlInternalId(Element e) {
    String internalId = null;

    Element idElement = e.select(".configurable-actions").first();
    if (idElement != null) {
      internalId = CommonMethods.getLast(idElement.attr("id").split("-"));
    }

    return internalId;
  }

  private String crawlProductUrl(Element e) {
    String productUrl = null;

    Element url = e.select(".product-name > a").first();
    if (url != null) {
      productUrl = url.attr("href");

      if (!productUrl.contains("enutri.com")) {
        productUrl = ("https://www.enutri.com.br/" + productUrl).replace(".br//", ".br/");
      }
    }

    return productUrl;
  }
}
