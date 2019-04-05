package br.com.lett.crawlernode.crawlers.ratingandreviews.florianopolis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.LeroymerlinRatingReviewCrawler;

public class FlorianopolisLeroymerlinRatingReviewCrawler extends LeroymerlinRatingReviewCrawler {

  private static final String REGION = "santa_catarina";

  public FlorianopolisLeroymerlinRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  public String handleURLBeforeFetch(String curURL) {

    try {
      String url = curURL;
      List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
      List<NameValuePair> paramsNew = new ArrayList<>();

      for (NameValuePair param : paramsOriginal) {
        if (!param.getName().equals("region")) {
          paramsNew.add(param);
        }
      }

      paramsNew.add(new BasicNameValuePair("region", REGION));
      URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

      builder.clearParameters();
      builder.setParameters(paramsNew);

      curURL = builder.build().toString();

      return curURL;

    } catch (URISyntaxException e) {
      return curURL;
    }
  }


}
