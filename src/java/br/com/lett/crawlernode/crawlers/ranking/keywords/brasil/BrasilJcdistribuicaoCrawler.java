package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.LifeappsCrawlerRanking;

/**
 * Date: 31/07/2019
 *
 * @author Joao Pedro
 */
public class BrasilJcdistribuicaoCrawler extends LifeappsCrawlerRanking {

   private static final String HOME_PAGE = "https://jcdistribuicao.superon.app/";
   private static final String COMPANY_ID = "6f0ae38d-50cd-4873-89a5-6861467b5f52";  //never changes
   private static final String API_HASH = "28f67bd0-5398-11ed-9f27-2fab409bac8ecc64548c0cbad6cf58d4d3bbd433142b43bae739-9ee6-4f36-b154-5fa748f7f280"; //can change, but is working since 2022
   private static final String FORMA_PAGAMENTO = "82df55b2-0cb6-40f0-9162-a2c7345d4fb0";   //never changes

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
