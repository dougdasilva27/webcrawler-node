package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.util.CommonMethods
import org.json.JSONArray


val saveLog = true;
lateinit var logJson : JSONArray


fun main() {
   logJson = JSONArray()
   TestUtils.initialize()
   Test.pathWrite = "/home/bussolotti/crawler/htmls-crawler/"


   discovery()

//   coreRanking()



   CommonMethods.saveDataToAFile(logJson,Test.pathWrite+"/log.json")
}



private fun discovery() {

   val marketId: Long = 113

   val keywords = listOf(
      "celular"
   )

   LocalDiscovery().discovery(marketId, keywords, 2, 5)
}


private fun coreRanking() {

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
//   val currentTest = TestType.KEYWORDS

   val parameters = if (currentTest == TestType.KEYWORDS) keywords else urls

   TestUtils.taskProcess(city = city, marketName = marketName, marketId = marketId, parameters = parameters, currentTest = currentTest)
}
