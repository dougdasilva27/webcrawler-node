package br.com.lett.crawlernode.crawlers.corecontent.campinas;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2020-11-04
 */
public class CampinasSitemercadogoddbomCrawler extends BrasilSitemercadoCrawler {
   public CampinasSitemercadogoddbomCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/goodbom/campinas-loja-sousas-jardim-conceicao-av-doutor-antonio-carlos-couto-de-barros";

   public static final int IDLOJA = 2883;
   public static final int IDREDE = 287;

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