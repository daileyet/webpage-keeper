package com.openthinks.others.webpages;

import com.openthinks.others.webpages.conf.WebPagesConfigure;

public interface Launch {

  /**
   * start web page transfer
   * 
   * @throws Exception when start failed
   */
  void start() throws Exception;

  /**
   * stop web page transfer
   */
  void stop();

  /**
   * configuration of web page transfer
   * 
   * @param config {@link WebPagesConfigure}
   */
  void setConfig(WebPagesConfigure config);

}
