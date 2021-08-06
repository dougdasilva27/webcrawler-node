package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;

public class ArgentinaCompreahoraCrawler extends CrawlerRankingKeywords {
   public ArgentinaCompreahoraCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {

      Document document = fetchDocument("https://www.compreahora.com.ar/catalogsearch/result/index/?p=2&q=shampoo");

      Elements items = document.select(".product-items li");

      for (Element element : items) {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".product-item-details .ulproduct-label", "data-product-id");
         String url = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".product-item-details .product-item-link", "href");


         saveDataProduct(internalId, internalId, url);

         this.log("position : "+this.position+" internalId : " + internalId + " internalPid : " + internalId + " url : " + url);

      }

   }
}
