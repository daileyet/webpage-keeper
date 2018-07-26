package com.openthinks.others.webpages;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.openthinks.libs.utilities.CommonUtilities;
import com.openthinks.libs.utilities.Result;
import com.openthinks.libs.utilities.logger.ProcessLogger;
import com.openthinks.others.webpages.exception.LostConfigureItemException;
import com.openthinks.others.webpages.exception.ManualStopException;

/**
 * The web pages download launcher
 * 
 * @author dailey.yet@outlook.com
 *
 */
public abstract class WebPagesLaunch {

	protected WebPagesConfigure config;

	private transient WebClient referClient;

	private volatile boolean running = false;

	protected volatile boolean sessionTimeout = false;
	
	protected Timer sessionTimer = new Timer();

	public WebPagesLaunch() {
		super();
	}

	public WebPagesLaunch(WebPagesConfigure config) {
		super();
		this.config = config;
	}

	/**
	 * start to download action
	 * 
	 * @throws SecurityException
	 * @throws IOException
	 */
	public final void launch() throws SecurityException, IOException {
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
			//set timer to reset session timeout repeatedly, force re-login and auth
			sessionTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					sessionTimeout = true;
				}
			}, getSessionRefreshInterval(), getSessionRefreshInterval());
			//bowser all pages
			travelWholePages(webClient);
			ProcessLogger.info("All pages has been download.");
		} catch (ManualStopException e) {//manual stop exception happend
			ProcessLogger.info(CommonUtilities.getCurrentInvokerMethod(), "Manual stop successfully");
		} catch (Exception e) {
			ProcessLogger.error(CommonUtilities.getCurrentInvokerMethod(), e.getMessage());
		} finally {
			if (webClient != null)
				webClient.close();
			running = false;
			sessionTimer.cancel();
		}
	}

	/**
	 * return session force timeout
	 * @return session timeout, unit is milliseconds
	 */
	protected long getSessionRefreshInterval() {
		Result<Long> result = new Result<Long>(WebPagesConfigure.DEFAULT_SESSION_TIMEOUT);
		config.getSessionTimeout().ifPresent((timeout)->{
			result.set(timeout);
		});
		return result.get();
	}

	/**
	 * do login and authentication action
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
		if (!config.getLoginAuthInputName().isPresent())
			throw new LostConfigureItemException("Lost configuration for the input name of login name.");
		if (!config.getLoginAuthInputValue().isPresent())
			throw new LostConfigureItemException("Lost configuration for the input value of login name.");
		if (!config.getLoginAuthPassInputName().isPresent())
			throw new LostConfigureItemException("Lost configuration for the input name of login password.");
		if (!config.getLoginAuthPassInputValue().isPresent())
			throw new LostConfigureItemException("Lost configuration for the input value of login password.");
		if (!config.getLoginSubmitBtnName().isPresent())
			throw new LostConfigureItemException("Lost configuration for the input name of login submit.");
		checkRuning();
		//get login page
		final HtmlPage loginPage = webClient.getPage(config.getLoginPageUrl().get());
		ProcessLogger.debug("Login page load success:" + (loginPage != null));
//		ProcessLogger.debug("Login page source:\n" + loginPage.getTextContent());
		//get login form element
		HtmlForm loginForm = null;
		String formSel = config.getLoginFormSelector().get();
		DomNodeList<DomNode> elements = loginPage.getBody().querySelectorAll(formSel);
		ProcessLogger.debug("Login form in login page found(1):" + !elements.isEmpty());
		loginForm = elements.isEmpty() ? null : (HtmlForm) elements.get(config.getLoginFormIndex());
		if (loginForm == null) {
			int index = config.getLoginFormIndex();
			List<HtmlForm> forms = loginPage.getForms();
			ProcessLogger.debug("Login form in login page found(2):" + !forms.isEmpty());
			if (index >= 0 && forms.size() > index) {
				loginForm = forms.get(0);
			}
		}
		if (loginForm == null) {
			throw new IllegalArgumentException("Cannot found the login form.");
		}
		checkRuning();
		HtmlInput button = loginForm.getInputByName(config.getLoginSubmitBtnName().get());
		HtmlTextInput userName = loginForm.getInputByName(config.getLoginAuthInputName().get());
		userName.setValueAttribute(config.getLoginAuthInputValue().get());
		HtmlPasswordInput userPass = loginForm.getInputByName(config.getLoginAuthPassInputName().get());
		userPass.setValueAttribute(config.getLoginAuthPassInputValue().get());
		ProcessLogger.info("Simulate the login action...");
		button.click();
		ProcessLogger.info("Login success.");
	}

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
	 * @param webClient {@link WebClient}
	 */
	protected void catalogResolver(WebClient webClient) {
		String catalogURL = config.getCatalogPageUrl().get();
		checkRuning();
		ProcessLogger.info("Go to download the catalog page:" + catalogURL);
		if (!config.getPageLinkOfCatalogSelector().isPresent())
			throw new LostConfigureItemException("Lost configuration for page link selector on catalog page.");
		try {
			checkRuning();
			HtmlPage catalogPage = webClient.getPage(catalogURL);
			HtmlPageTransfer htmlPageTransfer = getHtmlPageTransfer(catalogPage.cloneNode(true),
					config.getKeepDir().get());
			htmlPageTransfer.transfer();
			DomNodeList<DomNode> links = catalogPage.getBody()
					.querySelectorAll(config.getPageLinkOfCatalogSelector().get());

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
					HtmlPageTransfer pageTransfer = getHtmlPageTransfer(currentPage, config.getKeepDir().get());
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
	 * @param htmlPage {@link HtmlPage}
	 * @param file {@link File}
	 * @return {@link HtmlPageTransfer}
	 */
	public abstract HtmlPageTransfer getHtmlPageTransfer(HtmlPage htmlPage, File file);

	/**
	 * bowser web site by a first page, then go next page by a common link
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
				HtmlPageTransfer htmlPageTransfer = getHtmlPageTransfer(currentPage, config.getKeepDir().get());
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
				DomNode anchors = currentPage.getBody().querySelector(config.getNextChainPageAnchorSelector().get());
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
	 * @return {@link WebClient}
	 */
	protected WebClient createWebClient() {
		checkRuning();
		Result<WebClient> result = Result.valueOf(null);
		config.getProxy().ifPresent((proxy) -> {
			WebClient webClient = new WebClient(config.getBrowserVersion(), proxy.host.trim(), proxy.port);
			result.set(webClient);
			if (proxy.hasAuth()) {
				final DefaultCredentialsProvider credentialsProvider = (DefaultCredentialsProvider) webClient
						.getCredentialsProvider();
				credentialsProvider.addCredentials(proxy.auth.name.trim(), proxy.auth.pass.trim());
			}
		});

		if (result.isNull()) {
			result.set(new WebClient(config.getBrowserVersion()));
		}
		return result.get();
	}

	/**
	 * stop web page transfer
	 */
	public final void stop() {
		running = false;
		if (referClient != null) {
			referClient.close();
			referClient = null;
		}
	}

	protected void checkRuning() {
		if (running == false) {
			throw new ManualStopException();
		}
	}

	/**
	 * check current is need do re-login and auth or not.
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
