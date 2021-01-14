package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.RappiCrawler;

import java.util.Arrays;

   public class FortalezaRappimercadinhosaoluizCrawler extends RappiCrawler {
      public FortalezaRappimercadinhosaoluizCrawler(Session session) {
         super(session, Arrays.asList("900022515"));
      }
   }

