package br.com.lett.crawlernode.crawlers.corecontent.maringa;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class MaringaSitemercadocidadecancaoCrawler extends BrasilSitemercadoCrawler {
   public MaringaSitemercadocidadecancaoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoscidadecancao/maringa-loja-maringa-01-zona-05-avenida-brasil";

   public static final int IDLOJA = 504;
   public static final int IDREDE = 439;

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