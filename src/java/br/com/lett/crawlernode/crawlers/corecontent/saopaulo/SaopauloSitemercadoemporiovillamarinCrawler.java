package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class SaopauloSitemercadoemporiovillamarinCrawler extends BrasilSitemercadoCrawler {
   public SaopauloSitemercadoemporiovillamarinCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/emporiovillamarin/sao-paulo-emporio-villamarin-vila-regente-feijo-r-capituba";

   public static final int IDLOJA = 4843;
   public static final int IDREDE = 2754;

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