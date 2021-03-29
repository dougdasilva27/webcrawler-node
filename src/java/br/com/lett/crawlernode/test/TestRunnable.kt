package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.models.Market
import br.com.lett.crawlernode.core.task.base.Task

class TestRunnable(
   val market: Market ,
   val parameters: List<String>,
   val currentTest: TestType = TestType.CORE,
   val productsLimit: Int = 0
) : Runnable {

   var tasks: List<Task> = listOf()

   override fun run() {
      tasks = TestUtils.taskProcess( market, parameters, currentTest, productsLimit)
   }
}
