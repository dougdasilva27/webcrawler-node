package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.LifeappsCrawler;

/**
 * Date: 31/07/2019
 *
 * @author Joao Pedro
 */
public class BrasilJcdistribuicaoCrawler extends LifeappsCrawler {

   private static final String HOME_PAGE = "https://jcdistribuicao.superon.app/";
   private static final String COMPANY_ID = "6f0ae38d-50cd-4873-89a5-6861467b5f52";    //never changes
   private static final String API_HASH = "cc64548c0cbad6cf58d4d3bbd433142b694d281e-b509-42c4-8042-78cfeb0c52ff"; //can change, but is working since 2019
   private static final String FORMA_PAGAMENTO = "3dedac19-6643-4401-9c08-aac81d6edb7c";  //never changes
   private static final String SELLER_NAME_LOWER = "jc distribuicao brasil";

   public BrasilJcdistribuicaoCrawler(Session session) {
      super(session);
   }

   @Override
   public String getHomePage() {
      return HOME_PAGE;
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
   public String getSellerName() {
      return SELLER_NAME_LOWER;
   }
}
