package br.com.lett.crawlernode.crawlers.ranking.keywords.espana

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder
import br.com.lett.crawlernode.core.models.RankingProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.toJson
import org.json.JSONObject
import java.util.*

class EspanaCarrefourCrawler(session: Session) : CrawlerRankingKeywords(session) {
  var row = 0;

  override fun extractProductsFromCurrentPage() {
    val url = "https://www.carrefour.es/search-api/query/v1/search?query=$keywordEncoded&scope=desktop&lang=es&rows=${row + 24}&start=$row&origin=default&f.op=OR"

    val jsonBody = dataFetcher.get(session, RequestBuilder.create()
       .setUrl(url)
       .setProxyservice(Arrays.asList(
          ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
          ProxyCollection.NETNUT_RESIDENTIAL_ES
       ))
       .build()).body.toJson()
      .optJSONObject("content")
    if (row == 0) {
      totalProducts = jsonBody.optInt("numFound")
      pageSize = 24
    }

    for (jsonProduct in jsonBody.optJSONArray("docs")) {
      if (jsonProduct is JSONObject) {
  		   val productUrl =  "https://www.carrefour.es${jsonProduct.optString("url")}"
         val internalId = jsonProduct.optString("catalog_ref_id")
         val productName = jsonProduct.optString("display_name")
         val price = if (jsonProduct.optDouble("active_price") != 0.0) (jsonProduct.optDouble("active_price") * 100.0).toInt() else null
         val isAvailable = price != null

         val productRanking = RankingProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setName(productName)
            .setPriceInCents(price)
            .setAvailability(isAvailable)
            .build()

         saveDataProduct(productRanking)

        if (arrayProducts.size == productsLimit) {
            break;
         }
      }
    }
    row += 24
  }
}
