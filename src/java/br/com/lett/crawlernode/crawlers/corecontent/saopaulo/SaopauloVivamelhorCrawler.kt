package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import org.jsoup.nodes.Document

class SaopauloVivamelhorCrawler(session: Session) : Crawler(session) {
  override fun extractInformation(doc: Document): MutableList<Product> {
    val products = mutableListOf<Product>()
    if (doc.select(".liveupdate-old-price") != null) {
      products += product {
        url = session.originalURL
        name = doc.selectFirst(".product-info-item h1").text()
        internalId = doc.selectFirst("input[name='product_id']").attr("value")
        primaryImage = doc.selectFirst(".product-image-zoom").attr("src")
        categories = doc.select(".breadcrumb a").eachText(ignoreIndexes = arrayOf(0))
        offer {
          useSlugNameAsInternalSellerId
          isMainRetailer
          sellerFullName = "Loja Viva Melhor"
          val price = doc.selectFirst(".liveupdate-old-price").toDoubleComma()!!
          pricing {
            spotlightPrice = price
            creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX, Card.DINERS, Card.MAESTRO).toCreditCards(price)
            bankSlip = price.toBankSlip()
          }
        }
      }
    }
    return products
  }
}
