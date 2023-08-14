package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;

public class ChileSodimacBosch extends FalabellaCrawler {

   public ChileSodimacBosch(Session session) {
      super(session);
   }

   @Override
   protected String crawlBrandName(Document doc) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name", true);
      String brand = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[data-brand]", "data-brand");
      String model = CrawlerUtils.scrapStringSimpleInfo(doc, "#productInfoContainer tr:contains(Modelo)", false);

      String fullName = name != null && brand != null ? brand + " " + name : null;

      if (fullName == null) {
         return null;
      }

      return model != null ? fullName + " " + model : fullName;
   }
}
