package br.com.lett.crawlernode.crawlers.ranking.keywords.belgium

import br.com.lett.crawlernode.core.session.Session

class BelgiumCarrefourdefaultCrawler(session: Session) : BelgiumCarrefourCrawler(session) {

   companion object {
      const val SHOP_ID = br.com.lett.crawlernode.crawlers.corecontent.belgium.BelgiumCarrefourdefaultCrawler.SHOP_ID
      const val CHOSEN_DELIVERY = br.com.lett.crawlernode.crawlers.corecontent.belgium.BelgiumCarrefourdefaultCrawler.CHOSEN_DELIVERY
   }

   override fun getShopId(): String {
      return SHOP_ID
   }

   override fun getChosenDelivery(): String {
      return CHOSEN_DELIVERY
   }

}
