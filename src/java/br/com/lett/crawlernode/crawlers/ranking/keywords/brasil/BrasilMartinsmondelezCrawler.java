package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilMartinsmondelezCrawler extends CrawlerRankingKeywords {

  public BrasilMartinsmondelezCrawler(Session session) {
    super(session);
  }

  private static final String EMAIL_LOGIN = "erika.rosa@mdlz.com";
  private static final String PASSWORD = "monica08";


  @Override
  protected void processBeforeFetch() {
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver("https://www.martinsatacado.com.br/login", session);
    this.webdriver.waitLoad(10000);

    WebElement email = this.webdriver.driver.findElement(By.cssSelector("#j_username"));
    email.sendKeys(EMAIL_LOGIN);
    this.webdriver.waitLoad(2000);

    WebElement cnpj = this.webdriver.driver.findElement(By.cssSelector("#selectCNPJ"));
    this.webdriver.clickOnElementViaJavascript(cnpj);
    this.webdriver.waitLoad(2000);

    WebElement pass = this.webdriver.driver.findElement(By.cssSelector("#j_password"));
    pass.sendKeys(PASSWORD);
    this.webdriver.waitLoad(2000);

    WebElement login = this.webdriver.driver.findElement(By.cssSelector(".sectionButtons .btn-primary"));
    this.webdriver.clickOnElementViaJavascript(login);
    this.webdriver.waitLoad(2000);
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 9;
    this.log("Página " + this.currentPage);

    // monta a url com a keyword e a página
    String url;
    if (this.currentPage == 1) {
      url = "https://www.martinsatacado.com.br/martins/pt/BRL/search/?text=" + this.keywordWithoutAccents.replace(" ", "%20") + "&pageSize=45";
    } else {
      url = "https://www.martinsatacado.com.br/martins/pt/BRL/search/showMore?page=" + (this.currentPage - 1) + "&sort=relevance&q="
          + this.keywordWithoutAccents.replace(" ", "%20") + ":relevance&pageSize=45";
    }

    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocumentWithWebDriver(url);

    Elements products = this.currentDoc.select(".product[data-sku]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = CommonMethods.getLast(e.attr("data-sku").split("_"));
        String urlProduct = CrawlerUtils.scrapUrl(e, "a", "href", "https", "www.martinsatacado.com.br");

        saveDataProduct(internalId, null, urlProduct);

        this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".title1 .fr.obs1", null, null, true, true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
