package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.toInt
import org.json.JSONObject
import java.net.URLDecoder


/**
 * Date: 22/07/20
 *
 * @author Fellype Layunne
 *
 */
class BrasilRennerCrawler(session: Session) : CrawlerRankingKeywords(session) {

    init {
        pageSize = 40
    }

    override fun extractProductsFromCurrentPage() {

        val json = requestApi()

        if (this.totalProducts == 0) {
            setTotalProducts(json)
        }

        val elements = json.optJSONArray("docs")

        if (elements.isEmpty) {
            result = false
            return
        }

        for (elem in elements) {
            val element = (elem as JSONObject)
            val internalId = element.optJSONArray("sku_list").optString(0)
            val internalPid = element.optString("parent_product_id")

            val productUrl = crawlProductUrl(element)
            saveDataProduct(internalId, internalPid, productUrl)
            if (arrayProducts.size == productsLimit) {
                break
            }
        }
    }

    private fun crawlProductUrl(element: JSONObject): String {
        return URLDecoder.decode( element.optString("clickUrl").substringAfter("&ct=").replace("&redirect=true", ""), "UTF-8" );
    }

    private fun requestApi() :JSONObject {
        val url = "https://recs.richrelevance.com/rrserver/api/find/v1/24f07d816ef94d7f" +
                "?log=true&lang=pt&placement=search_page.find&ssl=true&mm=3%3C90%25" +
                "&query=$keywordEncoded" +
                "&rows=$pageSize" +
                "&start=${(currentPage-1)*pageSize}"

        val headers: MutableMap<String, String> = HashMap()
        headers["Content-Type"] = "application/json"

        val request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build()

        val json = CrawlerUtils.stringToJson(dataFetcher[session, request].body)

        return json.optJSONArray("placements").optJSONObject(0)
    }

    fun setTotalProducts(search: JSONObject) {

        val total = search.opt("numFound")

        if (total is Int) {
            totalProducts = total
        }
    }
}

