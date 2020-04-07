package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class FortalezaSitemercadosuperuchoasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public FortalezaSitemercadosuperuchoasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/superuchoasupermercado/fortaleza-loja-matriz-jardim-america-rua-andre-chaves";

   public static final int IDLOJA = 1838;
   public static final int IDREDE = 1613;

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
