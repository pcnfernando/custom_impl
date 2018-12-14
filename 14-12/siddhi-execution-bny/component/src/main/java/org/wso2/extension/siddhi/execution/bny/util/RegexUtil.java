package org.wso2.extension.siddhi.execution.bny.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class RegexUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RegexUtil.class);
    public static String runRegex(String regex, String content, int groupNo) {
        String result = "";
        Pattern regexPattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher regexMatcher = regexPattern.matcher(content);
        if (regexMatcher.find()) {
            result = regexMatcher.group(groupNo);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Result for regex: %s is : %s", regex, result));
            }
        } else {
            LOG.error("No matches found for " + regex);
        }
        return result;
    }
}
