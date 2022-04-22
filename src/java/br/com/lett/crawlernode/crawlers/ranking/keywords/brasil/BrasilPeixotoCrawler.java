package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BrasilPeixotoCrawler extends CrawlerRankingKeywords {
   public BrasilPeixotoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String url = "https://www.peixoto.com.br/consulta/?q=" + this.keywordEncoded + "&page=" + this.currentPage;
      this.currentDoc = fetch(url);

      Elements products = this.currentDoc.select(".product_item.logged");
      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            this.totalProducts = products.size();
         }

         for (Element e : products) {
            String productUrl = "https://www.peixoto.com.br/" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product_image", "href");
            Integer id = CrawlerUtils.scrapSimpleInteger(e, ".product_name> strong", false);
            String internalId = id != null ? String.valueOf(id) : null;
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product_name> span", false);
            String imgUrl = CrawlerUtils.scrapSimplePrimaryImage(e, "a.product_image img", Arrays.asList("src"), "https", "www.peixoto.com.br");
            Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "> div > h3", null, false, ',', session, null);

            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setName(name)
               .setImageUrl(imgUrl)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }

   protected Document fetch(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "ASP.NET_SessionId=js0eqm1oyenefe0ch4vj0q3v; b2bfilfatexp=003RR02-2ED026|58; b2bfilfatexplist=58,59; b2blog=true%230%23BAR+DO+PORTUGUES%23%23204743%2332%230%23-1%23%230%230%2c38%237100624%23%230%230%23; language=pt-BR; loja_id=32");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(false)
         .build();

      Response a = this.dataFetcher.get(session, request);

      String content = a.getBody();

      return Jsoup.parse(content);
   }

   @Override
   protected void processBeforeFetch() {

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.peixoto.com.br/User/Login")
         .setPayload("password=BAR1824&domain_id=167&email=40374650000111")
         .build();
      Response responseApi = new JsoupDataFetcher().post(session, request);
      this.cookies.addAll(responseApi.getCookies());
   }
}
