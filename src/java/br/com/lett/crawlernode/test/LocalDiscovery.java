package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LocalDiscovery {

   protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);
   private final JSONArray errors = new JSONArray();
   public static final String RED_COLOR = "\033[1;31m";
   public static final String RESET_COLOR = "\033[0m";

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
            Logging.printLogDebug(logger, "Erro instace Crawler Ranking");
         }
      }

      List<String> urls = products.stream().map((RankingProduct::getUrl)).collect(Collectors.toList());

      List<TestRunnable> tests = TestUtils.poolTaskProcess(market, urls, TestType.CORE, maxProducts, corePoolSize);

      int count = 1, countProducts = 0, mismatchingId = 0, mismatchingPid = 0;

      for (TestRunnable test : tests) {
         for (Task task : test.tasks) {
            Session session = task.getSession();
            if (session instanceof TestCrawlerSession) {
               System.out.println(count + "|| url: " + session.getOriginalURL());
               count++;
               if (((TestCrawlerSession) session).getLastError() != null) {
                  JSONObject error = new JSONObject();
                  error.put(session.getOriginalURL(), ((TestCrawlerSession) session).getLastError().getStackTrace());
                  errors.put(error);
                  System.out.println("\t " + ((TestCrawlerSession) session).getLastError().getMessage());
               } else if (((TestCrawlerSession) session).getProducts() != null && !((TestCrawlerSession) session).getProducts().isEmpty()) {
                  countProducts++;
                  for (Product product : ((TestCrawlerSession) session).getProducts()) {
                     RankingProduct productRanking = products.stream().filter(rankingProduct -> Objects.equals(rankingProduct.getUrl(), product.getUrl())).findAny().orElse(null);
                     if(productRanking != null){
                        if(productRanking.getInternalId() != null && !productRanking.getInternalId().equals(product.getInternalId())) {
                           System.out.println(RED_COLOR + "MISMATCHING internalId: Ranking > " + productRanking.getInternalId() + " - Core > " + product.getInternalId());
                           mismatchingId++;
                        }
                        if(productRanking.getInternalPid() != null && !productRanking.getInternalPid().equals(product.getInternalPid())) {
                           System.out.println(RED_COLOR + "MISMATCHING internalPid: Ranking > " + productRanking.getInternalPid() + " - Core > " + product.getInternalPid());
                           mismatchingPid++;
                        }
                     }
                     System.out.println(RESET_COLOR + TestUtils.printProduct(product));
                  }
               }else {
                  System.out.println("\t Nenhum produto encontrado");
               }
            }
         }
      }

      CommonMethods.saveDataToAFile(errors, Test.pathWrite + "/log.json");
      System.out.println("Products ranking found: " + products.size());
      System.out.println(("Products core found: " + countProducts));
      System.out.println(RED_COLOR + "MISMATCHING internalPid found: " + mismatchingPid);
      System.out.println(RED_COLOR + "MISMATCHING internalId found: " + mismatchingId);
   }

}

