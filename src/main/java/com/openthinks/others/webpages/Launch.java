package com.openthinks.others.webpages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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


  /**
   * accept these command line arguments or not
   * 
   * @param args arguments for command line input
   * @return true or false
   */
  default boolean accept(String[] args) {
    return true;
  };

  /**
   * resolve partly parameters by given token
   * 
   * @param params arguments for command line input
   * @param token token which arguments start with
   * @return {@link PartParams}
   */
  static PartParams getPartParamsBy(final List<String> params, final String token) {
    PartParams part = new PartParams();
    int index = params.indexOf(token);
    if (index == -1 || index + 1 > params.size())
      return part;
    part.hasToken = true;
    for (int i = index + 1, j = params.size(); i < j; i++) {
      String param = params.get(i);
      if (param.startsWith("-")) {
        break;
      }
      part.addParam(param);
    }
    return part;
  }

  static PartParams getPartParamsBy(final String[] args, final String token) {
    List<String> list = null;
    if (args == null) {
      list = Collections.emptyList();
    } else {
      list = Arrays.asList(args);
    }
    return getPartParamsBy(list, token);
  }

  public static final class PartParams {
    private boolean hasToken = false;
    private List<String> params = new ArrayList<>();

    private void addParam(String param) {
      params.add(param);
    }

    public List<String> getParams() {
      return Collections.unmodifiableList(params);
    }

    public boolean isHasToken() {
      return hasToken;
    }

    public boolean isEmpty() {
      return params.isEmpty();
    }

    public String get(int index) {
      return params.get(index);
    }

    public String getAllJoined(String joined) {
      String delimiter = joined == null ? " " : joined;
      return String.join(delimiter, params);
    }

    public String getAllJoined() {
      return getAllJoined(" ");
    }
  }
}
