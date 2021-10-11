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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

      if (doc.selectFirst("#main_header_name") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = getInternalId(doc); //TODO verificar formatação id PR-7257752
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#main_header_name", false);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product_longdescription_1599029")); //TODO: remover html tags e tornar pageId dinamico
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#productMainImage ", Arrays.asList("src"), "https", "");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc,"[class=mz-lens] img", Arrays.asList("src"),"https", "padovani.vteximg.com.br", primaryImage);
         CategoryCollection categories = getCategories(doc, "[name=keywords]");
         System.out.println("categories 0 " + categories.getCategory(0));
         System.out.println("categories 1 " + categories.getCategory(1));
         boolean available = doc.selectFirst("[class=available]") != null;

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

   public String getInternalId (Document doc) {
      String sku = CrawlerUtils.scrapStringSimpleInfo(doc, "#IntelligentOfferMainPartNumber", false);
      return sku;
   }

   public static CategoryCollection getCategories(Document doc, String selector) {
      CategoryCollection categories = new CategoryCollection();
      selector = selector.attr("content");
      Elements elementCategories = doc.select(selector);
      String categoriesString = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=keywords] ", "content");
      System.out.println("categoriesString " + categoriesString);

      for (String e : categoriesString) {
         System.out.println("e " + e);

         categories.add(e.replace(">", "").trim());
      }

      return categories;
   }
}
