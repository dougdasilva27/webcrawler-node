package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.util.*;

public class AngeloniSuperUtils  {
   private static final String HOME_PAGE = "www.angeloni.com.br";
   private static final String USER_CONFIRM_COOKIE = "f39c001644e53eaf330491f914127251";
   private static final String USER_ID_COOKIE = "1333925147";


   public static List<Cookie> fetchLocationCookies(Session session, DataFetcher dataFetcher) {
      BasicClientCookie userConfirm = new BasicClientCookie("DYN_USER_CONFIRM", USER_CONFIRM_COOKIE);
      userConfirm.setDomain(HOME_PAGE);
      userConfirm.setPath("/");

      BasicClientCookie userID = new BasicClientCookie("DYN_USER_ID", USER_ID_COOKIE);
      userID.setDomain(HOME_PAGE);
      userID.setPath("/");

      List<Cookie> userCookies = new ArrayList<>(Arrays.asList(userConfirm, userID));
      String addressId = session.getOptions().optString("addressId");

      if(addressId != null && !addressId.isEmpty()) {
         Map<String, String> headers = new HashMap<>();
         headers.put("cookie", CommonMethods.cookiesToString(userCookies));
         Request request = Request.RequestBuilder.create()
            .setUrl("https://www.angeloni.com.br/super/components/Customer/setShippingAddress.jsp?addressId=" + addressId)
            .setHeaders(headers)
            .setCookies(userCookies)
            .build();

         Response response = dataFetcher.get(session, request);

         userCookies.addAll(response.getCookies());
      }
      return userCookies;
   }
}
