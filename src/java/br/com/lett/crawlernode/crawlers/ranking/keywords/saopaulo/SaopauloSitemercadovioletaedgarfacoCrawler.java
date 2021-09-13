package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.VipcommerceRanking;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadovioletaedgarfacoCrawler extends VipcommerceRanking {

   public SaopauloSitemercadovioletaedgarfacoCrawler(Session session) {
      super(session);
   }

   protected String getSellerFullName(){
      return "supermerca do violeta";
   }

   protected String getDomain(){
      return "violetaexpress.com.br";
   }




}
