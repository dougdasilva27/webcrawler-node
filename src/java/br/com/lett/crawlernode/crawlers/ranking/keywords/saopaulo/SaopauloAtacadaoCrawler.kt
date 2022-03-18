package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.AtacadaoCrawlerRanking
import org.apache.http.cookie.Cookie
import org.apache.http.impl.cookie.BasicClientCookie


class SaopauloAtacadaoCrawler(session: Session) : AtacadaoCrawlerRanking(session) {

   companion object {
      const val CITY_ID: String = br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloAtacadaoCrawler.CITY_ID
   }

   override fun setCookies(): List<Cookie> {
      this.cookies.add(BasicClientCookie("cb_user_type", "CPF"))
      this.cookies.add(BasicClientCookie("cb_user_city_id", CITY_ID))

      return this.cookies;
   }

}
