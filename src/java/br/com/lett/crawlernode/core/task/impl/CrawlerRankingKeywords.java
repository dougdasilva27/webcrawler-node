package br.com.lett.crawlernode.core.task.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.util.CommonMethods;

public abstract class CrawlerRankingKeywords extends CrawlerRanking {

  protected static Logger logger = LoggerFactory.getLogger(CrawlerRankingKeywords.class);

  private static final String RANK_TYPE = "keywords";
  protected String keywordEncoded;
  protected String keywordWithoutAccents;


  public CrawlerRankingKeywords(Session session) {
    super(session, RANK_TYPE, logger);

    if (session instanceof RankingKeywordsSession) {
      this.location = ((RankingKeywordsSession) session).getLocation();
    } else if (session instanceof TestRankingKeywordsSession) {
      this.location = ((TestRankingKeywordsSession) session).getLocation();
    } else if (session instanceof RankingDiscoverKeywordsSession) {
      this.location = ((RankingDiscoverKeywordsSession) session).getLocation();
    }
    // Danger! I remove this because it is too old and it can be unuseless.
    // if (!"mexico".equals(session.getMarket().getCity())) {
    this.keywordWithoutAccents = CommonMethods.removeAccents(this.location.replaceAll("/", " ").replaceAll("\\.", ""));
    // }

    try {
      this.keywordEncoded = URLEncoder.encode(location, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
      session.registerError(error);
    }
  }
}
