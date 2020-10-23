package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.session.Session

class BelgiumCarrefourbruxellesCrawler(session: Session) : BelgiumCarrefourCrawler(session) {

   companion object {
      const val SHOP_ID = "5000"
      const val CHOSEN_DELIVERY = "{\"type\":\"Home Delivery\",\"value\":{\"zipCode\":\"1082\",\"city\":\"Bruxelles\",\"id\":\"5000\"}}"
   }

   override fun getShopId(): String {
      return SHOP_ID
   }

   override fun getChosenDelivery(): String {
      return CHOSEN_DELIVERY
   }

}
