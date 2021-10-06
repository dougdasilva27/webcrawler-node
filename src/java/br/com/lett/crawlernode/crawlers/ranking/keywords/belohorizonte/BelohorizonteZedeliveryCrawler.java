package br.com.lett.crawlernode.crawlers.ranking.keywords.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler.ZedeliveryInfo;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ZedeliveryCrawler.ZedeliveryInfoBuilder;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ZedeliveryCrawlerRanking;

public class BelohorizonteZedeliveryCrawler extends ZedeliveryCrawlerRanking {

   public BelohorizonteZedeliveryCrawler(Session session) {
      super(session);
   }

   private String longitude = "-43.9016252";
   private String latitude = "-19.8721346";
   private String street = "Avenida Josefino Gonçalves da Silva";
   private String neighborhood = "Goiânia";
   private String city = "Belo Horizonte";
   private String province = "MG";
   private String zipCode = "31950-020";

   @Override
   protected ZedeliveryInfo getZedeliveryInfo() {
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
