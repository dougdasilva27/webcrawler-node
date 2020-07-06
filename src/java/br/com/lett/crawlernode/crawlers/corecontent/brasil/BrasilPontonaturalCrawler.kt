package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import org.jsoup.nodes.Document

class BrasilPontonaturalCrawler(session: Session) : Crawler(session) {

  override fun extractInformation(document: Document): MutableList<Product> {
    val products = mutableListOf<Product>()
    products += product {
      name = ".product-title h1"
      url = session.originalURL

      val descriptionElem = document.selectFirst(".tab-content.pr-tab-content")
      description = with(descriptionElem) {
        document.selectFirst("#short_description_content").html() +
            selectFirst("#descriptionTab").html() +
            selectFirst("#featuresTab").html()
      }
      primaryImage = document.selectFirst("#view_full_size a").attr("href")
      internalId = document.selectFirst("span[itemprop='sku']").attr("content")

      offer {
        useSlugNameAsInternalSellerId
        isMainRetailer
        sellerFullName = "Ponto Natural"

        pricing {
          spotlightPrice = document.selectFirst(".price_cash span").toDoubleComma()
          bankSlip = spotlightPrice?.toBankSlip()
          priceFrom = document.selectFirst("#old_price_display span").toDoubleComma()
          val pair = document.selectFirst(".price_in_installment").text().split("de")
          creditCards = listOf(Card.UNKNOWN_CARD)
            .toCreditCards(instPrice = pair[1].toDoubleComma(), instNumber = MathUtils.parseInt(pair[0]))
        }
      }
    }
    return products
  }
}
