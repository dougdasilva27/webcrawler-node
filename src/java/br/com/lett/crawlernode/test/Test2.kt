package br.com.lett.crawlernode.test

enum class TestType {
   KEYWORDS, INSIGHTS
}

fun main() {

   // if city is empty, fetch by marketId
   // fill city and market or marketId
   val city = ""
   val marketName = ""

   val marketId: Long = 440 //martins

   val urls = listOf(
      ""
   )
   val keywords = listOf(
      ""
   )

   val currentTest = TestType.INSIGHTS

   TestUtils.initialize()

   val parameters = if (currentTest == TestType.KEYWORDS) keywords else urls

   TestUtils.taskProcess(city = city, marketName = marketName, marketId = marketId, parameters = parameters, currentTest = currentTest)
}
