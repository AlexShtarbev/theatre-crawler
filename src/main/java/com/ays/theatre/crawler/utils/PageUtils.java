package com.ays.theatre.crawler.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;

public class PageUtils {
    private static final Logger LOG = Logger.getLogger(PageUtils.class);
    public static final int MAX_NUM_RETRIES = 10;

    public static Document navigateWithRetry(String url) {
        var stopWatch = new StopWatch();
        stopWatch.start();
        Document doc = null;
        for (int i = 0; i < MAX_NUM_RETRIES && doc == null; i++) {
            try {
                var connection = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1)").timeout(10_000);
                doc = connection.get();
            } catch (Exception ex) {
                LOG.error(ex);
                LOG.info(String.format("Will retry failed navigation to %s [%d remaining]", url, MAX_NUM_RETRIES - i));
            }
        }
        stopWatch.stop();
        LOG.info(String.format("Navigation to %s took %dms", url, stopWatch.getTime()));
        return doc;
    }

    public static HtmlPage navigateWithRetry(WebClient webClient, String url) {
        var stopWatch = new StopWatch();
        stopWatch.start();
        HtmlPage doc = null;
        for (int i = 0; i < MAX_NUM_RETRIES && doc == null; i++) {
            try {
                doc = webClient.getPage(url);
            } catch (Exception ex) {
                LOG.error(ex);
                LOG.info(String.format("Will retry failed navigation to %s [%d remaining]", url, MAX_NUM_RETRIES - i));
            }
        }
        stopWatch.stop();
        LOG.info(String.format("Navigation to %s took %dms", url, stopWatch.getTime()));
        return doc;
    }

}
