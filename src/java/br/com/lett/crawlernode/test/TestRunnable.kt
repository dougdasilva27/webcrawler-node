package br.com.lett.crawlernode.test

class TestRunnable(
   val city: String = "",
   val marketName: String = "",
   val marketId: Long = 0,
   val parameters: List<String>,
   val currentTest: TestType = TestType.INSIGHTS,
   val productsLimit: Int = 0
) : Runnable {

   override fun run() {
      TestUtils.taskProcess(city, marketName, marketId, parameters, currentTest, productsLimit)
   }
}
