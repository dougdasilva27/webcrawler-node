@file:Suppress("unused")

package br.com.lett.crawlernode.util

import br.com.lett.crawlernode.core.models.Card
import models.pricing.*
import models.pricing.BankSlip.BankSlipBuilder
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.Normalizer
import java.util.*
import java.util.regex.Pattern
import kotlin.math.round

val NONLATIN: Pattern by lazy { Pattern.compile("[^\\w-]") }
val WHITESPACE: Pattern by lazy { Pattern.compile("[\\s]") }
val EDGESDHASHES: Pattern by lazy { Pattern.compile("(^-|-$)") }

/**
 * @return jsoup [Document] instance
 */
fun String?.toDoc(): Document? = if (this != null) Jsoup.parse(this) else null

/**
 * @return list containing each [Elements]' text
 */
fun Elements.eachText(ignoreIndexes: Array<Int> = arrayOf(), ignoreTokens: Array<String> = arrayOf()): List<String> {
   val list = mutableListOf<String>()
   forEachIndexed { index, element ->
      if ((index !in (ignoreIndexes)) and (element.text() !in ignoreTokens))
         list.add(element.text())
   }
   return list
}

/**
 * Get each [Elements]' text by attribute, representing our secondary images data model.
 * @return json-like string
 */
fun Elements.toSecondaryImagesBy(attr: String = "href", ignoreIndexes: Array<Int> = arrayOf()): String = JSONArray().also {
   this.eachAttr(attr, ignoreIndexes).forEach { attr -> it.put(attr) }
}.toString()

fun Elements.eachAttr(attr: String, ignoreIndexes: Array<Int> = arrayOf()): List<String> {
   val attrs = mutableListOf<String>()
   this.forEachIndexed { index, element ->
      if (index !in ignoreIndexes)
         attrs.add(element.attr(attr))
   }
   return attrs
}

/**
 * @return [JSONObject] in a cool way
 */
fun String?.toJson(): JSONObject = if (this != null && this.isNotEmpty())
   JSONObject(this)
else JSONObject()

/**
 * @return [JSONArray] in a cool way
 */
fun String?.toJsonArray(): JSONArray = if (this != null)
   JSONArray(this)
else JSONArray()

/**
 * @return [CreditCards] by a [Card] collection
 */
fun Collection<Card>.toCreditCards(instPrice: Double, instNumber: Int = 1): CreditCards = CreditCards(this.map { card: Card ->
   CreditCard.CreditCardBuilder.create()
      .setBrand(card.toString())
      .setIsShopCard(false)
      .setInstallments(
         Installments(
            setOf(
               Installment.InstallmentBuilder.create()
                  .setInstallmentPrice(instPrice)
                  .setInstallmentNumber(instNumber)
                  .build()
            )
         )
      )
      .build()
})

fun Collection<Card>.toCreditCards(installments: Installments): CreditCards = CreditCards(this.map { card: Card ->
   CreditCard.CreditCardBuilder.create()
      .setBrand(card.toString())
      .setIsShopCard(false)
      .setInstallments(installments)
      .build()
})

/**
 * @return Parse [String] number with comma ',' to [Double]
 */
fun String?.toDoubleComma(): Double? {
   val doubleText = this?.replace("[^0-9,]+".toRegex(), "")?.replace(".", "")?.replace(",", ".")
   return if (!doubleText.isNullOrEmpty())
      doubleText.toDouble()
   else null
}

/**
 * @return Parse [Element]'s text number with comma ',' to [Double]
 */
fun Element?.toDoubleComma(): Double? = this?.text()?.toDoubleComma()

fun Element?.toInt(): Int? {
   return this?.text()?.int()
}

fun String?.int(): Int? {
   val text = this?.replace("[^0-9]".toRegex(), "")?.trim()
   return if (!text.isNullOrEmpty())
      text.toInt()
   else null
}

/**
 * @return Parse [String] number with dot '.' to [Double]
 */
fun String?.toDoubleDot(): Double? {
   val doubleText = this?.replace("[^0-9.]+".toRegex(), "")
   return if (!doubleText.isNullOrEmpty())
      doubleText.toDouble()
   else null
}

/**
 * @return Parse [Element]'s text number with dot '.' to [Double]
 */
fun Element?.toDoubleDot(): Double = MathUtils.parseDoubleWithDot(this?.text())

/**
 * @return Pattern slug [String]
 */
fun toSlug(input: String): String {
   val nowhitespace = WHITESPACE.matcher(input).replaceAll("-")
   val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
   var slug = NONLATIN.matcher(normalized).replaceAll("")
   slug = EDGESDHASHES.matcher(slug).replaceAll("")
   return slug.toLowerCase(Locale.ENGLISH)
}

/**
 * It tends to void null, but depends on selectors
 */
fun Document.selectAny(vararg selectors: String): Element? {
   var found: Element? = null
   for (selector in selectors) {
      val element = selectFirst(selector)
      if (element != null) {
         found = element
      }
   }
   return found
}

fun Document.htmlOf(vararg selectors: String): String = buildString {
   for (selector in selectors) {
      val element = selectFirst(selector)
      if (element != null && !element.html().isNullOrEmpty()) {
         append(element.html())
      }
   }
}

fun Double.toBankSlip(discount: Double? = null): BankSlip = BankSlipBuilder.create().setFinalPrice(this).setOnPageDiscount(discount).build()

infix fun <T> MutableCollection<T>.addNonNull(elem: T?) {
   elem?.let { this.plusAssign(elem) }
}

// fix floating point
fun Double.round(decimals: Int = 2): Double {
   var multiplier = 1.0
   repeat(decimals) { multiplier *= 10 }
   return round(this * multiplier) / multiplier
}

// fix floating point
fun Float.round(decimals: Int = 2): Float {
   var multiplier = 1.0
   repeat(decimals) { multiplier *= 10 }
   return (round(this * multiplier) / multiplier).toFloat()
}

fun <E> List<E>.sliceFirst(): List<E> = this.slice(1 until size)

fun Element.installment(selector: String, itOwnText: Boolean = false): Installment {
   val inst: Pair<Int, Float> = CrawlerUtils.crawlSimpleInstallment(selector, this, itOwnText)
   return Installment.InstallmentBuilder
      .create()
      .setInstallmentPrice(inst.second?.toDouble()?.round())
      .setInstallmentNumber(inst.first)
      .build()
}

fun kotlin.Pair<Int, Double?>.installment(): Installment {
   return Installment.InstallmentBuilder
      .create()
      .setInstallmentPrice(second?.round())
      .setInstallmentNumber(first)
      .build()
}
