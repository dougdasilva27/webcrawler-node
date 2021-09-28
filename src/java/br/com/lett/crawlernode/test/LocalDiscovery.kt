package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.models.Market
import br.com.lett.crawlernode.core.models.RankingProduct
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking
import br.com.lett.crawlernode.util.CommonMethods
import org.json.JSONArray
import org.json.JSONObject

class LocalDiscovery {

   val errors = JSONArray()



   fun discovery(market: Market, keywords: List<String>, maxProducts: Int, corePoolSize: Int = 1) {

      val tasks = TestUtils.taskProcess(market, parameters = keywords, currentTest = TestType.KEYWORDS, productsLimit = maxProducts)

      val products = mutableListOf<RankingProduct>()

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

      val tests = TestUtils.poolTaskProcess(market, parameters = urls, currentTest = TestType.CORE, corePoolSize = corePoolSize)

      var count = 0
      for (test in tests) {
         for (task in test.tasks) {
            val session = task.session
            if (session is TestCrawlerSession) {
               if (session.lastError != null) {
                  val error = JSONObject()
                  error.put(session.originalURL, session.lastError)
                  errors.put(error)
               }
               count++
               println("$count|| url: ${session.originalURL}")
               if(session.products != null) {
                  for (product in session.products) {
                     println("\t internalId: ${product.internalId} || isVoid: ${product.name == null} || name: ${product.name}")
                  }
               }
            }
         }
      }
      CommonMethods.saveDataToAFile(errors, Test.pathWrite + "/log.json")

      println("Products ranking found: ${products.size}")
      println("Products core found: $count")

   }
}
