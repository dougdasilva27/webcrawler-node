package br.com.lett.crawlernode.crawlers.corecontent.belgium

import br.com.lett.crawlernode.core.session.Session

class BelgiumCarrefourdefaultCrawler(session: Session) : BelgiumCarrefourCrawler(session) {

   companion object {
      const val SHOP_ID = "D0615"
      const val CHOSEN_DELIVERY = "{\"type\":\"Home Delivery\",\"value\":{\"zipCode\":null,\"city\":null,\"id\":\"5000\"}}"
   }

   override fun getShopId(): String {
      return SHOP_ID
   }

   override fun getChosenDelivery(): String {
      return CHOSEN_DELIVERY
   }

}
