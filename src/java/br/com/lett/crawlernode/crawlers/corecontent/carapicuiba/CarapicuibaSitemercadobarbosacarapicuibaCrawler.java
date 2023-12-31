package br.com.lett.crawlernode.crawlers.corecontent.carapicuiba;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CarapicuibaSitemercadobarbosacarapicuibaCrawler extends BrasilSitemercadoCrawler {

   public CarapicuibaSitemercadobarbosacarapicuibaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/carapicuiba-loja-37-vila-silva-vila-silva-ribeiro-av-inocencio-serafico";

   public static final int IDLOJA = 2258;
   public static final int IDREDE = 135;

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
