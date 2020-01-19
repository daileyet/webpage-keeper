package com.openthinks.others.webpages.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Optional;
import com.openthinks.libs.utilities.logger.ProcessLogger;

/**
 * 
 * @author dailey.dai@openthinks.com
 *
 */
public class DownloadBookTaskInfo extends Config {
  private static final long serialVersionUID = -2676260790818380174L;

  public static final String CONFIG_PROPERTIES = ".properties";
  public static final String CONFIG_XML = ".xml";
  @ConfigDesc("[required]download pages name")
  public static final String BOOKNAME = "pages-name";
  @ConfigDesc("[option when the first page url was configured]The catalog page url")
  public static final String CATALOGPAGEURL = "pages-catalog-url";
  @ConfigDesc("[option when the first page url was configured]The css selector for each download page anchor on catalog page")
  public static final String PAGELINKOFCATALOGSELECTOR = "catalog-pagelinks-selector";
  @ConfigDesc("[option when the catalog page url was configured]The first page url")
  public static final String STARTCHAINPAGEURL = "pages-first-url";
  @ConfigDesc("[option when the catalog page url was configured]The css selector for next chain page anchor on each page")
  public static final String NEXTCHAINPAGEANCHORSELECTOR = "pages-next-anchor-selector";
  public static final String DOWNLOADED = "is-processed";

  private File storeFile = null;

  private Optional<String> getProp(String propertyName) {
    try {
      return Optional.of(this.getProperty(propertyName));
    } catch (Exception e) {
    }
    return Optional.empty();
  }

  public Optional<String> getBookName() {
    return getProp(BOOKNAME);
  }

  public void setBookName(String value) {
    setProperty(BOOKNAME, value);
  }

  public Optional<String> getCatalogPageUrl() {
    return getProp(CATALOGPAGEURL);
  }

  public void setCatalogPageUrl(String value) {
    setProperty(CATALOGPAGEURL, value);
  }

  public Optional<String> getPageLinkOfCatalogSelector() {
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
    return getProp(NEXTCHAINPAGEANCHORSELECTOR);
  }

  public void setNextChainPageAnchorSelector(String value) {
    setProperty(NEXTCHAINPAGEANCHORSELECTOR, value);
  }

  public Optional<String> getIsProcessed() {
    return getProp(DOWNLOADED);
  }

  public void setIsProcessed(String value) {
    setProperty(DOWNLOADED, value);
  }

  public static final DownloadBookTaskInfo create() {
    return new DownloadBookTaskInfo();
  }

  public static final DownloadBookTaskInfo readXML(File file)
      throws InvalidPropertiesFormatException, IOException {
    DownloadBookTaskInfo instance = DownloadBookTaskInfo.create();
    instance.loadFromXML(new FileInputStream(file));
    instance.setStoreFile(file);
    return instance;
  }

  public static final DownloadBookTaskInfo readProps(File file) throws IOException {
    DownloadBookTaskInfo instance = DownloadBookTaskInfo.create();
    instance.load(new FileInputStream(file));
    instance.setStoreFile(file);
    return instance;
  }

  void setStoreFile(File storeFile) {
    this.storeFile = storeFile;
  }



  @Override
  public String toString() {
    return getBookName().orElse("");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    DownloadBookTaskInfo other = (DownloadBookTaskInfo) obj;
    if (!getCatalogPageUrl().isPresent()) {
      if (other.getCatalogPageUrl().isPresent())
        return false;
    } else if (!getCatalogPageUrl().get().equals(other.getCatalogPageUrl().orElse("")))
      return false;
    return true;
  }

  public final boolean keep() {
    if (this.storeFile != null) {
      Date updateTime = new Date();
      String fileName = this.storeFile.getName();
      String comment = updateTime.toString();
      try {
        if (fileName.endsWith(CONFIG_XML)) {
          this.storeToXML(new FileOutputStream(storeFile), comment);
        } else if (fileName.endsWith(CONFIG_PROPERTIES)) {
          this.store(new FileOutputStream(storeFile), comment);
        } else {
          return false;
        }
        return true;
      } catch (IOException e) {
        ProcessLogger.error("Failed to save task configuration to file:{0} by reason:{1}", fileName,
            e);
        return false;
      }
    }
    return false;
  }
}
