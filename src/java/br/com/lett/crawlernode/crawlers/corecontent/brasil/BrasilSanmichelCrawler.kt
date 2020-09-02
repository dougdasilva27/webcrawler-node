package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card.*
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.product
import br.com.lett.crawlernode.util.toBankSlip
import br.com.lett.crawlernode.util.toCreditCards
import br.com.lett.crawlernode.util.toDoubleComma
import org.jsoup.nodes.Document

class BrasilSanmichelCrawler(session: Session) : Crawler(session) {
   override fun extractInformation(doc: Document): MutableList<Product> {
      val products = mutableListOf<Product>()

      products += product {
         name = doc.selectFirst(".title a").text()
         url = session.originalURL
         internalId = doc.selectFirst("input[name='id']").attr("value")
         primaryImage = "https://www.meusanmichel.com.br/${doc.selectFirst(".img-responsive").attr("src")}"
         description = doc.selectFirst("#description").text()
         offer {
            isMainRetailer
            sellerFullName = "San Michel"
            useSlugNameAsInternalSellerId
            pricing {
               val priceNullable = doc.selectFirst(".price-current").toDoubleComma()
               priceNullable?.let { price ->

                  spotlightPrice = price
                  priceFrom = doc.selectFirst(".price-prev").toDoubleComma()

                  bankSlip = price.toBankSlip()
                  creditCards = setOf(VISA, MASTERCARD, AMEX, ELO, DINERS, DISCOVER, HIPERCARD, AURA, JCB).toCreditCards(price)
               }
            }
         }
      }
      return products
   }
}
