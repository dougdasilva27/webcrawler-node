package br.com.lett.crawlernode.core.fetcher.methods;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;

import java.io.File;

public interface DataFetcher {

  /**
   * GET request
   *
   * @param session
   * @param request {@link Request}
   * @return {@link Response}
   */
  Response get(Session session, Request request);

  /**
   * POST request
   *
   * @param session
   * @param request {@link Request}
   * @return {@link Response}
   */
  Response post(Session session, Request request);

  /**
   * Download Image
   *
   * @param session
   * @param request {@link Request}
   * @return {@link File}
   */

  File fetchImage(Session session, Request request);
}
