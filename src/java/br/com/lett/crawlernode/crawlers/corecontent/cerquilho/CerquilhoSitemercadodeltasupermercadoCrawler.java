package br.com.lett.crawlernode.crawlers.corecontent.cerquilho;

import java.util.HashMap;
import java.util.Map;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilSitemercadoCrawler;

/**
 * @author gabriel date: 2019-09-24
 */
public class CerquilhoSitemercadodeltasupermercadoCrawler extends BrasilSitemercadoCrawler {

   public CerquilhoSitemercadodeltasupermercadoCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://www.sitemercado.com.br/deltasupermercados/cerquilho-loja-cerquilho-centro-avenida-dr-vinicio-gagliardi";

   public static final int IDLOJA = 4861;
   public static final int IDREDE = 2268;

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
