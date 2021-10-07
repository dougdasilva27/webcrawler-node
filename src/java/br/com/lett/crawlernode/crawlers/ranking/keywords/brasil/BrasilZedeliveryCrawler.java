package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler.ZedeliveryInfo;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler.ZedeliveryInfoBuilder;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ZedeliveryCrawlerRanking;

public class BrasilZedeliveryCrawler extends ZedeliveryCrawlerRanking {

   public BrasilZedeliveryCrawler(Session session) {
      super(session);
   }

   private String longitude = "-46.6942293";
   private String latitude = "-23.5627391";
   private String street = "Avenida Brigadeiro Faria Lima";
   private String neighborhood = "Pinheiros";
   private String city = "São Paulo";
   private String province = "SP";
   private String zipCode = "05426-100";

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
