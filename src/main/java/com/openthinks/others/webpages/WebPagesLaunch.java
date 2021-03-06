package com.openthinks.others.webpages;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.openthinks.libs.utilities.CommonUtilities;
import com.openthinks.libs.utilities.Result;
import com.openthinks.libs.utilities.logger.ProcessLogger;
import com.openthinks.others.webpages.conf.WebPagesConfigure;
import com.openthinks.others.webpages.exception.LaunchFailedException;
import com.openthinks.others.webpages.exception.LostConfigureItemException;
import com.openthinks.others.webpages.exception.ManualStopException;
import com.openthinks.others.webpages.transfer.BatchHtmlPageTransfer;
import com.openthinks.others.webpages.transfer.HtmlPageTransfer;

/**
 * The web pages download launcher
 * 
 * @author dailey.yet@outlook.com
 *
 */
public class WebPagesLaunch implements Launch {

  protected WebPagesConfigure config;

  private transient WebClient referClient;

  private volatile boolean running = false;

  protected volatile boolean sessionTimeout = false;

  protected Timer sessionTimer = null;

  public WebPagesLaunch() {
    super();
  }

  public WebPagesLaunch(WebPagesConfigure config) {
    super();
    this.config = config;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.openthinks.others.webpages.Launch#setConfig(com.openthinks.others.webpages.
   * WebPagesConfigure)
   */
  @Override
  public void setConfig(WebPagesConfigure config) {
    this.config = config;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.openthinks.others.webpages.Launch#start()
   */
  @Override
  public void start() throws Exception {
    ProcessLogger.debug("WebPagesLanuch start...");
    try {
      LogManager.getLogManager()
          .readConfiguration(WebPagesLaunch.class.getResourceAsStream("/logging.properties"));
    } catch (SecurityException | IOException e1) {
      ProcessLogger.warn(CommonUtilities.getCurrentInvokerMethod(), e1);
    }
    ProcessLogger.currentLevel = config.getLoggerLevel();
    try {
      this.preProcessConfigure();
      this.launch();
    } catch (SecurityException | IOException | LostConfigureItemException e) {
      ProcessLogger.fatal(CommonUtilities.getCurrentInvokerMethod(), e);
      throw new LaunchFailedException(e);
    }
  }

  protected void preProcessConfigure() {
    if (this.config.getBookName().isPresent()) {
      String bookFolder = this.config.getBookName().get();
      File finalKeepDir = new File(this.config.getKeepDir().get(), bookFolder);
      this.config.setKeepDir(finalKeepDir.getAbsolutePath());
    }
  }

  /**
   * start to download action
   * 
   * @throws SecurityException
   * @throws IOException
   */
  protected void launch() throws SecurityException, IOException {
    if (config == null)
      throw new LostConfigureItemException("No configuration.");
    if (!config.getKeepDir().isPresent())
      throw new LostConfigureItemException("Lost configuration for save dir.");
    running = true;
    final WebClient webClient = createWebClient();
    try {
      referClient = webClient;
      webClient.getOptions().setThrowExceptionOnScriptError(false);
      webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
      webClient.getOptions().setTimeout(35000);
      if (config.needLogin().isPresent() && config.needLogin().get())
        loginAndAuth(webClient);
      startTimer();
      // bowser all pages
      travelWholePages(webClient);
      ProcessLogger.info("All pages has been download.");
      processNormalCompleted();
    } catch (ManualStopException e) {// manual stop exception happened
      ProcessLogger.info(CommonUtilities.getCurrentInvokerMethod(), "Manual stop successfully");
      processManualAbort(e);
    } catch (Exception e) {
      ProcessLogger.error(CommonUtilities.getCurrentInvokerMethod(), e.getMessage());
      processAbort(e);
    }
  }

  protected void processAbort(Exception e) {
    stop();
  }

  protected void processManualAbort(ManualStopException e) {
    stop();
  }

  protected void processNormalCompleted() {
    stop();
  }

  protected void startTimer() {
    // set timer to reset session timeout repeatedly, force re-login and auth
    stopTimer();
    sessionTimer = new Timer();
    sessionTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        sessionTimeout = true;
      }
    }, getSessionRefreshInterval(), getSessionRefreshInterval());
  }

  protected void stopTimer() {
    if (sessionTimer != null) {
      sessionTimer.cancel();
      sessionTimer.purge();
      sessionTimer = null;
    }
  }

  /**
   * return session force timeout
   * 
   * @return session timeout, unit is milliseconds
   */
  protected long getSessionRefreshInterval() {
    Result<Long> result = new Result<Long>(WebPagesConfigure.DEFAULT_SESSION_TIMEOUT);
    config.getSessionTimeout().ifPresent((timeout) -> {
      result.set(timeout);
    });
    return result.get();
  }

  /**
   * do login and authentication action
   * 
   * @param webClient {@link WebClient}
   * @throws FailingHttpStatusCodeException
   * @throws MalformedURLException
   * @throws IOException
   */
  protected void loginAndAuth(WebClient webClient)
      throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    webClient.getOptions().setJavaScriptEnabled(true);
    ProcessLogger.info("Open login page...");
    if (!config.getLoginPageUrl().isPresent())
      throw new LostConfigureItemException("Lost configuration for login page url.");
    if (!config.getLoginFormSelector().isPresent())
      throw new LostConfigureItemException("Lost configuration for login form selector.");
    if (!config.getLoginAuthInputName().isPresent()
        && !config.getLoginAuthInputSelector().isPresent())
      throw new LostConfigureItemException(
          "Lost configuration for the input name/selector of login name.");
    if (!config.getLoginAuthInputValue().isPresent())
      throw new LostConfigureItemException("Lost configuration for the input value of login name.");
    if (!config.getLoginAuthPassInputName().isPresent()
        && !config.getLoginAuthPassInputSelector().isPresent())
      throw new LostConfigureItemException(
          "Lost configuration for the input name/selector of login password.");
    if (!config.getLoginAuthPassInputValue().isPresent())
      throw new LostConfigureItemException(
          "Lost configuration for the input value of login password.");
    if (!config.getLoginSubmitBtnName().isPresent()
        && !config.getLoginSubmitBtnSelector().isPresent())
      throw new LostConfigureItemException(
          "Lost configuration for the submit name/selector of login submit.");
    checkRuning();
    // get login page
    HtmlPage loginPage = webClient.getPage(config.getLoginPageUrl().get());
    ProcessLogger.debug("Login page load success:" + (loginPage != null));
    // ProcessLogger.debug("Login page source:\n" + loginPage.getTextContent());
    // get login form element
    loginPage = processBeforeLogin(loginPage, webClient);
    HtmlForm loginForm = null;
    String formSel = config.getLoginFormSelector().get();
    DomNodeList<DomNode> elements = loginPage.getBody().querySelectorAll(formSel);
    ProcessLogger.debug("Login form in login page found(1):" + !elements.isEmpty());
    loginForm = elements.isEmpty() ? null : (HtmlForm) elements.get(0);
    if (loginForm == null) {
      int index = config.getLoginFormIndex();
      List<HtmlForm> forms = loginPage.getForms();
      ProcessLogger.debug("Login form in login page found(2):" + !forms.isEmpty());
      if (index >= 0 && forms.size() >= index) {
        loginForm = forms.get(index - 1);
      }
    }
    if (loginForm == null) {
      throw new IllegalArgumentException("Cannot found the login form.");
    }
    checkRuning();
    HtmlInput userName = null, userPass = null;
    HtmlElement button = null;
    ProcessLogger.debug(loginPage.asXml());
    if (config.getLoginAuthInputName().isPresent()) {
      userName = loginForm.getInputByName(config.getLoginAuthInputName().get());
    } else if (config.getLoginAuthInputSelector().isPresent()) {
      userName = loginForm.querySelector(config.getLoginAuthInputSelector().get());
    }
    if (config.getLoginAuthPassInputName().isPresent()) {
      userPass = loginForm.getInputByName(config.getLoginAuthPassInputName().get());
    } else if (config.getLoginAuthPassInputSelector().isPresent()) {
      userPass = loginForm.querySelector(config.getLoginAuthPassInputSelector().get());
    }
    if (config.getLoginSubmitBtnName().isPresent()) {
      button = loginForm.getInputByName(config.getLoginSubmitBtnName().get());
    } else if (config.getLoginSubmitBtnSelector().isPresent()) {
      button = loginForm.querySelector(config.getLoginSubmitBtnSelector().get());
    }
    if (userName == null) {
      throw new IllegalArgumentException("Cannot found the input name/selector of login name.");
    }
    if (userPass == null) {
      throw new IllegalArgumentException("Cannot found the input name/selector of login password.");
    }
    if (button == null) {
      throw new IllegalArgumentException("Cannot found the input name/selector of login submit.");
    }
    userName.setValueAttribute(config.getLoginAuthInputValue().get());
    userPass.setValueAttribute(config.getLoginAuthPassInputValue().get());
    ProcessLogger.info("Simulate the login action...");
    final HtmlPage afterSubmitPage = button.click();
    processAfterSubmit(afterSubmitPage, webClient);
    ProcessLogger.info("Login success.");
  }

  protected HtmlPage processBeforeLogin(HtmlPage loginPage, WebClient webClient) {
    return loginPage;
  }

  protected void processAfterSubmit(HtmlPage afterSubmitPage, WebClient webClient) {}

  protected final void travelWholePages(WebClient webClient) {
    if (config.getCatalogPageUrl().isPresent()) {
      catalogResolver(webClient);
    } else if (config.getStartChainPageUrl().isPresent()) {
      chainResolver(webClient);
    } else {
      ProcessLogger.warn("No url configuration.");
    }
  }

  /**
   * bowser web site by catalog page, which in this page will list all other pages link
   * 
   * @param webClient {@link WebClient}
   */
  protected void catalogResolver(WebClient webClient) {
    String catalogURL = config.getCatalogPageUrl().get();
    checkRuning();
    ProcessLogger.info("Go to download the catalog page:" + catalogURL);
    if (!config.getPageLinkOfCatalogSelector().isPresent())
      throw new LostConfigureItemException(
          "Lost configuration for page link selector on catalog page.");
    try {
      checkRuning();
      HtmlPage catalogPage = webClient.getPage(catalogURL);
      HtmlPageTransfer htmlPageTransfer =
          getHtmlPageTransfer(catalogPage.cloneNode(true), config.getKeepDir().get());
      htmlPageTransfer.transfer();
      DomNodeList<DomNode> links =
          catalogPage.getBody().querySelectorAll(config.getPageLinkOfCatalogSelector().get());

      List<DomNode> nodes = links.stream().filter((domNode) -> {
        DomElement el = (DomElement) domNode;
        return el.hasAttribute("href") && !el.getAttribute("href").isEmpty()
            && !el.getAttribute("href").startsWith("#");
      }).collect(Collectors.toList());
      ListIterator<DomNode> iter = nodes.listIterator();
      while (running && iter.hasNext()) {
        checkRuning();
        DomElement el = (DomElement) iter.next();
        String relativeUrl = el.getAttribute("href");
        HtmlPage currentPage = null;
        ProcessLogger.debug(relativeUrl);
        try {
          reLoginIfNecessary(webClient);
          URL currentUrl = catalogPage.getFullyQualifiedUrl(relativeUrl);
          ProcessLogger.debug(currentUrl.toString());
          currentPage = webClient.getPage(currentUrl);
          ProcessLogger.info("Go to download page:" + currentUrl);
          HtmlPageTransfer pageTransfer =
              getHtmlPageTransfer(currentPage, config.getKeepDir().get());
          pageTransfer.transfer();
        } catch (Exception e) {
          ProcessLogger.error(CommonUtilities.getCurrentInvokerMethod(), e.getMessage());
        } finally {
          if (currentPage != null)
            currentPage.cleanUp();
          currentPage = null;
        }
      }
    } catch (FailingHttpStatusCodeException | IOException | LostConfigureItemException e) {
      ProcessLogger.fatal(CommonUtilities.getCurrentInvokerMethod(), e.getMessage());
    }
  }

  /**
   * return page download helper implementation
   * 
   * @param htmlPage {@link HtmlPage}
   * @param file {@link File}
   * @return {@link HtmlPageTransfer}
   */
  protected HtmlPageTransfer getHtmlPageTransfer(HtmlPage htmlPage, File file) {
    return BatchHtmlPageTransfer.create(htmlPage, file);
  }

  /**
   * bowser web site by a first page, then go next page by a common link
   * 
   * @param webClient {@link WebClient}
   */
  protected void chainResolver(WebClient webClient) {
    String nextURL = config.getStartChainPageUrl().get();
    HtmlAnchor nextAnchor = null;
    checkRuning();
    ProcessLogger.info("Go to download the start page:" + nextURL);
    if (!config.getNextChainPageAnchorSelector().isPresent()) {// enhance for no next anchor
      HtmlPage currentPage;
      try {
        currentPage = webClient.getPage(nextURL);
        HtmlPageTransfer htmlPageTransfer =
            getHtmlPageTransfer(currentPage, config.getKeepDir().get());
        htmlPageTransfer.transfer();
        ProcessLogger.info("Go to download next page:" + nextURL);
      } catch (FailingHttpStatusCodeException | IOException e) {
        ProcessLogger.fatal(CommonUtilities.getCurrentInvokerMethod(), e.getMessage());
      }
      return;
      // throw new LostConfigureItemException("Lost configuration for next page link
      // selector on each page.");
    }
    do {
      checkRuning();
      HtmlPage currentPage = null;
      try {
        reLoginIfNecessary(webClient);
        currentPage = webClient.getPage(nextURL);
        DomNode anchors =
            currentPage.getBody().querySelector(config.getNextChainPageAnchorSelector().get());
        nextAnchor = (HtmlAnchor) anchors;
        if (nextAnchor != null) {// issue for no found next anchor
          nextURL = currentPage.getFullyQualifiedUrl(nextAnchor.getHrefAttribute()).toString();
        }
        HtmlPageTransfer pageTransfer = getHtmlPageTransfer(currentPage, config.getKeepDir().get());
        pageTransfer.transfer();
        ProcessLogger.info("Go to download next page:" + nextURL);
      } catch (Exception e) {
        ProcessLogger.fatal(CommonUtilities.getCurrentInvokerMethod(), e.getMessage());
        nextAnchor = null;
      } finally {
        if (currentPage != null)
          currentPage.cleanUp();
      }
    } while (nextAnchor != null && running);
  }

  /**
   * return a instance of {@link WebClient}
   * 
   * @return {@link WebClient}
   */
  protected WebClient createWebClient() {
    checkRuning();
    Result<WebClient> result = Result.valueOf(null);
    config.getProxy().ifPresent((proxy) -> {
      WebClient webClient =
          new WebClient(config.getBrowserVersion(), proxy.getHost().trim(), proxy.getPort());
      result.set(webClient);
      if (proxy.hasAuth()) {
        final DefaultCredentialsProvider credentialsProvider =
            (DefaultCredentialsProvider) webClient.getCredentialsProvider();
        credentialsProvider.addCredentials(proxy.getAuthName().trim(), proxy.getAuthPass().trim());
      }
    });

    if (result.isNull()) {
      result.set(new WebClient(config.getBrowserVersion()));
    }
    return result.get();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.openthinks.others.webpages.Launch#stop()
   */
  @Override
  public final void stop() {
    running = false;
    if (referClient != null) {
      referClient.close();
      referClient = null;
    }
    stopTimer();
  }

  protected void checkRuning() {
    if (running == false) {
      throw new ManualStopException();
    }
  }

  /**
   * check current is need do re-login and auth or not.
   * 
   * @param webClient {@link WebClient}
   * @throws FailingHttpStatusCodeException
   * @throws MalformedURLException
   * @throws IOException
   */
  protected void reLoginIfNecessary(WebClient webClient)
      throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    if (config.needLogin().isPresent() && config.needLogin().get()) {
      if (sessionTimeout == true) {
        loginAndAuth(webClient);
        sessionTimeout = false;
      }
    }
  }
}
