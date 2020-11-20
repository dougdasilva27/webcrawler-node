package br.com.lett.crawlernode.crawlers.corecontent.sorocaba;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class SorocabaSitemercadobarbosasorocabaCrawler extends BrasilSitemercadoCrawler {

   public SorocabaSitemercadobarbosasorocabaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/barbosa/sorocaba-loja-36-sorocaba-jardim-santa-rosalia-avenida-dom-aguirre";

   public static final int IDLOJA = 1089;
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
