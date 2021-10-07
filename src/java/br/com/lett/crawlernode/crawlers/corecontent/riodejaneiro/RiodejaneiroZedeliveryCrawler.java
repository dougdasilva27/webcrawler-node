package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler;

public class RiodejaneiroZedeliveryCrawler extends ZedeliveryCrawler {

   public RiodejaneiroZedeliveryCrawler(Session session) {
      super(session);
   }

   private String longitude = "-43.4748623";
   private String latitude = "-23.0256221";
   private String street = "Avenida Gilka Machado";
   private String neighborhood = "Recreio dos Bandeirantes";
   private String city = "Rio de Janeiro";
   private String province = "RJ";
   private String zipCode = "22795-570";

   @Override
   public ZedeliveryInfo getZedeliveryInfo() {
      return ZedeliveryInfoBuilder.create()
            .setLongitude(longitude)
            .setLatitude(latitude)
            .setNeighborhood(neighborhood)
            .setStreet(street)
            .setCity(city)
            .setProvince(province)
            .setZipCode(zipCode)
            .build();
   }

}
