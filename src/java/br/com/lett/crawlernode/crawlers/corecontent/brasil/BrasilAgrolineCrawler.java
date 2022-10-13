package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class BrasilAgrolineCrawler extends Crawler {

   private static String SELLER_NAME = "Agroline";

   public BrasilAgrolineCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(document,".novaColunaEstoque > .fbits-produto-nome.prodTitle",true);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,".content.produto > div > #hdnProdutoVarianteId","value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"[property=\"product:retailer_item_id\"]","content");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"[property=\"og:image\"]","content");


         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(productName)
            //.setPrimaryImage(primaryImage)
            //.setSecondaryImages(sec)
            //.setDescription(description)
            //.setCategories(categories)
            //.setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".blocoEstoquePrincipal") != null;
   }
}
