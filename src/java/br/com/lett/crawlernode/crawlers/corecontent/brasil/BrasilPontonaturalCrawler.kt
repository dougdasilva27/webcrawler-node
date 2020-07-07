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
      name = document.selectFirst(".product-title h1").text()
      url = session.originalURL

      description = document.htmlOf(
        ".tab-content.pr-tab-content #descriptionTab",
        ".tab-content.pr-tab-content #featuresTab",
        "#short_description_content"
      )

      primaryImage = document.selectFirst("#view_full_size a").attr("href")
      internalId = document.selectFirst("span[itemprop='sku']").attr("content")

      offer {
        useSlugNameAsInternalSellerId
        isMainRetailer
        sellerFullName = "Ponto Natural"
        document.selectFirst(".price_cash span")?.toDoubleComma()?.let { price ->

          pricing {
            spotlightPrice = price
            bankSlip = spotlightPrice?.toBankSlip()
            priceFrom = document.selectFirst("#old_price_display span")?.toDoubleComma()
            val pair = document.selectFirst(".price_in_installment")?.text()?.split("de")

            pair?.get(1).toDoubleComma()?.let { number ->
              creditCards = listOf(Card.UNKNOWN_CARD)
                .toCreditCards(instPrice = number, instNumber = MathUtils.parseInt(pair?.get(0)))
            }
          }
        }
      }
    }
    return products
  }
}
