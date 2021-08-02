package br.com.lett.crawlernode.crawlers.ranking.keywords.portoalegre;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;

public class PortoalegreAsunCrawler extends CrawlerRankingKeywords {
   public PortoalegreAsunCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      pageSize = 16;

      String url = "https://loja.asun.com.br/pesquisa?p=" + keywordEncoded+"&page="+currentPage;
      Document document = fetch(url);

      Elements itens = document.select(".result article");
      if (itens.size()>0){

         for (Element item : itens) {
            String incompleteUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(item, "a", "href");
            String[] urlSplit = incompleteUrl.split("/");
            String internalId = urlSplit[urlSplit.length-2];

            String productUrl = CrawlerUtils.completeUrl(incompleteUrl,"https","loja.asun.com.br");

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);
            saveDataProduct(internalId,internalId,productUrl);
         }
      }else {
         this.result=false;
      }

   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   private Document fetch(String url) {

      webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, session);
      waitForElement(webdriver.driver, ".form-group input");
      WebElement cep = webdriver.driver.findElement(By.cssSelector(".form-group input"));
      cep.sendKeys("91740-001");
      webdriver.waitLoad(2000);
      waitForElement(webdriver.driver, ".form-group button");
      WebElement login = webdriver.driver.findElement(By.cssSelector(".form-group button"));
      webdriver.clickOnElementViaJavascript(login);
      webdriver.waitLoad(2000);
      WebElement delivery = webdriver.driver.findElement(By.cssSelector(".method-delivery button"));
      webdriver.clickOnElementViaJavascript(delivery);
      webdriver.waitLoad(5000);
      Document doc = Jsoup.parse(webdriver.getCurrentPageSource());
      webdriver.driver.close();

      return doc;

   }

   public static void waitForElement(WebDriver driver, String cssSelector) {
      WebDriverWait wait = new WebDriverWait(driver, 20);
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
   }
}
