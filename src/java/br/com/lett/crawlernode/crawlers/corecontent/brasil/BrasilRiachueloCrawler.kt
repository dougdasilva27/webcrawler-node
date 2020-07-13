package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.pricing.Installment
import models.pricing.Installments
import org.jsoup.nodes.Document

class BrasilRiachueloCrawler(session: Session) : Crawler(session) {

  override fun extractInformation(doc: Document): MutableList<Product> {
    val products = mutableListOf<Product>()
    products += product {
      internalId = doc.selectFirst(".price-box").attr("data-product-id")

      val images = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc)
      primaryImage = images.remove(0)?.toString()
      secondaryImages = images.toString()

      offer {
        isMainRetailer
        useSlugNameAsInternalSellerId
        pricing {
          val price = doc.selectFirst("span[data-price-type='finalPrice']")?.attr("data-price-amount")?.toDoubleComma()!!
          spotlightPrice = price
          priceFrom = doc.selectFirst("span[data-price-type='oldPrice']")?.attr("data-price-amount")?.toDoubleComma()
          bankSlip = spotlightPrice?.toBankSlip()
          creditCards = listOf(Card.VISA, Card.MASTERCARD, Card.AMEX, Card.DINERS, Card.ELO).toCreditCards(scrapInstallments(doc, price))
        }
      }
    }
    return products
  }

  fun scrapInstallments(doc: Document, spotlightPrice: Double): Installments {

    val installments = mutableSetOf<Installment>()
    installments += Installment.InstallmentBuilder.create()
      .setInstallmentPrice(spotlightPrice)
      .setInstallmentNumber(1)
      .build()

    doc.select(".installement-info p")?.forEach { instElem ->
      val pair = CrawlerUtils.crawlSimpleInstallmentFromString(instElem.text(), "de", "no", false)
      if (!pair.isAnyValueNull) {
        installments += Installment.InstallmentBuilder.create()
          .setInstallmentPrice(pair.second.toDouble())
          .setInstallmentNumber(pair.first)
          .build()
      }
    }
    return Installments(installments)
  }
}
