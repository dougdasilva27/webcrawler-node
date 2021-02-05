package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.task.base.Task

class TestRunnable(
   val city: String = "",
   val marketName: String = "",
   val marketId: Long = 0,
   val parameters: List<String>,
   val currentTest: TestType = TestType.INSIGHTS,
   val productsLimit: Int = 0
) : Runnable {

   var tasks: List<Task> = listOf()

   override fun run() {
      tasks = TestUtils.taskProcess(city, marketName, marketId, parameters, currentTest, productsLimit)
   }
}
