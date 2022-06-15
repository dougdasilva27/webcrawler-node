package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Offers;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrasilTudoEquipaCrawler extends Crawler {
   public BrasilTudoEquipaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception{
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (doc.selectFirst(".page-title") == null) {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name span[itemprop=name]", false);
//         this.pageId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=pageId]", "content");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".tab-content .std", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-image.visible ", Arrays.asList("src"), "https", "");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc,".thumb-link img", Arrays.asList("src"),"https", "", primaryImage);
//         boolean available = doc.selectFirst("p .available") != null;
//         Offers offers = available ? scrapOffers(doc) : new Offers();
         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
//            .setInternalId(internalId)
//            .setInternalPid(internalId)
//            .setName(name)
//            .setOffers(offers)
//            .setPrimaryImage(primaryImage)
//            .setSecondaryImages(secondaryImages)
//            .setDescription(description)
            .build();
         products.add(product);

      }else {
            Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
         }
      return products;
   }

}
