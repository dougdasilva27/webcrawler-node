package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CarrefourCrawler;


/**
 * 18/04/2020
 * 
 * @author Fabrício
 *
 */
public class SaopauloCarrefouranchietaCrawler extends CarrefourCrawler {

   public SaopauloCarrefouranchietaCrawler(Session session) {
      super(session);
   }

   public static final String HOME_PAGE = "https://mercado.carrefour.com.br/";
   public static final String LOCATION = "04702-000";
   public static final String LOCATION_TOKEN = "eyJhbGciOiJFUzI1NiIsImtpZCI6IjgxOERFQ0U0NkNCOEUxNzE1NkIxRUQwNzc3RUQzNjQ3QzM1NjdFNkMiLCJ0eXAiOiJqd3QifQ.eyJhY2NvdW50LmlkIjoiNzQzMWNlNzItMTJlMy00OTczLWE5MjUtZTA2YTBmODhiZjU3IiwiaWQiOiIwZTM2MzhlNC01MGEzLTQ5MjQtOTc5Yi0yMDU5NzU4MWYxZTYiLCJ2ZXJzaW9uIjoyLCJzdWIiOiJzZXNzaW9uIiwiYWNjb3VudCI6InNlc3Npb24iLCJleHAiOjE2MDcyMDEzNjksImlhdCI6MTYwNjUxMDE2OSwiaXNzIjoidG9rZW4tZW1pdHRlciIsImp0aSI6ImUwMDZiNTZiLTQ0ZmUtNDM1YS1iZjFmLTZmN2M1Mjk1NGNmOSJ9.-MHF4G6rvgP_bs_dRKQBkdxT_cT7-L0E8cCYJWVF6ke_EXzSlUkL2vejFde7FGZMohFL20L1Agg5mxACCazkoQ";

   @Override
   protected String getLocationToken() {
      return LOCATION_TOKEN;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }
}
