package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColombiasurtiappbogotaCrawler extends CrawlerRankingKeywords {
   private int login = 0;
   List<String> proxies = Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY);

   @Override
   protected Document fetchDocument(String url) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(this.cookies)
         .setProxyservice(proxies)
         .build();
      String html =  this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(html);
   }


   public ColombiasurtiappbogotaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 25;
      this.log("Página " + this.currentPage);

      String url = "https://tienda.surtiapp.com.co/WithoutLoginB2B/Store/SearchResults/" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url, this.cookies);


      Elements products = this.currentDoc.select(".product-card.product-id-contaniner");
      if (!products.isEmpty()) {
         for (Element e : products) {
            String dataJson = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "#card-product","data-json");
            JSONObject data = dataJson != null && !dataJson.isEmpty() ? CrawlerUtils.stringToJson(dataJson) : null;
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "data-product");
            String productUrl = CrawlerUtils.completeUrl(internalId, "https", "tienda.surtiapp.com.co/Store/ProductDetail/");
            String name = CrawlerUtils.scrapStringSimpleInfo(e, ".product-card__name", true);
            String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product-card__image img", "src");
            Integer price = data.optInt("NewPrice");
            if (price == 0){
               price = data.optInt("Price");
            }
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(null)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

}
