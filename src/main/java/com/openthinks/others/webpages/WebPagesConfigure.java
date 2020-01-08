package com.openthinks.others.webpages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Optional;
import java.util.Properties;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.openthinks.libs.utilities.Result;
import com.openthinks.libs.utilities.logger.PLLevel;

/**
 * The configuration for {@link WebPagesLaunch}
 * 
 * @author dailey.yet@outlook.com
 *
 */
public class WebPagesConfigure extends Properties {

  public class Proxy {
    public class Auth {
      String name;
      String pass;
    }

    String host;
    Integer port;
    Auth auth;

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public Integer getPort() {
      return port;
    }

    public void setPort(Integer port) {
      this.port = port;
    }

    public Auth getAuth() {
      return auth;
    }

    public void setAuth(Auth auth) {
      this.auth = auth;
    }

    public boolean hasAuth() {
      return auth != null && auth.name != null && auth.pass != null;
    }
  }

  private static final long serialVersionUID = 1571830072530562701L;
  public static final String BROWSERVERSION = "browser-version";
  public static final String PROXYHOST = "proxy-host";
  public static final String PROXYPORT = "proxy-port";
  public static final String PROXYAUTHNAME = "proxy-auth-user";
  public static final String PROXYAUTHPASS = "proxy-auth-pass";
  public static final String LOGINPAGEURL = "login-url";
  public static final String LOGINFORMSELECTOR = "login-form-selector";
  public static final String LOGINFROMINDEX = "login-form-index";
  public static final String LOGINSUBMITBTNNAME = "login-form-submit-name";
  public static final String LOGINNAMEINPUTNAME = "login-form-username-input-name";
  public static final String LOGINPASSINPUTNAME = "login-form-password-input-name";
  public static final String LOGINNAMEVALUE = "login-form-username-input-value";
  public static final String LOGINPASSVALUE = "login-form-password-input-value";
  public static final String KEEPDIR = "save-dir";
  public static final String NEEDLOGIN = "need-login";
  public static final String CATALOGPAGEURL = "pages-catalog-url";
  public static final String PAGELINKOFCATALOGSELECTOR = "catalog-pagelinks-selector";
  public static final String STARTCHAINPAGEURL = "pages-first-url";
  public static final String NEXTCHAINPAGEANCHORSELECTOR = "pages-next-anchor-selector";
  public static final String LOGGERLEVEL = "logger-level";
  public static final String SESSION_TIMEOUT = "session-timeout";

  public static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

  ///////////////////////////
  public static WebPagesConfigure create() {
    return new WebPagesConfigure();
  }

  public static WebPagesConfigure readXML(InputStream is)
      throws InvalidPropertiesFormatException, IOException {
    WebPagesConfigure instance = WebPagesConfigure.create();
    instance.loadFromXML(is);
    return instance;
  }

  public static WebPagesConfigure readProps(InputStream is) throws IOException {
    WebPagesConfigure instance = WebPagesConfigure.create();
    instance.load(is);
    return instance;
  }

  ///////////////////////////

  public WebPagesConfigure() {
    super();
  }

  public Optional<String> getProp(String propertyName) {
    try {
      return Optional.of(this.getProperty(propertyName));
    } catch (Exception e) {
    }
    return Optional.empty();
  }

  public BrowserVersion getBrowserVersion() {
    Optional<String> version = getProp(BROWSERVERSION);
    if (version.isPresent()) {
      switch (version.get()) {
        case "FF52":
          return BrowserVersion.FIREFOX_52;
        case "FF45":
          return BrowserVersion.FIREFOX_45;
        case "CHROME":
          return BrowserVersion.CHROME;
        case "IE":
          return BrowserVersion.INTERNET_EXPLORER;
        case "EDGE":
          return BrowserVersion.EDGE;
        default:
          return BrowserVersion.BEST_SUPPORTED;
      }
    }
    return BrowserVersion.getDefault();
  }

  public void setBrowserVersion(BrowserVersion version) {
    BrowserVersion bversion = (version == null) ? BrowserVersion.getDefault() : version;
    this.setProperty(BROWSERVERSION, bversion.getNickname());
  }

  public Optional<String> getProxyHost() {
    return getProp(PROXYHOST);
  }

  public void setProxyHost(String host) {
    this.setProperty(PROXYHOST, host);
  }

  public int getProxyPort() {
    Optional<String> portOpt = getProp(PROXYPORT);
    if (portOpt.isPresent()) {
      try {
        return Integer.valueOf(portOpt.get());
      } catch (NumberFormatException e) {
      }
    }
    return 80;
  }

  public void setProxyPort(int port) {
    setProperty(PROXYPORT, String.valueOf(port));
  }

  public Optional<Proxy> getProxy() {
    Result<Proxy> result = new Result<>(null);
    Optional<String> hostOpt = getProp(PROXYHOST);
    Optional<String> authNameOpt = getProp(PROXYAUTHNAME);
    Optional<String> authPassOpt = getProp(PROXYAUTHPASS);
    hostOpt.ifPresent((host) -> {
      if ("".equals(host.trim()))
        return;
      Proxy proxy = new Proxy();
      proxy.host = host;
      proxy.port = getProxyPort();
      result.set(proxy);
      if (authNameOpt.isPresent() && authPassOpt.isPresent()) {
        if ("".equals(authNameOpt.get().trim()) || "".equals(authPassOpt.get().trim()))
          ;
        else {
          proxy.auth = proxy.new Auth();
          proxy.auth.name = authNameOpt.get();
          proxy.auth.pass = authPassOpt.get();
        }
      }
    });
    return Optional.ofNullable(result.get());
  }

  public void setProxyAuthName(String name) {
    this.setProperty(PROXYAUTHNAME, name);
  }

  public Optional<String> getProxyAuthName() {
    return getProp(PROXYAUTHNAME);
  }

  public void setProxyAuthPass(String pass) {
    this.setProperty(PROXYAUTHPASS, pass);
  }

  public Optional<String> getProxyAuthPass() {
    return getProp(PROXYAUTHPASS);
  }

  public Optional<String> getLoginPageUrl() {
    return getProp(LOGINPAGEURL);
  }

  public void setLoginPageUrl(String value) {
    setProperty(LOGINPAGEURL, value);
  }

  public Optional<String> getLoginFormSelector() {
    return getProp(LOGINFORMSELECTOR);
  }

  public void setLoginFormSelector(String value) {
    setProperty(LOGINFORMSELECTOR, value);
  }

  public int getLoginFormIndex() {
    if (getProp(LOGINFROMINDEX).isPresent()) {
      try {
        return Integer.valueOf(getProp(LOGINFROMINDEX).get());
      } catch (NumberFormatException e) {
      }
    }
    return 0;
  }

  public void setLoginFormIndex(int value) {
    setProperty(LOGINFROMINDEX, String.valueOf(value));
  }

  public Optional<String> getLoginSubmitBtnName() {
    // return Optional.of("login");
    return getProp(LOGINSUBMITBTNNAME);
  }

  public void setLoginSubmitBtnName(String value) {
    setProperty(LOGINSUBMITBTNNAME, value);
  }

  public Optional<String> getLoginAuthInputName() {
    // return Optional.of("email");
    return getProp(LOGINNAMEINPUTNAME);
  }

  public void setLoginAuthInputName(String value) {
    setProperty(LOGINNAMEINPUTNAME, value);
  }

  public Optional<String> getLoginAuthInputValue() {
    return getProp(LOGINNAMEVALUE);
  }

  public void setLoginAuthInputValue(String value) {
    setProperty(LOGINNAMEVALUE, value);
  }

  public Optional<String> getLoginAuthPassInputName() {
    // return Optional.of("password1");
    return getProp(LOGINPASSINPUTNAME);
  }

  public void setLoginAuthPassInputName(String value) {
    setProperty(LOGINPASSINPUTNAME, value);
  }

  public Optional<String> getLoginAuthPassInputValue() {
    return getProp(LOGINPASSVALUE);
  }

  public void setLoginAuthPassInputValue(String value) {
    setProperty(LOGINPASSVALUE, value);
  }

  public Optional<File> getKeepDir() {
    if (getProp(KEEPDIR).isPresent()) {
      return Optional.of(new File(getProp(KEEPDIR).get()));
    }
    return Optional.ofNullable(null);
  }

  public void setKeepDir(String value) {
    setProperty(KEEPDIR, value);
  }

  public Optional<Boolean> needLogin() {
    if (getProp(NEEDLOGIN).isPresent()) {
      return Optional.of(Boolean.valueOf(getProp(NEEDLOGIN).get()));
    }
    return Optional.ofNullable(true);
  }

  public void setNeedLogin(Boolean value) {
    setProperty(NEEDLOGIN, String.valueOf(value));
  }

  public Optional<String> getCatalogPageUrl() {
    return getProp(CATALOGPAGEURL);
  }

  public void setCatalogPageUrl(String value) {
    setProperty(CATALOGPAGEURL, value);
  }

  public Optional<String> getPageLinkOfCatalogSelector() {
    // return Optional.of("div.catalog_container a[href*='9781491901939']");
    return getProp(PAGELINKOFCATALOGSELECTOR);
  }

  public void setPageLinkOfCatalogSelector(String value) {
    setProperty(PAGELINKOFCATALOGSELECTOR, value);
  }

  public Optional<String> getStartChainPageUrl() {

    return getProp(STARTCHAINPAGEURL);
  }

  public void setStartChainPageUrl(String value) {
    setProperty(STARTCHAINPAGEURL, value);
  }

  public Optional<String> getNextChainPageAnchorSelector() {
    // return Optional.of("a.next[title*='Next (Key: n)']");
    return getProp(NEXTCHAINPAGEANCHORSELECTOR);
  }

  public void setNextChainPageAnchorSelector(String value) {
    setProperty(NEXTCHAINPAGEANCHORSELECTOR, value);
  }

  public PLLevel getLoggerLevel() {
    PLLevel pLevel = null;
    if (getProp(LOGGERLEVEL).isPresent()) {
      pLevel = PLLevel.valueOf(getProp(LOGGERLEVEL).get());
    }
    if (pLevel == null) {
      pLevel = PLLevel.INFO;
    }
    return pLevel;
  }

  public void setLoggerLevel(PLLevel level) {
    setProperty(LOGGERLEVEL, level.name());
  }

  public Optional<Long> getSessionTimeout() {
    Optional<String> oplTimeout = getProp(SESSION_TIMEOUT);
    if (oplTimeout.isPresent()) {
      try {
        Long timeout = Long.valueOf(oplTimeout.get());
        return Optional.of(timeout);
      } catch (NumberFormatException e) {
      }
    }
    return Optional.empty();
  }

  public void setSessionTimeout(long timeout) {
    setProperty(SESSION_TIMEOUT, String.valueOf(timeout));
  }
}
