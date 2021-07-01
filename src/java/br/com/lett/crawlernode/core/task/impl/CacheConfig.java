package br.com.lett.crawlernode.core.task.impl;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RequestMethod;
import br.com.lett.crawlernode.core.models.Parser;

public class CacheConfig {

   private RequestMethod requestMethod;
   private Request request;
   private Parser parser = Parser.HTML;

   public CacheConfig() {
   }

   public boolean isActive() {
      return request != null && requestMethod != null;
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

   public Parser getParser() {
      return parser;
   }

   public void setParser(Parser parser) {
      this.parser = parser;
   }
}
