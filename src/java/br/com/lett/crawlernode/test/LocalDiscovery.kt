package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.models.RankingProducts
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking

class LocalDiscovery {

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

      TestUtils.poolTaskProcess(marketId = marketId, parameters = urls, currentTest = TestType.INSIGHTS, corePoolSize = corePoolSize)

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
