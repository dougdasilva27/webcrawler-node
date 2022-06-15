package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrasilTudoEquipaCrawler extends CrawlerRankingKeywords {
   public BrasilTudoEquipaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      String url ="https://www.tudoequipa.com.br/catalogsearch/result/index/?cat=0&p=" + this.currentPage + "&q=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetch(url);
      Elements products = this.currentDoc.select(".products-grid.products-grid--max-4-col.first.last.odd");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }
         for (Element e : products) {

         }
      }


   }

   protected Document fetch(String url) {
      Map<String, String> headers = new HashMap<>();

      headers.put("Accept","*/*");
      headers.put("Accept-Encoding","gzip, deflate, br");
      headers.put("Connection","keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();

      return Jsoup.parse(content);
   }
}
