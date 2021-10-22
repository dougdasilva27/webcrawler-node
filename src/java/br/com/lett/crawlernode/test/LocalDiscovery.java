package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LocalDiscovery {

   JSONArray errors = new JSONArray();

   public void discovery(Market market, List<String> keywords, Integer maxProducts, Integer corePoolSize) throws InterruptedException {

      List<Task> tasks = TestUtils.taskProcess(market, keywords, TestType.KEYWORDS, maxProducts);

      List<RankingProduct> products = new ArrayList<>();

      for (Task task : tasks) {
         if (task instanceof CrawlerRanking) {
            if (products.size() >= maxProducts) {
               break;
            } else if (products.size() + ((CrawlerRanking) task).getArrayProducts().size() <= maxProducts) {
               products.addAll(((CrawlerRanking) task).getArrayProducts());
            } else {
               products.addAll(((CrawlerRanking) task).getArrayProducts().subList(0, maxProducts - products.size()));
            }

         } else {
            System.err.println("Erro instace Crawler Ranking");
         }
      }

      List<String> urls = products.stream().map((RankingProduct::getUrl)).collect(Collectors.toList());

      List<TestRunnable> tests = TestUtils.poolTaskProcess(market, urls, TestType.CORE, maxProducts, corePoolSize);

      int count = 0;
      for (TestRunnable test : tests) {
         for (Task task : test.tasks) {
            Session session = task.getSession();
            if (session instanceof TestCrawlerSession) {
               if (((TestCrawlerSession) session).getLastError() != null) {
                  JSONObject error = new JSONObject();
                  error.put(((TestCrawlerSession) session).getLastError(), ((TestCrawlerSession) session).getLastError());
                  errors.put(error);
               }
               count++;
               System.out.println(count + "|| url: " + session.getOriginalURL());
               if (((TestCrawlerSession) session).getProducts() != null) {
                  for (Product product : ((TestCrawlerSession) session).getProducts()) {
                     System.out.println("\t internalId: " + product.getInternalId() + " || isVoid: " + product.getName() == null + " || name: " + product.getName());
                  }
               }
            }
         }
      }
      CommonMethods.saveDataToAFile(errors, Test.pathWrite + "/log.json");

      System.out.println("Products ranking found: " + products.size());
      System.out.println(("Products core found: " + count));

   }


}
