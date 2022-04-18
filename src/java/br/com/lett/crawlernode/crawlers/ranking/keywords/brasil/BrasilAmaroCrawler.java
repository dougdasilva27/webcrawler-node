package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;

public class BrasilAmaroCrawler extends CrawlerRankingKeywords {
   public BrasilAmaroCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      this.pageSize = 24;
      this.log("Página " + this.currentPage);
      String url = "https://cerberus.amaro.pro/search/products?q="+this.keywordEncoded+"&sort=relevance&curr=BRL&lang=pt&currentPage="+this.currentPage+"&pageSize=24&f";
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Request request = Request.RequestBuilder.create()
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .setUrl(url)
         .build();
      Response response = new FetcherDataFetcher().get(session, request);
      JSONObject jsonObject = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONArray products = jsonObject.optJSONArray("products");

      if (products != null) {
         for (Object o : products) {
            JSONObject product = (JSONObject) o;
            String internalPid = product.optString("baseProduct");
            String internalId = product.optString("code");
            String productUrl = product.optString("url");
            String name = product.optString("name");

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setImageUrl(null)
               .setPriceInCents(null)
               .setAvailability(true)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

   }
}
