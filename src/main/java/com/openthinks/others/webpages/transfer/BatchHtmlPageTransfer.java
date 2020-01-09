package com.openthinks.others.webpages.transfer;

import java.io.File;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * add common folder for common resource
 * 
 * @author dailey.dai@openthinks.com
 *
 */
public class BatchHtmlPageTransfer extends HtmlPageTransfer {
  public static final String COMMON_RESOURCE_DIR = "common";
  private File commonDir = null;

  private BatchHtmlPageTransfer(HtmlPage htmlPage, File keepDir) {
    super(htmlPage, keepDir);
    this.commonDir = new File(keepDir.getParentFile(), COMMON_RESOURCE_DIR);
  }

  public static BatchHtmlPageTransfer create(HtmlPage htmlPage, File keepDir) {
    return new BatchHtmlPageTransfer(htmlPage, keepDir);
  }



  @Override
  public File getJsKeepDir() {
    return new File(commonDir, RESOURCE_SCRIPT_DIR);
  }

  @Override
  public String getJsPath() {
    return "../common/" + RESOURCE_SCRIPT_DIR;
  }

  @Override
  public File getCssKeepDir() {
    return new File(commonDir, RESOURCE_STYLE_DIR);
  }

  @Override
  public String getCssPath() {
    return "../common/" + RESOURCE_STYLE_DIR;
  }

}
