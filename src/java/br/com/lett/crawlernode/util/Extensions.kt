package br.com.lett.crawlernode.util

import br.com.lett.crawlernode.core.models.Card
import models.pricing.CreditCard
import models.pricing.CreditCards
import models.pricing.Installment
import models.pricing.Installments
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

fun String.toDoc(): Document = Jsoup.parse(this)

fun Elements.toCategories(ignoreIndex: Array<Int> = arrayOf(), ignoreTokens: Array<String> = arrayOf()): Set<String> {
    val set = mutableSetOf<String>()
    forEachIndexed { index, element ->
        if ((index !in (ignoreIndex)) and (element.text() !in ignoreTokens))
            set.add(element.text())
    }
    return set
}

fun Elements.toSecondaryImagesBy(attr: String = "href", ignoreIndex: Array<Int> = arrayOf()): String = JSONArray().also {
    this.forEachIndexed { index, element ->
        if (index !in ignoreIndex)
            it.put(element.attr(attr))
    }
}.toString()

fun String.toJson(): JSONObject = JSONObject(this)

fun String?.toJsonArray() = if (this != null)
    JSONArray(this)
else JSONArray()

fun Collection<Card>.toCreditCards(instPrice: Double, instNumber: Int = 1): CreditCards = CreditCards(this.map { card: Card ->
    CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setIsShopCard(false)
            .setInstallments(Installments(setOf(Installment.InstallmentBuilder.create()
                    .setInstallmentPrice(instPrice)
                    .setInstallmentNumber(instNumber)
                    .build())))
            .build()
})

fun String.toDoubleComma(): Double = MathUtils.parseDoubleWithComma(this)
fun Element.toDoubleComma(): Double = MathUtils.parseDoubleWithComma(this.text())

fun String.toDoubleDot(): Double = MathUtils.parseDoubleWithDot(this)
fun Element.toDoubleDot(): Double = MathUtils.parseDoubleWithDot(this.text())
