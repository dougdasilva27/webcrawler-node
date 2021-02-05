package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.models.RankingProducts
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking
import br.com.lett.crawlernode.util.CommonMethods
import org.json.JSONArray
import org.json.JSONObject

class LocalDiscovery {

   val errors = JSONArray()

   fun discovery(marketId: Long, supplierId: Long, maxProducts: Int) {

      val keywords = listOf<String>()

      if (keywords.isEmpty()) {
         println("keywords is required")
         return
      }

      discovery(marketId, keywords, maxProducts)
   }

   fun discovery(marketId: Long, keywords: List<String>, maxProducts: Int, corePoolSize: Int = 1) {

      val tasks = TestUtils.taskProcess(marketId = marketId, parameters = keywords, currentTest = TestType.KEYWORDS, productsLimit = maxProducts)

      val products = mutableListOf<RankingProducts>()

      for (task in tasks) {
         if (task is CrawlerRanking) {
            products += if (products.size >= maxProducts) {
               break
            } else if (products.size + task.arrayProducts.size <= maxProducts) {
               task.arrayProducts
            } else {
               task.arrayProducts.subList(0, maxProducts - products.size)
            }

         } else {
            println("não foi")
            println(task::class.java)
         }
      }

      val urls = products.map { it.url }

      val tests = TestUtils.poolTaskProcess(marketId = marketId, parameters = urls, currentTest = TestType.INSIGHTS, corePoolSize = corePoolSize)

      for (test in tests){
         for (task in test.tasks){
            if(task is TestCrawlerSession){
               val error = JSONObject()
               error.put(task.originalURL, task.lastError)
               errors.put(error)
            }
         }
      }

      CommonMethods.saveDataToAFile(errors,Test.pathWrite+"/log.json")

      val productsScraped = Test.products

      var productsFound = 0
      productsScraped.forEach { (_, u) ->  productsFound += u.size }

      println("Products ranking found: ${products.size}")
      println("Products core found: $productsFound")

      var count = 0
      for ((_, productList) in productsScraped) {
         for (product in productList) {
            count++
            println("$count || internalId: ${product.internalId} || isVoid: ${product.isVoid} || name: ${product.name} || url: ${product.url}")
         }
      }
   }
}
