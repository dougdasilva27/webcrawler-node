package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LifeappsCrawlerRanking;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

/**
 * Date: 31/07/2019
 * 
 * @author Joao Pedro
 *
 */
public class BrasilJcdistribuicaoCrawler extends LifeappsCrawlerRanking {

  private static final String HOME_PAGE = "https://jcdistribuicao.superon.app/";
  private static final String COMPANY_ID = "6f0ae38d-50cd-4873-89a5-6861467b5f52";  //never changes
  private static final String API_HASH = "dccca000-b2ea-11e9-a27c-b76c91df9dd6cc64548c0cbad6cf58d4d3bbd433142b"; //can change, but is working since 2019
  private static final String FORMA_PAGAMENTO = "2001546a-9851-4393-bb68-7c04e932fa4c";   //never changes

   public BrasilJcdistribuicaoCrawler(Session session) {
      super(session);
   }

   @Override
   public String getCompanyId() {
      return COMPANY_ID;
   }

   @Override
   public String getApiHash() {
      return API_HASH;
   }

   @Override
   public String getFormaDePagamento() {
      return FORMA_PAGAMENTO;
   }

   @Override
   public String getHomePage() {
      return HOME_PAGE;
   }
}
