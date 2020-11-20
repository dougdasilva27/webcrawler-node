package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SaopauloSitemercadobarbosaermelinomatarazzoCrawler extends BrasilSitemercadoCrawler {

   public SaopauloSitemercadobarbosaermelinomatarazzoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/sao-paulo-loja-07-ermelino-matarazzo-vila-paranagua-rua-victoria-simionato";

   public static final int IDLOJA = 316;
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
