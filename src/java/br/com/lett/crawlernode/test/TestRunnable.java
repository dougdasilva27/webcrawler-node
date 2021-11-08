package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.task.base.Task;

import java.util.ArrayList;
import java.util.List;

public class TestRunnable implements Runnable {

   Market market;
   List<String> parameters;
   TestType currentTest = TestType.CORE;
   Integer productsLimit = 0;

   List<Task> tasks = new ArrayList<>();

   private TestRunnable() {
   }

   public TestRunnable(Market market, List<String> parameters, TestType currentTest, Integer productsLimit) {
      this.market = market;
      this.parameters = parameters;
      this.currentTest = currentTest;
      this.productsLimit = productsLimit;
   }

   @Override
   public void run() {
      tasks = TestUtils.taskProcess(market, parameters, currentTest, productsLimit);
   }
}
