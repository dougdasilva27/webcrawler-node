package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.crawlers.extractionutils.core.Vipcommerce;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */

   public class SaopauloSitemercadovioletaedgarfacoCrawler extends Vipcommerce {

      public SaopauloSitemercadovioletaedgarfacoCrawler(Session session) {
         super(session);
      }

      public static final String HOME_PAGE = "https://supermercadovioleta.com.br/";


      protected String getHomePage(){
         return HOME_PAGE;
      }

      protected String getSellerFullName(){
         return "supermerca do violeta";
      }

      protected String getDomain(){
         return "violetaexpress.com.br";
      }
}
