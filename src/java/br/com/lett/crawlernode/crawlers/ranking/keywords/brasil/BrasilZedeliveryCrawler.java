package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler.ZedeliveryInfo;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler.ZedeliveryInfoBuilder;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ZedeliveryCrawlerRanking;

public class BrasilZedeliveryCrawler extends ZedeliveryCrawlerRanking {

   public BrasilZedeliveryCrawler(Session session) {
      super(session);
   }

   private String longitude = "-46.6931558";
   private String latitude = "-23.5674273";
   private String street = "Avenida Brigadeiro Faria Lima";
   private String neighborhood = "Pinheiros";
   private String city = "S\\u00e3o Paulo";
   private String province = "SP";

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
