package br.com.lett.crawlernode.core.task.impl;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RequestMethod;

public class CacheConfig {

   private RequestMethod requestMethod;
   private Request request;

   public CacheConfig() {
   }

   public RequestMethod getRequestMethod() {
      return requestMethod;
   }

   public void setRequestMethod(RequestMethod requestMethod) {
      this.requestMethod = requestMethod;
   }

   public Request getRequest() {
      return request;
   }

   public void setRequest(Request request) {
      this.request = request;
   }
}
