package br.com.lett.crawlernode.test

fun main() {

   TestUtils.initialize()

   discovery()
//   coreRanking()

}

private fun discovery() {

   val marketId: Long = 440

   val keywords = listOf(
      ""
   )

   LocalDiscovery().discovery(marketId, keywords, 100, 5)
}


private fun coreRanking() {

   // if city is empty, fetch by marketId
   // fill city and market or marketId
   val city = ""
   val marketName = ""

   val marketId: Long = 440 //martins

   val urls = listOf(
      "",
   )
   val keywords = listOf(
      ""
   )

   val currentTest = TestType.INSIGHTS
//   val currentTest = TestType.KEYWORDS

   val parameters = if (currentTest == TestType.KEYWORDS) keywords else urls

   TestUtils.taskProcess(city = city, marketName = marketName, marketId = marketId, parameters = parameters, currentTest = currentTest)
}
