package com.zhaow.utils;

import com.google.gson.*;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author zhaow
 */
public class JsonUtils {
    public JsonUtils() {
    }

    public static boolean isValidJson(String json) {
        if (StringUtil.isEmptyOrSpaces(json)) {
            return false;
        }

        return isValidJsonObject(json) || isValidJsonArray(json);
    }

    public static String format(String str) {
        // JsonParser.parseString replaces the deprecated instance-based new JsonParser().parse()
        JsonElement parse = JsonParser.parseString(str);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(parse);
    }

    private static boolean isGsonFormat(String targetStr,Class clazz) {
        try {
            new Gson().fromJson(targetStr,clazz);
            return true;
        } catch(JsonSyntaxException ex) {
            return false;
        }
    }

    public static boolean isValidJsonObject(String targetStr){
        return isGsonFormat(targetStr,JsonObject.class);
    }

    public static boolean isValidJsonArray(String targetStr){
        return isGsonFormat(targetStr,JsonArray.class);
    }

}