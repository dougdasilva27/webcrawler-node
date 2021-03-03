package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.corecontent.araraquara.AraraquaraSitemercadosupermercados14Crawler
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler
import br.com.lett.crawlernode.util.CommonMethods
import org.json.JSONObject

class BrasilNagumoCrawler(session: Session) : BrasilSitemercadoCrawler(session) {

    companion object {
        private const val HOME_PAGE = "https://www.sitemercado.com.br/nagumo/lorena-lj48-lorena-vila-hepacare-avenida-coronel-jose-vicente"

        private const val IDLOJA = 2700
        private const val IDREDE = 884
    }

    override fun getHomePage(): String {
        return HOME_PAGE
    }

    override fun getLoadPayload(): String? {
        val payload = JSONObject()
        val split = AraraquaraSitemercadosupermercados14Crawler.HOME_PAGE.split("/".toRegex()).toTypedArray()
        payload.put("lojaUrl", CommonMethods.getLast(split))
        payload.put("redeUrl", split[split.size - 2])
        return payload.toString()
    }

    override fun getLojaInfo(): Map<String, Int>? {
        val lojaInfo: MutableMap<String, Int> = HashMap()
        lojaInfo["IdLoja"] = IDLOJA
        lojaInfo["IdRede"] = IDREDE
        return lojaInfo
    }


}
