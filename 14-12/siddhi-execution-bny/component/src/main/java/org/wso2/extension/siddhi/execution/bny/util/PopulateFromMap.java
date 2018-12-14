package org.wso2.extension.siddhi.execution.bny.util;

import java.util.HashMap;
import java.util.Map;

/**
 *Util methods for the bny extensions
 */
public class PopulateFromMap {
    private static final String ALL_PLACEHOLDERS_REGEX = "\\$\\*.*?\\^";
    private static final String PLACEHOLDER_START_CHARACTERS = "\\$\\*";
    private static final String PLACEHOLDER_END_CHARACTERS = "\\^";
    private static final String ESCAPE_CHARACTER = "$";
    private static final String REPLACE_STR = "";

    public static String populateTemplateFromMap(Map propertiesMap, String template) {
        HashMap<String, String> map = (HashMap<String, String>) propertiesMap;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String regex = PLACEHOLDER_START_CHARACTERS.concat(entry.getKey()).concat(PLACEHOLDER_END_CHARACTERS);
            String val = entry.getValue();
            if (val.contains(ESCAPE_CHARACTER)) {
                val = "\\".concat(val);
            }
            template = template.replaceAll(regex, val);
        }
        template = template.replaceAll(ALL_PLACEHOLDERS_REGEX, REPLACE_STR);
        return template;
    }

    public static String populateTemplateFromMap(Map propertiesMap, String template, String startRegx, String endRegx) {
        HashMap<String, String> map = (HashMap<String, String>) propertiesMap;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String regex = startRegx.concat(entry.getKey()).concat(endRegx);
            String val = entry.getValue();
            if (val.contains(ESCAPE_CHARACTER)) {
                val = "\\".concat(val);
            }
            template = template.replaceAll(regex, val);
        }
        return template;
    }
}
