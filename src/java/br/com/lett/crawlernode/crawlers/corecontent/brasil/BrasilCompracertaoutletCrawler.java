package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;

public class BrasilCompracertaoutletCrawler extends BrasilCompracertaCrawler {

   public BrasilCompracertaoutletCrawler(Session session) {
      super(session);
   }

   @Override
   protected boolean isProductPage(Document doc) {
      String producReference = crawlProductReference(doc).toLowerCase();
      return !doc.select(".productName").isEmpty() && producReference.endsWith("_out");
   }
}
