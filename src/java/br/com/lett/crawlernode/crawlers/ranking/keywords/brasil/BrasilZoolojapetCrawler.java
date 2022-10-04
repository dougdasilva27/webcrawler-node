package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrasilZoolojapetCrawler extends CrawlerRankingKeywords {
   public BrasilZoolojapetCrawler(Session session) {
      super(session);
   }
   Integer pageSize = 36;
      @Override
      protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
         JSONObject json = fetchJSONObject("https://m2.zoolojapet.com.br/graphql/V1/products");

      }
   @Override
   protected JSONObject fetchJSONObject(String url) {
      String payload = "{\"query\":\"\\n        query($page: Int!, $pageSize: Int!) {\\n          products(\\n            search: \\\""+this.keywordEncoded+"\\\"\\n            currentPage: $page\\n            pageSize: $pageSize\\n            sort: { relevance: DESC }\\n            \\n          ) {\\n            \\n  items {\\n    id\\n    name\\n    sku\\n    stock_status\\n    url_key\\n    url_suffix\\n    recorrente\\n    price_range {\\n      minimum_price {\\n        regular_price { value }\\n        final_price { value }\\n        discount { amount_off, percent_off }\\n      }\\n    }\\n    thumbnail { url, label }\\n  }\\n  total_count\\n  page_info { current_page, total_pages }\\n\\n            aggregations {\\n              attribute_code\\n              count\\n              label\\n              options { count, label, value }\\n            }\\n          }\\n        }\\n      \",\"variables\":{\"page\":"+this.currentPage+",\"pageSize\":"+this.pageSize+"}}";
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://zoolojapet.com.br/");
      headers.put("referer", "https://zoolojapet.com.br/");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      Response response = this.dataFetcher.get(session, request);
      return JSONUtils.stringToJson(response.getBody());
   }


}
