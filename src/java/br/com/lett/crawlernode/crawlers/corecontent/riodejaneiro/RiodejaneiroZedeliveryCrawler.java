package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler;

public class RiodejaneiroZedeliveryCrawler extends ZedeliveryCrawler {

   public RiodejaneiroZedeliveryCrawler(Session session) {
      super(session);
   }

   private String longitude = "-43.47541510000001";
   private String latitude = "-23.024845";
   private String street = "Avenida Brigadeiro Faria Lima";
   private String neighborhood = "Recreio dos Bandeirantes";
   private String city = "Rio de Janeiro";
   private String province = "RJ";

   @Override
   protected ZedeliveryInfo getZedeliveryInfo() {
      return ZedeliveryInfoBuilder.create()
            .setLongitude(longitude)
            .setLatitude(latitude)
            .setNeighborhood(neighborhood)
            .setStreet(street)
            .setCity(city)
            .setProvince(province)
            .build();
   }

}
