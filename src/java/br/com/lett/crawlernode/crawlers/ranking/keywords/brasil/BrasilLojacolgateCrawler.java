package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilLojacolgateCrawler extends CrawlerRankingKeywords {
  
  private static final String HOST = "lojacolgate.com.br";
  
  private static final String LOGIN_URL = "https://lojacolgate.com.br/cpb2bglobalstorefront/colgateSite/pt/login";
  private static final String CNPJ = " 45543915000181";
  private static final String PASSWORD = "12345678";

  public BrasilLojacolgateCrawler(Session session) {
    super(session);
  }
  
  @Override
  protected void processBeforeFetch() {    
    try {
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(LOGIN_URL, session);
      this.webdriver.waitLoad(10000);
      
      WebElement email = this.webdriver.driver.findElement(By.cssSelector("#j_username"));
      email.sendKeys(CNPJ);
      this.webdriver.waitLoad(2000);
      
      WebElement pass = this.webdriver.driver.findElement(By.cssSelector("#j_password"));
      pass.sendKeys(PASSWORD);
      this.webdriver.waitLoad(2000);
      
      WebElement login = this.webdriver.driver.findElement(By.cssSelector("#loginForm .btn-primary"));
      this.webdriver.clickOnElementViaJavascript(login);
      this.webdriver.waitLoad(2000);
      
      for(org.openqa.selenium.Cookie cook : this.webdriver.driver.manage().getCookies()) {
        BasicClientCookie cookie = new BasicClientCookie(cook.getName(), cook.getValue());
        cookie.setDomain('.' + HOST);
        cookie.setPath("/");
        
        this.cookies.add(cookie);
      }      
    } catch (Exception e) {
      Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
    }
  }

  @Override
  protected void extractProductsFromCurrentPage() {
    this.pageSize = 20;
    this.log("Página " + this.currentPage);

    String url = "https://lojacolgate.com.br/cpb2bglobalstorefront/colgateSite/pt/search?q=" 
        + this.keywordEncoded + "%3Arelevance&view=Grid&page=" + (this.currentPage-1);
    
    this.log("Link onde são feitos os crawlers: " + url);
    
    this.currentDoc = fetchDocument(url, this.cookies);
    
    Elements products = this.currentDoc.select(".product__listing > .product__list--item");

    if (!products.isEmpty()) {
      if(this.totalProducts == 0) {
        setTotalProducts();
      }
      
      for (Element e : products) {
        Elements options = e.select("option[value]");
        
        String productUrl = CrawlerUtils.scrapUrl(e, "a.product__list--thumb", "href", "https", HOST);
        
        if(options.isEmpty()) {
          
          String internalId = scrapInternalId(e);

          saveDataProduct(internalId, null, productUrl);
  
          this.log(
              "Position: " + this.position + 
              " - InternalId: " + internalId +
              " - InternalPid: " + null + 
              " - Url: " + productUrl);
          
          if (this.arrayProducts.size() == productsLimit)
            break;
          
        } else {          
          for(Element optionElement : options) {
            String internalId = scrapVariationInternalId(optionElement);
    
            saveDataProduct(internalId, null, productUrl, this.position);
    
            this.log(
                "Position: " + this.position + 
                " - InternalId: " + internalId +
                " - InternalPid: " + null + 
                " - Url: " + productUrl);
            
            if (this.arrayProducts.size() == productsLimit)
              break;
          }
          
          this.position++;
        }
      }
    } else {
      this.result = false;
      this.log("Keyword sem resultado!");
    }

    this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
        + this.arrayProducts.size() + " produtos crawleados");
  }

  private String scrapInternalId(Element e) {
    String internalId = null;
    Element internalIdElement = e.selectFirst("[id*=ean-]");
    
    if(internalIdElement != null && internalIdElement.hasText()) {
      internalIdElement.text().trim();
    }
    
    return internalId;
  }
  
  private String scrapVariationInternalId(Element e) {
    String internalId = null;
    
    if(e.hasAttr("value")) {
      internalId = e.attr("value");
    }
    
    return internalId;
  }
  
  @Override
  protected void setTotalProducts() {    
    this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".pagination-bar-results", null, "encontrados", true, false, 0);
    
    this.log("Total products: " + this.totalProducts);
  }
}
