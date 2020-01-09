/**
 * 
 */
package com.openthinks.others.webpages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import com.openthinks.libs.utilities.CommonUtilities;
import com.openthinks.libs.utilities.logger.ProcessLogger;
import com.openthinks.others.webpages.conf.DownloadBookTaskInfo;
import com.openthinks.others.webpages.conf.WebPagesConfigure;

/**
 * Start entry point class
 * 
 * @author dailey.dai@openthinks.com
 *
 */
public final class Bootstarp {

  private static Launch INSTANCE = null;

  public static void main(String[] args) {
    // load first available implementation by ServiceLoader
    ServiceLoader<Launch> serviceLoader = ServiceLoader.load(Launch.class);
    Iterator<Launch> launchIterator = serviceLoader.iterator();
    if (!launchIterator.hasNext()) {
      return;
    }

    INSTANCE = launchIterator.next();
    // load main configuration from arguments
    WebPagesConfigure config = initialConfig(args);
    if (config == null) {
      return;
    }
    if (hasGroupTask(config)) {
      doGroupTask(config);
    } else {
      try {
        INSTANCE.setConfig(config);
        INSTANCE.start();
      } catch (Exception e) {
        ProcessLogger.fatal(CommonUtilities.getCurrentInvokerMethod(), e);
      } finally {
        INSTANCE.stop();
      }
    }
  }

  private static WebPagesConfigure initialConfig(String[] args) {
    String config_path = null;
    if (args != null && args.length > 0) {
      if ("-help".equalsIgnoreCase(args[0])) {
        showUsage();
        return null;
      }
      if ("-config".equalsIgnoreCase(args[0])) {
        if (args.length >= 2) {
          config_path = args[1];
        } else {
          System.out.println("miss configure file path!");
          showUsage();
          return null;
        }
      }
    } else {
      showUsage();
      return null;
    }

    try {
      return WebPagesConfigure.readXML(new FileInputStream(config_path));
    } catch (IOException e) {
      ProcessLogger.error(e.getMessage());
    }
    return null;
  }

  private static void showUsage() {
    System.out.println("Usage: <option> [args]");
    System.out.println("option:");
    System.out.println("\t -help");
    System.out.println("\t -config");
    System.out.println("example:");
    System.out.println(" -config W:\\keeper\\configure.xml");
  }

  private static void doGroupTask(WebPagesConfigure config) {
    final String groupTaskDefDir = config.getGroupTaskDir().get();
    if (groupTaskDefDir == null || groupTaskDefDir.trim().length() == 0) {
      ProcessLogger.fatal("Configure item {0} cannot be empty.",
          WebPagesConfigure.DOWNLOADGROUPTASKDIR);
      return;
    }
    final File dir = new File(groupTaskDefDir.trim());
    if (!dir.exists()) {
      ProcessLogger.fatal("Configure item {0}:{1} is not exist in local file system.",
          WebPagesConfigure.DOWNLOADGROUPTASKDIR, groupTaskDefDir);
      return;
    }
    final File[] acceptXmlFiles = dir.listFiles((d, n) -> {
      return n.endsWith(DownloadBookTaskInfo.CONFIG_XML);
    });
    final File[] acceptPropFiles = dir.listFiles((d, n) -> {
      return n.endsWith(DownloadBookTaskInfo.CONFIG_PROPERTIES);
    });
    final List<DownloadBookTaskInfo> taskInfos = new ArrayList<>();
    for (File xml : acceptXmlFiles) {
      try {
        DownloadBookTaskInfo task = DownloadBookTaskInfo.readXML(xml);
        taskInfos.add(task);
      } catch (IOException e) {
        ProcessLogger.warn("Failed to read task configuration:{0} by reason:{1}", xml, e);
      }
    }
    for (File prop : acceptPropFiles) {
      try {
        DownloadBookTaskInfo task = DownloadBookTaskInfo.readProps(prop);
        taskInfos.add(task);
      } catch (IOException e) {
        ProcessLogger.warn("Failed to read task configuration:{0} by reason:{1}", prop, e);
      }
    }
    final List<WebPagesConfigure> configList = new ArrayList<>();
    taskInfos.stream().filter(ti -> {
      if (ti.getIsProcessed().isPresent()) {
        return !ti.getIsProcessed().get().trim().equalsIgnoreCase("true");
      } else {
        return true;
      }
    }).forEach(ti -> {
      WebPagesConfigure cf = (WebPagesConfigure) config.clone();
      cf.setBookTaskInfo(ti);
      ti.getBookName().ifPresent(val -> cf.setBookName(val));
      ti.getCatalogPageUrl().ifPresent(val -> cf.setCatalogPageUrl(val));
      cf.setCatalogPageUrl(ti.getCatalogPageUrl().orElse(null));
      ti.getPageLinkOfCatalogSelector().ifPresent(val -> cf.setPageLinkOfCatalogSelector(val));
      configList.add(cf);
    });

    for (WebPagesConfigure cf : configList) {
      try {
        INSTANCE.setConfig(cf);
        INSTANCE.start();
        cf.getBookTaskInfo().ifPresent(ti -> {
          ti.setIsProcessed("true");
        });
      } catch (Exception e) {
        ProcessLogger.fatal(CommonUtilities.getCurrentInvokerMethod(), e);
        cf.getBookTaskInfo().ifPresent(ti -> {
          ti.setIsProcessed("false");
        });
      } finally {
        INSTANCE.stop();
      }
      cf.getBookTaskInfo().ifPresent(ti -> {
        ti.keep();
      });
    }

  }

  private static boolean hasGroupTask(WebPagesConfigure config) {
    return config.getGroupTaskDir().isPresent();
  }
}
