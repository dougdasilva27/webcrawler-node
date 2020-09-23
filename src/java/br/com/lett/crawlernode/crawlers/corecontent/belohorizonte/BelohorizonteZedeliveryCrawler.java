package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.ZedeliveryCrawler;

public class BelohorizonteZedeliveryCrawler extends ZedeliveryCrawler {

   public BelohorizonteZedeliveryCrawler(Session session) {
      super(session);
   }

   private String longitude = "-43.90162520000001";
   private String latitude = "-19.8721346";
   private String street = "Avenida Josefino Gonçalves da Silva";
   private String neighborhood = "Goiânia";
   private String city = "Belo Horizonte";
   private String province = "MG";

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
