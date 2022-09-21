package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class BrasilAlegrapetCrawler extends Crawler {
   protected BrasilAlegrapetCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {

         String internalId = crawlInternalId(document);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .build();
         products.add(product);
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst("") != null;
   }

   private String crawlInternalId(Document json) {
      String internalId = null;

      if (json.hasAttr("id_item")) {
         internalId = json.attributes().get("id_item");
      }

      return internalId;
   }

}
