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

            //https://recs.richrelevance.com/rrserver/api/find/v1/track/click/24f07d816ef94d7f?a=24f07d816ef94d7f&vg=e2274951-d32c-40fe-1f07-b05cde1ed998&pti=2&pa=find&hpi=0&stn=PersonalizedProductSearchAndBrowse&stid=184&rti=2&sgs=&mvtId=-1&mvtTs=1595454291362&uguid=686c2819-ebed-4011-a8c7-075cce5a105c&channelId=WEB&s=221281996248874661&pg=-1&page=1&query=camisa&lang=pt&searchConfigId=5e2ee0d2d0a97c000f1a6ad4&searchType=CATALOG&p=552061961-COR552061961-16-3919TCX&ind=3&ct=https%3A%2F%2Fwww.lojasrenner.com.br%2Fp%2Fcamisa-manga-curta-comfort-em-oxford%2F-%2FA-552061961-br.lr%3Fsku%3D552062154&redirect=true
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

    override fun setTotalProducts() {
        totalProducts = currentDoc.selectFirst(".heading-counter").toInt() ?: 0
    }
}

