package br.com.lett.crawlernode.crawlers.corecontent.dourados;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;
import br.com.lett.crawlernode.util.CommonMethods;

/**
 * @author gabriel date: 2019-09-24
 */
public class DouradosSitemercadosupermercadoschamaCrawler extends BrasilSitemercadoCrawler {

   public DouradosSitemercadosupermercadoschamaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoschama/dourados-dourados-vila-planalto-rua-hayel-bon-faker-3855";

   public static final int IDLOJA = 787;
   public static final int IDREDE = 706;

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected Map<String, Integer> getLojaInfo() {
      Map<String, Integer> lojaInfo = new HashMap<>();
      lojaInfo.put("IdLoja", IDLOJA);
      lojaInfo.put("IdRede", IDREDE);
      return lojaInfo;
   }
}
