package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Card
import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.models.ProductBuilder
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import br.com.lett.crawlernode.util.*
import models.Offer.OfferBuilder
import models.Offers
import models.pricing.Installment.InstallmentBuilder
import models.pricing.Installments
import models.pricing.Pricing.PricingBuilder
import org.jsoup.nodes.Document
import java.util.*

class BrasilLefarmaCrawler(session: Session?) : Crawler(session) {

   override fun extractInformation(document: Document): List<Product> {
      val products = mutableListOf<Product>()
      if (document.selectFirst(".nome-produto.titulo.cor-secundaria") != null) {

         val description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList("#descricao"))
         val name = CrawlerUtils.scrapStringSimpleInfo(document,".nome-produto.titulo",true)
         val internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"div[data-trustvox-product-code-js]","data-trustvox-product-code-js")
         val primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document,"#imagemProduto",Arrays.asList("src"),"https:","cdn.awsli.com.br")
         val available = document.selectFirst(".comprar a.botao.botao-comprar") != null
         val offers = if (available) scrapOffers(document) else Offers()
         val categories = document.select(".info-principal-produto ul a")?.eachText(ignoreIndexes = arrayOf(0))
         val secondaryImages = document.select(".miniaturas.slides a")?.toSecondaryImagesBy(attr = "data-imagem-grande", ignoreIndexes = arrayOf(0))
         val product = ProductBuilder.create()
            .setDescription(description)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .setUrl(session.originalURL)
         if (document.selectFirst(".atributo-item") != null) {
            for (elem in document.select(".atributo-item")) {
               products.add(
                  product.setInternalId(elem.attr("data-variacao-id"))
                     .setName("$name ${elem.selectFirst(".atributo-item span")?.text()?.trim() ?: ""}".trim()).build()
               )
            }
         } else {
            product.setInternalId(internalId)
               .setName(name)
               .build()
            products.add(product.build())
         }
      }

      return products
   }

   private fun scrapOffers(document: Document): Offers {
      val offers = Offers()
      val price = document.selectFirst(".preco-promocional.cor-principal")?.toDoubleComma()
      val priceFrom = document.selectFirst(".principal .preco-venda")?.toDoubleComma()

      val installments = Installments()
      if (document.selectFirst(".parcela") != null) {
         for (elem in document.select(".parcela")) {
            val instNumber = elem.selectFirst(".cor-principal")?.toInt()
            val instVal = elem.text()?.substringAfter("R$")?.toDoubleComma()
            installments.add(
               InstallmentBuilder.create()
                  .setInstallmentNumber(instNumber)
                  .setInstallmentPrice(instVal).build()
            )
         }
      } else {
         installments.add(
            InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(price).build()
         )
      }

      val bankSlip = document.selectFirst(".text-parcelas.pull-right.cor-principal")?.toDoubleComma()?.toBankSlip()

      val creditCards = setOf(Card.VISA, Card.MASTERCARD, Card.ELO, Card.AMEX, Card.HIPERCARD, Card.AURA).toCreditCards(installments)

      val pricing = PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build()

      offers.add(
         OfferBuilder.create()
            .setPricing(pricing)
            .setSellerFullName("Le Farma")
            .setUseSlugNameAsInternalSellerId(true)
            .setIsMainRetailer(true)
            .setIsBuybox(false)
            .build()
      )

      return offers
   }
}
