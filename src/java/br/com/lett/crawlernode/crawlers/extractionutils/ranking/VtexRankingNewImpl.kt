package br.com.lett.crawlernode.crawlers.extractionutils.ranking

import br.com.lett.crawlernode.core.session.Session
import org.apache.http.impl.cookie.BasicClientCookie

class VtexRankingNewImpl(session: Session) : VtexRankingKeywordsNew(session) {

   override fun processBeforeFetch() {
      session.options?.optJSONObject("cookies")?.toMap()
         ?.forEach { (key: String?, value: Any) -> cookies.add(BasicClientCookie(key, value.toString())) }
   }
    override fun setHomePage(): String {
        return session.options.optString("homePage")
    }
}
