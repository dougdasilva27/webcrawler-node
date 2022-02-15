package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.avro.data.Json;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class BrasilDrogaoSuperCrawler extends CrawlerRankingKeywords {
   private String HOME_PAGE = "https://www.drogaosuper.com.br/";
   private Integer pageSize = 18;
   public BrasilDrogaoSuperCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url = HOME_PAGE + "busca.asp?PalavraChave=" + this.keywordEncoded + "&viewType=M&nrRows=" + pageSize + "&idPage=" + this.currentPage + "&ordem=V";
      this.currentDoc = fetchDocument(url);
      Elements products = this.currentDoc.select(".miolo.small-12.large-9 .products-list .grid-x .product-list-container");
      if (!products.isEmpty()) {
         if (totalProducts == 0) {
            setTotalProducts();
         }
         for (Element e : products) {
            String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".foto a ", "href");
            String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".foto a ", "title");
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, ".produto", Arrays.asList("src"), "", "");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, ".precoPor ", null, false, ',', session, 0);
            String internalId = scrapInternalId(e);
            boolean isAvailable = scrapAvailable(e);
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }
   private String scrapInternalId(Element e) {
      String stringJson = CrawlerUtils.scrapStringSimpleInfoByAttribute(e,".itemGtm >input", "value");
      JSONObject json = CrawlerUtils.stringToJson(stringJson);
      return json.optString("id");
   }
   private boolean scrapAvailable(Element e) {
      String available= CrawlerUtils.scrapStringSimpleInfo(e,".esgotado", false);

      return available==null;
   }


   @Override
   protected void setTotalProducts(){
      Integer count = 0;
      String next;
      Document doc;
      do {
         String url = HOME_PAGE + "busca.asp?PalavraChave=" + this.keywordEncoded + "&viewType=M&nrRows=" + 60 + "&idPage=" + count + "&ordem=V";
         doc = fetchDocument(url);
         next = CrawlerUtils.scrapStringSimpleInfo(doc,".next.inactive", false);
         count++;
      }while (next == null);
      Elements products = doc.select(".miolo.small-12.large-9 .products-list .grid-x .product-list-container");
      count = count >= 2 ? count - 2 : 0;
      this.totalProducts = products.size() + count *60;
      this.log("Total: " + this.totalProducts);
   }
}
