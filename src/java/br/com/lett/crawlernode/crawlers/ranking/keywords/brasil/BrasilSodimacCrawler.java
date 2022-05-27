package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SodimacCrawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;

public class BrasilSodimacCrawler extends SodimacCrawler {
   public BrasilSodimacCrawler(Session session) {
      super(session);
   }

   @Override
   public String getBaseUrl() {
      return "https://www.sodimac.com.br/sodimac-br/";
   }

   @Override
   public char getPriceFormat() {
      return ',';
   }
}
