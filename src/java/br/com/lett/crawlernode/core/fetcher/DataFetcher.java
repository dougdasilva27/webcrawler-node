package br.com.lett.crawlernode.core.fetcher;

import java.io.File;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;

public interface DataFetcher {

  /**
   * GET request
   * 
   * @param session
   * @param request {@link Request}
   * @return {@link Response}
   */
  public Response get(Session session, Request request);

  /**
   * POST request
   * 
   * @param session
   * @param request {@link Request}
   * @return {@link Response}
   */
  public Response post(Session session, Request request);

  /**
   * Download Image
   * 
   * @param session
   * @param request {@link Request}
   * @return {@link File}
   */

  public File fetchImage(Session session, Request request);
}
