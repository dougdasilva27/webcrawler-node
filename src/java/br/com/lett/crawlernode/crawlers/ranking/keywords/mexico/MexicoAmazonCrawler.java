package br.com.lett.crawlernode.crawlers.ranking.keywords.mexico;

import java.util.HashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.AmazonScraperUtils;
import br.com.lett.crawlernode.util.CommonMethods;

public class MexicoAmazonCrawler extends CrawlerRankingKeywords {

  public MexicoAmazonCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.amazon.com.mx";
  private String nextPageUrl;

  private AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    String url;

    if (this.currentPage == 1) {
      url = "https://www.amazon.com.mx/s/ref=nb_sb_noss?url=search-alias%3Daps&page=" + this.currentPage + "&keywords=" + this.keywordEncoded;
    } else {
      url = this.nextPageUrl;
    }
    this.log("Link onde são feitos os crawlers: " + url);

    this.currentDoc = Jsoup.parse(amazonScraperUtils.fetchPage(url, new HashMap<>(), cookies, session, dataFetcher));
    this.nextPageUrl = crawlNextPage();

    Elements products = this.currentDoc.select(".s-result-list .s-result-item");
    Element result = this.currentDoc.select("#noResultsTitle").first();

    if (!products.isEmpty() && result == null) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {

        String internalPid = crawlInternalPid(e);
        String internalId = internalPid;
        String productUrl = crawlProductUrl(internalPid);

        saveDataProduct(internalId, internalPid, productUrl);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

  private String crawlNextPage() {
    String url = null;

    Element e = this.currentDoc.select(".pagnRA > a").first();

    if (e != null) {
      url = e.attr("href");

      if (url.startsWith("#")) {
        url = url.replaceFirst("#", "");
      }

      if (!url.contains("amazon.com")) {
        url = HOME_PAGE + url;
      }
    }

    return url;
  }

  @Override
  protected boolean hasNextPage() {
    return this.currentDoc.select("#pagnNextString").first() != null;
  }

  @Override
  protected void setTotalProducts() {
    Element total = this.currentDoc.selectFirst("#s-result-count");
    if (total != null) {
      String text = total.ownText();

      if (!text.contains("de")) {
        String totalText = CommonMethods.getLast(text.split("de")).replaceAll("[^0-9]", "");

        if (!totalText.isEmpty()) {
          this.totalProducts = Integer.parseInt(totalText);
          this.log("Total da busca: " + this.totalProducts);
        }
      }
    }
  }

  private String crawlInternalPid(Element e) {
    return e.attr("data-asin");
  }

  private String crawlProductUrl(String id) {
    return HOME_PAGE + "/dp/" + id;
  }

}
