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
import java.text.Normalizer
import java.util.*
import java.util.regex.Pattern

val NONLATIN = Pattern.compile("[^\\w-]")
val WHITESPACE = Pattern.compile("[\\s]")
val EDGESDHASHES = Pattern.compile("(^-|-$)")

/**
 * @return jsoup [Document] instance
 */
fun String?.toDoc(): Document? = if (this != null) Jsoup.parse(this) else null

/**
 * @return list containing each [Elements]' text
 */
fun Elements.eachText(ignoreIndex: Array<Int> = arrayOf(), ignoreTokens: Array<String> = arrayOf()): List<String> {
  val list = mutableListOf<String>()
  forEachIndexed { index, element ->
    if ((index !in (ignoreIndex)) and (element.text() !in ignoreTokens))
      list.add(element.text())
  }
  return list
}

/**
 * Get each [Elements]' text by attribute, representing our secondary images data model.
 * @return json-like string
 */
fun Elements.toSecondaryImagesBy(attr: String = "href", ignoreIndex: Array<Int> = arrayOf()): String = JSONArray().also {
  this.forEachIndexed { index, element ->
    if (index !in ignoreIndex)
      it.put(element.attr(attr))
  }
}.toString()

/**
 * @return [JSONObject] in a cool way
 */
fun String?.toJson(): JSONObject = if (this != null)
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

/**
 * @return Parse [String] number with comma ',' to [Double]
 */
fun String?.toDoubleComma(): Double = MathUtils.parseDoubleWithComma(this)

/**
 * @return Parse [Element]'s text number with comma ',' to [Double]
 */
fun Element?.toDoubleComma(): Double = MathUtils.parseDoubleWithComma(this?.text())

/**
 * @return Parse [String] number with dot '.' to [Double]
 */
fun String?.toDoubleDot(): Double = MathUtils.parseDoubleWithDot(this)

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