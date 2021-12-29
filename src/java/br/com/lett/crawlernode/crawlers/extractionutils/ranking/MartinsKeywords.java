package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class MartinsKeywords extends CrawlerRankingKeywords {

  public MartinsKeywords(Session session) {
    super(session);
  }

  protected String password = getPassword();
  protected String login = getLogin();

  protected String getPassword(){
     return session.getOptions().optString("pass");
  }

  protected String getLogin(){
      return session.getOptions().optString("login");

   }

  @Override
  protected void processBeforeFetch() {
    try {
      this.webdriver = DynamicDataFetcher
          .fetchPageWebdriver("https://www.martinsatacado.com.br/login", ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);
      this.webdriver.waitLoad(10000);

      WebElement email = this.webdriver.driver.findElement(By.cssSelector("#js_username_login"));
      email.sendKeys(login);
      this.webdriver.waitLoad(2000);

      WebElement cnpj = this.webdriver.driver.findElement(By.cssSelector("#jsSelectCNPJ"));
      this.webdriver.clickOnElementViaJavascript(cnpj);
      this.webdriver.waitLoad(2000);

      WebElement pass = this.webdriver.driver.findElement(By.cssSelector("#j_password[required]"));
      pass.sendKeys(password);
      this.webdriver.waitLoad(2000);

      WebElement login = this.webdriver.driver.findElement(By.cssSelector(".sectionButtons .btn-primary"));
      this.webdriver.clickOnElementViaJavascript(login);
      this.webdriver.waitLoad(2000);
    } catch (Exception e) {
      Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
    }
  }

  @Override
  public void extractProductsFromCurrentPage() {
    this.pageSize = 12;
    this.log("Página " + this.currentPage);

    // monta a url com a keyword, page size e a página
    String url = "https://www.martinsatacado.com.br/busca/?text=/engage/search/v3/search?terms=" + this.keywordWithoutAccents.replace(" ", "%20") + "&resultsperpage=" + this.pageSize + "&saleschannel=default&page=" + this.currentPage;


    this.log("Link onde são feitos os crawlers: " + url);
    this.currentDoc = fetchDocumentWithWebDriver(url);

    Elements products = this.currentDoc.select(".product[data-sku]");

    if (!products.isEmpty()) {
      if (this.totalProducts == 0) {
        setTotalProducts();
      }

      for (Element e : products) {
        String internalId = CommonMethods.getLast(e.attr("data-sku").split("_"));
        String urlProduct = CrawlerUtils
            .scrapUrl(e, "a", "href", "https", "www.martinsatacado.com.br");

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
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".fr.obs1.reslts", null, null, true, true, 0);
    this.log("Total da busca: " + this.totalProducts);
  }
}
