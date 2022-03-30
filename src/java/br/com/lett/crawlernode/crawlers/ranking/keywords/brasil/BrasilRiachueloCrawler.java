package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class BrasilRiachueloCrawler extends CrawlerRankingKeywords {

   public BrasilRiachueloCrawler(Session session) {
      super(session);
      pageSize = 24;
      fetchMode = FetchMode.APACHE;
   }

   protected JSONObject fetchJSONObject(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "*/*");
      headers.put("accept-encoding", "no");
      headers.put("connection", "keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setIgnoreStatusCode(false)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build()
         ).build();

      return JSONUtils.stringToJson(dataFetcher.get(session, request).getBody());

   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      String url = "https://recs.richrelevance.com/rrserver/api/find/v1/e20fd45b1e19a8c6?lang=pt&log=true&facetDepth=5&placement=search_page.find&query=" + keywordEncoded + "&rows=" + pageSize + "&start=" + (currentPage - 1) * pageSize;

      JSONObject jsonApi = fetchJSONObject(url);
      JSONObject placement = (JSONObject) jsonApi.optQuery("/placements/0");
      if (placement != null) {
         if (totalProducts == 0) {
            totalProducts = placement.optInt("numFound");
         }
         JSONArray jsonArray = placement.optJSONArray("docs");
         for (Object o : jsonArray) {
            this.position++;
            if (o instanceof JSONObject) {
               JSONObject elemJson = (JSONObject) o;
               JSONArray skus = elemJson.optJSONArray("sku_list");
               String urlProduct = "https://www.riachuelo.com.br/" + elemJson.optString("linkId");
               String internalPid = elemJson.optString("id");
               String name = elemJson.optString("name");
               Integer price = elemJson.optInt("priceCents");
               boolean isAvailable = elemJson.optBoolean("in_stock");
               String imageUrl = elemJson.optString("imageId");
               
               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(urlProduct)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .setPosition(position)
                  .build();

               saveDataProduct(productRanking);


            }


         }
      }
   }
      else

   {
      log("keyword sem resultados");
      result = false;
   }
}
}

