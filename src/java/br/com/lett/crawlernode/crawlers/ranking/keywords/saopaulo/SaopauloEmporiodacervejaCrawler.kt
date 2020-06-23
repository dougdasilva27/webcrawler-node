package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.fetcher.models.Request
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.CrawlerUtils
import br.com.lett.crawlernode.util.Logging
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

class SaopauloEmporiodacervejaCrawler(session: Session) : CrawlerRankingKeywords(session) {

    private val API_VERSION = 1
    private val SENDER = "vtex.search@0.x"
    private val PROVIDER = "vtex.search@0.x"

    private var keySHA256: String = "dcf550c27cd0bbf0e6899e3fa1f4b8c0b977330e321b9b8304cc23e2d2bad674"

    override fun extractProductsFromCurrentPage() {
        log("Página $currentPage")
        pageSize = 20

        if (currentPage == 1) {
            keySHA256 = fetchSHA256Key()
        }

        val searchApi = fetchSearchApi()
        val products = if (searchApi.opt("products") != null) searchApi.optJSONArray("products") else JSONArray()

        if (products.length() > 0) {
            if (totalProducts == 0) {

                setTotalProducts(searchApi)
            }
            for (item in products) {
                val product = item as JSONObject
                val productUrl = CrawlerUtils.completeUrl(product.optString("link"), "https", "www.emporiodacerveja.com.br").replace("portal.vtexcommercestable", "emporiodacerveja")
                val internalPid = product.optString("productId")
                saveDataProduct(null, internalPid, productUrl)
                log("Position: $position - InternalId: null - InternalPid: $internalPid - Url: $productUrl")
            }
        } else {
            result = false
            log("Keyword sem resultado!")
        }

        log("Finalizando Crawler de produtos da página " + currentPage + " - até agora " + arrayProducts.size + " produtos crawleados")
    }

    private fun setTotalProducts(data: JSONObject) {
        totalProducts = data.optInt("total")
        log("Total da busca: $totalProducts")
    }

    private fun fetchSearchApi(): JSONObject {
        var searchApi = JSONObject()
        val url = StringBuilder()
        url.append("https://www.emporiodacerveja.com.br/_v/segment/graphql/v1?")
        url.append("workspace=abtestbeermenu")
        url.append("&maxAge=short")
        url.append("&appsEtag=remove")
        url.append("&domain=store")
        url.append("&locale=pt-BR")
        url.append("&operationName=searchResult")

        // https://www.emporiodacerveja.com.br/_v/segment/graphql/v1?workspace=abtestbeermenu&maxAge=short&appsEtag=remove&domain=store&locale=pt-BR&operationName=searchResult&variables=%7B%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%22dcf550c27cd0bbf0e6899e3fa1f4b8c0b977330e321b9b8304cc23e2d2bad674%22%2C%22sender%22%3A%22vtex.search%400.x%22%2C%22provider%22%3A%22vtex.search%400.x%22%7D%2C%22variables%22%3A%22eyJwcm9kdWN0T3JpZ2luIjoiVlRFWCIsImluZGV4aW5nVHlwZSI6IkFQSSIsInF1ZXJ5IjoiY2VydmVqYSIsInBhZ2UiOjEsImF0dHJpYnV0ZVBhdGgiOiIiLCJzb3J0IjoiIiwiY291bnQiOjIwLCJsZWFwIjpmYWxzZX0%3D%22%7D

        val extensions = JSONObject()
        val persistedQuery = JSONObject()

        persistedQuery.put("version", API_VERSION)
        persistedQuery.put("sha256Hash", this.keySHA256)
        persistedQuery.put("sender", SENDER)
        persistedQuery.put("provider", PROVIDER)

        extensions.put("variables", createVariablesBase64())
        extensions.put("persistedQuery", persistedQuery)

        val payload = StringBuilder()
        try {
            payload.append("&variables=" + URLEncoder.encode("{}", "UTF-8"))
            payload.append("&extensions=" + URLEncoder.encode(extensions.toString(), "UTF-8"))
        } catch (e: UnsupportedEncodingException) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e))
        }
        url.append(payload.toString())

        log("Link onde são feitos os crawlers: $url")

        val request = Request.RequestBuilder.create()
                .setUrl(url.toString())
                .setCookies(cookies)
                .setPayload(payload.toString())
                .build()

        val response = CrawlerUtils.stringToJson(dataFetcher[session, request].body)
        if (response.has("data") && !response.isNull("data")) {
            val data = response.getJSONObject("data")
            if (data.has("searchResult") && !data.isNull("searchResult")) {
                searchApi = data.getJSONObject("searchResult")
            }
        }
        return searchApi
    }

    private fun createVariablesBase64(): String? {
        val search = JSONObject()
        search.put("productOrigin", "VTEX")
        search.put("indexingType", "API")
        search.put("query", keywordEncoded)
        search.put("page", currentPage)
        search.put("attributePath", "")
        search.put("sort", "")
        search.put("count", 20)
        search.put("leap", false)

        if (currentPage != 1) {
            val from: Int = pageSize * (currentPage - 1)
            search.put("from", from)
            search.put("to", from.plus(19))
        }
        return Base64.getEncoder().encodeToString(search.toString().toByteArray())
    }

    private fun fetchSHA256Key(): String {
        // When sha256Hash is not found, this key below works (on 05/06/2020)
        var hash = "dcf550c27cd0bbf0e6899e3fa1f4b8c0b977330e321b9b8304cc23e2d2bad674"
        // When script with hash is not found, we use this url
        var url: String? = "https://www.emporiodacerveja.com.br/_v/public/assets/v1/published/bundle/public/react/asset.min.js?v=1&files=vtex.search@0.6.4,0"
        val homePage = "https://www.emporiodacerveja.com.br"
        val requestHome = Request.RequestBuilder.create().setUrl(homePage).setCookies(cookies).mustSendContentEncoding(false).build()
        val doc = Jsoup.parse(dataFetcher[session, requestHome].body)
        val scripts = doc.select("body > script[crossorigin]")
        for (e in scripts) {
            val scriptUrl = CrawlerUtils.scrapUrl(e, null, "src", "https", "exitocol.vtexassets.com")
            if (scriptUrl.contains("vtex.search@")) {
                url = scriptUrl
                break
            }
        }
        val request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).mustSendContentEncoding(false).build()
        val response = dataFetcher[session, request].body.replace(" ", "")
        val searchProducts = CrawlerUtils.extractSpecificStringFromScript(response, "searchResult(", false, "',", false)
        val firstIndexString = "@runtimeMeta(hash:"
        if (searchProducts.contains(firstIndexString) && searchProducts.contains(")")) {
            val x = searchProducts.indexOf(firstIndexString) + firstIndexString.length
            val y = searchProducts.indexOf(')', x)
            hash = searchProducts.substring(x, y).replace("\"", "")
        }
        return hash
    }
}