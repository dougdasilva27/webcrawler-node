package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler
import br.com.lett.crawlernode.util.CommonMethods
import org.json.JSONObject

class BrasilNagumoCrawler(session: Session) : BrasilSitemercadoCrawler(session) {
    companion object {
        private const val HOME_PAGE = "https://www.nagumo.com.br/guarulhos-loja-guarulhos-aruja-jardim-cumbica-caminho-do-campo-do-rincao"
        private const val API_URL = "https://sitemercado-b2c-wl-www-api-production.azurewebsites.net/api/v1/b2c/product/loadSearch"
    }

    override fun getHomePage(): String {
        return HOME_PAGE
    }

    override fun getLoadPayload(): String {
        val payload = JSONObject()
        val split = HOME_PAGE.split("/".toRegex()).toTypedArray()
        payload.put("lojaUrl", CommonMethods.getLast(split))
        payload.put("redeUrl", split[split.size - 2])
        return payload.toString()
    }

    override fun getApiSearchUrl(): String {
        return API_URL
    }
}