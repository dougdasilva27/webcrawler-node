package br.com.lett.crawlernode.util;

import br.com.lett.crawlernode.main.GlobalConfigurations;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class CustomLevelFilter extends Filter<ILoggingEvent> {

  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (GlobalConfigurations.executionParameters.getDebug() == false && event.getLevel() == Level.DEBUG) {
      return FilterReply.DENY;
    } else {
      return FilterReply.ACCEPT;
    }
  }

}
