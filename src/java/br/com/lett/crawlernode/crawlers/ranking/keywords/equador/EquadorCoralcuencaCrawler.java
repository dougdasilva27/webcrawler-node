package br.com.lett.crawlernode.crawlers.ranking.keywords.equador;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.UnsupportedEncodingException;

public class EquadorCoralcuencaCrawler extends CrawlerRankingKeywords {

   public EquadorCoralcuencaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      Document document = fetch();
      this.currentDoc = document;

      Elements products =  document.select(".listaProductoBus .wrapperDetalle");
      for (Element product : products) {

         String productUrl = CrawlerUtils.scrapUrl(product, "a", "href", "https", "www.coralhipermercados.com");
         String internalId = CommonMethods.getLast(productUrl.split("/"));

         String name = CrawlerUtils.scrapStringSimpleInfo(product,".nomProdCarrito",true);
         Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(product,".preProductoCar",null,true,'.',session,null);

         String imageUrl = CrawlerUtils.scrapUrl(product,"a img","src","https","s3.amazonaws.com");

         RankingProduct productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPriceInCents(price)
            .setAvailability(true)
            .setImageUrl(imageUrl)
            .build();

         saveDataProduct(productRanking);

      }
   }

   protected Document fetch() {
      Document doc = null;
      String url = null;

      if(this.currentPage == 1) {
         url = "https://www.coralhipermercados.com/buscar?q=" + this.keywordEncoded.replaceAll(" ","%20");
      } else {
       url = "https://www.coralhipermercados.com/buscar?q=" + this.keywordEncoded.replaceAll(" ","%20")+ "&page=" + this.currentPage;

      }

      webdriver = DynamicDataFetcher.fetchPageWebdriver(url, ProxyCollection.BUY_HAPROXY, session);

      this.log("awaiting page");

      webdriver.waitForElement(".lotties", 25);

      webdriver.waitLoad(6000);
      webdriver.waitForElement(".botonSelectCiudad",25);

      WebElement city = webdriver.driver.findElement(By.xpath(("//div[@class='botonSelectCiudad']")));
      webdriver.clickOnElementViaJavascript(city);
      webdriver.waitPageLoad(35);
      doc = Jsoup.parse(webdriver.getCurrentPageSource());
      webdriver.terminate();
      return doc;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pagination li:last-of-type").hasClass("disabled");
   }
}
