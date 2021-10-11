package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Offers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class MexicoCoppelCrawler extends Crawler {

   private static final String SELLER_NAME = "coppel";

   public MexicoCoppelCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected Document fetch() {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setProxyservice(Collections.singletonList(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();

      return Jsoup.parse(content);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
//      boolean isAvailable = price != 0;
      if (doc.selectFirst("#main_header_name") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
//         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"#product_SKU_1599029","value");//tornar pageId dinâmico
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#main_header_name", false);
//         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList());//product_longdescription_1599029
//         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "."); //[name=keywords]
//         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#image a #image-main", Arrays.asList("src"), "https", "padovani.vteximg.com.br");
//         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc,".thumbs li a img",Arrays.asList("src"),"https", "padovani.vteximg.com.br",   primaryImage);
         boolean available = doc.selectFirst("[class=available]") != null;
         Logging.printLogDebug(logger, session, "\nname   " + name);
         Logging.printLogDebug(logger, session, "\navailable   " + available);
//         Logging.printLogDebug(logger, session, "doc   " + doc);
//         Offers offers = available ? new Offers() : new Offers();

//         // Creating the product
//         Product product = ProductBuilder.create()
//            .setUrl(session.getOriginalURL())
//            .setInternalId(internalId)
//            .setInternalPid(internalId)
//            .setName(name)
//            .setOffers(offers)
//            .setCategory1(categories.getCategory(0))
//            .setCategory2(categories.getCategory(1))
//            .setCategory3(categories.getCategory(2))
//            .setPrimaryImage(primaryImage)
//            .setSecondaryImages(secondaryImages)
//            .setDescription(description)
//            .build();
//
//         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }
}
