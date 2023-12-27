package com.ays.theatre.crawler.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.Logger;

import com.microsoft.playwright.Page;

public class PageUtils {
    private static final Logger LOG = Logger.getLogger(PageUtils.class);
    public static final int PAGE_DEFAULT_TIMEOUT = 120_000;
    public static final int MAX_NUM_RETRIES = 10;

    public static void navigateWithRetry(Page currentPage, String url) {
        var stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < MAX_NUM_RETRIES; i++) {
            try {
                currentPage.setDefaultTimeout(PAGE_DEFAULT_TIMEOUT);
                currentPage.navigate(url);
            } catch (Exception ex) {
                LOG.error(ex);
                LOG.info(String.format("Will retry failed navigation to %s [%d remaining]", url, MAX_NUM_RETRIES - i));
            }
        }
        stopWatch.stop();
        LOG.info(String.format("Navigation to %s took %dms", url, stopWatch.getTime()));
    }

}
