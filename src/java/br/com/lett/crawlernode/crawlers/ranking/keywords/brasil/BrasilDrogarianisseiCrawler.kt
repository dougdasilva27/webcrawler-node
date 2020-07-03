package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import org.json.JSONObject

class BrasilDrogarianisseiCrawler(session: Session?) : CrawlerRankingKeywords(session) {

  val apiToken: String by lazy {
    currentDoc = fetchDocument("https://www.farmaciasnissei.com.br/searchanise/result?q=$keywordEncoded")
    CrawlerUtils.selectJsonFromHtml(currentDoc, "script", "window.Searchanise=", "};", true, false)
      .optString("api_key")
  }

  init {
    pageSize = 20
  }

  override fun extractProductsFromCurrentPage() {
    val url = "https://www.searchanise.com/getresults?api_key=$apiToken" +
        "&q=$keywordEncoded" +
        "&sortBy=relevance" +
        "&sortOrder=desc" +
        "&restrictBy%5Bstatus%5D=1" +
        "&restrictBy%5Bvisibility%5D=3%7C4" +
        "&startIndex=${arrayProducts.size}" +
        "&maxResults=30" +
        "&items=true" +
        "&pages=true" +
        "&categories=false" +
        "&suggestions=false" +
        "&queryCorrection=true" +
        "&suggestionsMaxResults=3" +
        "&pageStartIndex=0" +
        "&pagesMaxResults=20" +
        "&categoryStartIndex=0" +
        "&categoriesMaxResults=20" +
        "&facets=false" +
        "&facetsShowUnavailableOptions=false" +
        "&ResultsTitleStrings=2" +
        "&ResultsDescriptionStrings=0" +
        "&output=jsonp"
    val json = fetchJSONObject(url)
    if (this.arrayProducts.size == 0) {
      totalProducts = json.optInt("totalItems")
    }
    pageSize = json.optInt("itemsPerPage")
    val productsArray = json.optJSONArray("items")

    for (productJson in productsArray) {
      if (productJson is JSONObject) {
        val internalId = productJson.optString("product_id")
        val productUrl = productJson.optString("link")
        saveDataProduct(internalId, null, productUrl)
        log("InternalId: $internalId - Url: $productUrl")
      }
    }
    log("Pag $currentPage - ${arrayProducts.size} produtos crawleados")
  }
}
