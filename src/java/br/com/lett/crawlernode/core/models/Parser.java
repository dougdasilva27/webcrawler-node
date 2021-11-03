package br.com.lett.crawlernode.core.models;

import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.function.Function;

public enum Parser {
   HTML(Jsoup::parse), JSON(JSONObject::new), NONE(s -> s), JSONARRAY(CrawlerUtils::stringToJsonArray);

   private final Function<String, Object> function;

   Parser(Function<String, Object> function) {
      this.function = function;
   }

   public Object parse(String string) {
      return function.apply(string);
   }
}
