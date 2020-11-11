package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo

import br.com.lett.crawlernode.core.fetcher.FetchMode
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.CNOVACrawlerRanking

class SaopauloPontofrioCrawler(session: Session) : CNOVACrawlerRanking(session) {

   init {
      super.fetchMode = FetchMode.FETCHER
   }

   override fun getApiKey(): String {
      return "pontofrio"
   }
}
