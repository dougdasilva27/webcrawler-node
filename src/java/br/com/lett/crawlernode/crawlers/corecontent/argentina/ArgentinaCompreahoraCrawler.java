package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.Offers;
import org.json.JSONArray;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArgentinaCompreahoraCrawler extends Crawler {
   public ArgentinaCompreahoraCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,".pdp-item","data-product-id");
      String internalPid = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(document,".base",true);
      JSONArray imagesArray = CrawlerUtils.crawlArrayImagesFromScriptMagento(document);
      String primaryImage = CrawlerUtils.scrapPrimaryImageMagento(imagesArray);
      String secondaryImages = CrawlerUtils.scrapSecondaryImagesMagento(imagesArray,primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList(".data.item.content .value",".data.item.content .additional-composition-wrapper"));

      products.add(ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setPrimaryImage(primaryImage)
         .setSecondaryImages(secondaryImages)
         .setDescription(description)
         .setOffers(new Offers())
         .build());

      return products;
   }
}
