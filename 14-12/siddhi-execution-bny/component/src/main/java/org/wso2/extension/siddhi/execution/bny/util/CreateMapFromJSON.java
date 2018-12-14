package org.wso2.extension.siddhi.execution.bny.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *Util methods for the bny extensions
 */
public class CreateMapFromJSON {
    public static Object createMapFromJson(String data) {
        if (data instanceof String) {
            Map<Object, Object> map = new HashMap<Object, Object>();
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(data.toString());
            } catch (JSONException e) {
                throw new SiddhiAppRuntimeException(
                        "Cannot create JSON from '" + data.toString() + "' in create from json function", e);
            }
            return getMapFromJson(map, jsonObject);
        } else {
            throw new SiddhiAppRuntimeException("Data should be a string");
        }
    }

    private static Map<Object, Object> getMapFromJson(Map<Object, Object> map, JSONObject jsonObject) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = null;
            try {
                value = jsonObject.get(key);
            } catch (JSONException e) {
                throw new SiddhiAppRuntimeException(
                        "JSON '" + jsonObject + "'does not contain key '" + key + "' in create from json function", e);
            }
            if (value instanceof JSONObject) {
                value = getMapFromJson(new HashMap<Object, Object>(), (JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }
}
