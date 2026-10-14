package com.zhaow.restful.method.action;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
// profile.active != null  // YamlPropertySourceLoader extends PropertySourceLoader .load

// PropertySourcesLoader.load 配置文件加载类
// 路径location：[file:./config/, file:./, classpath:/config/, classpath:/]
//文件name：bootstrap，application
//后缀：[properties, xml, yml, yaml]
//applicationConfig: [classpath:/application.yml]#prod

// 如果存在 activeProfile spring.profiles.active 存在，判断是否存在 application-activeProfile. 文件，
// 如果存在，判断是否存在设置，不存在则忽略
//最终可能优先级 application.properties>application.yml>bootstrap.propertis>bootstrap.yml
//路径：classpath:/(resource)>classpath:/config/
public class PropertiesHandler {

    private static final Logger LOG = Logger.getInstance(PropertiesHandler.class);

    public String[] getFileExtensions() { //优先级
        return new String[]{"properties", "yml", "yaml"};
    }

    public String[] getConfigFiles() { // 优先级
        return new String[]{"application", "bootstrap"};
    }

    String SPRING_PROFILE = "spring.profiles.active";

    String placeholderPrefix = "${";
    String valueSeparator = ":";
    String placeholderSuffix = "}";

    final Module module;

    // Parsed config files, keyed by file name. Profile lookup is repeated per key otherwise.
    private final Map<String, Map<String, Object>> parsedFiles = new HashMap<>();

    public PropertiesHandler(Module module) {
        this.module = module;
    }

    public String getServerPort() {
        return resolve("server.port");
    }

    public String getContextPath() {
        return resolve("server.context-path");
    }

    public String getProperty(String propertyKey) {
        return resolve(propertyKey);
    }

    /**
     * Profile-specific config wins over the base config; missing keys fall through.
     */
    @NotNull
    private String resolve(String propertyKey) {
        String activeProfile = findPropertyValue(SPRING_PROFILE, null);

        String value = activeProfile == null ? null : findPropertyValue(propertyKey, activeProfile);
        if (value == null) {
            value = findPropertyValue(propertyKey, null);
        }
        return value != null ? value : "";
    }



    /* 暂时不考虑路径问题，默认找到的第一文件 */
    @Nullable
    private String findPropertyValue(String propertyKey, @Nullable String activeProfile) {
        String profile = activeProfile != null ? "-" + activeProfile : "";
        for (String conf : getConfigFiles()) {
            for (String ext : getFileExtensions()) {
                Map<String, Object> properties = loadConfigFile(conf + profile + "." + ext);
                if (properties == null) {
                    continue;
                }
                Object valueObj = properties.get(propertyKey);
                // Not found in this file: keep walking the fallback chain.
                if (valueObj == null) {
                    continue;
                }
                String raw = valueObj instanceof String ? (String) valueObj : valueObj.toString();
                return cleanPlaceholderIfExist(raw);
            }
        }

        return null;
    }

    /**
     * One lookup walks up to 12 config files and every getter repeats it, so parsed files
     * are memoised for the lifetime of this handler.
     */
    @Nullable
    private Map<String, Object> loadConfigFile(String configFile) {
        if (parsedFiles.containsKey(configFile)) {
            return parsedFiles.get(configFile);
        }
        Map<String, Object> parsed = configFile.endsWith(".properties")
                ? loadPropertiesFromConfigFile(configFile)
                : getPropertiesMapFromYamlFile(configFile);
        parsedFiles.put(configFile, parsed);
        return parsed;
    }

    @Nullable
    private Map<String, Object> loadPropertiesFromConfigFile(String configFile) {
        PsiFile applicationPropertiesFile = findPsiFileInModule(configFile);
        if (applicationPropertiesFile == null) {
            return null;
        }
        Properties properties = loadPropertiesFromText(applicationPropertiesFile.getText());
        Map<String, Object> values = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return values.isEmpty() ? null : values;
    }

    @NotNull
    private Properties loadPropertiesFromText(String text) {
        Properties prop = new Properties();
        try {
            prop.load(new StringReader(text));
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("Failed to parse properties content", e);
        }
        return prop;
    }

    private String cleanPlaceholderIfExist(String value) {
        // server.port=${PORT:8080}
        if (value != null && value.contains(placeholderPrefix) && value.contains(valueSeparator)) {
            String[] split = value.split(valueSeparator);


            if (split.length > 1) {
                value = split[1].replace(placeholderSuffix, "");
            }
//            value = value.replace(placeholderPrefix,"").replace(placeholderSuffix,"");
        }
        return value;
    }


    private Map<String, Object> getPropertiesMapFromYamlFile(String configFile) {
        PsiFile applicationPropertiesFile = findPsiFileInModule(configFile);
        if (applicationPropertiesFile != null) {
            Yaml yaml = new Yaml();

            String yamlText = applicationPropertiesFile.getText();
            try {
                Map<String, Object> merged = new LinkedHashMap<>();
                Iterable<Object> docs = yaml.loadAll(yamlText);
                for (Object doc : docs) {
                    if (!(doc instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> ymlPropertiesMap = (Map<String, Object>) doc;
                    Map<String, Object> flattenedMap = getFlattenedMap(ymlPropertiesMap);
                    // Later docs override earlier docs, same as Spring-style layered behavior.
                    merged.putAll(flattenedMap);
                }
                return merged.isEmpty() ? null : merged;
            } catch (Exception e) {
                LOG.warn("Failed to parse " + configFile, e);
                return null;
            }
        }
        return null;
    }


    private PsiFile findPsiFileInModule(String fileName) {
        PsiFile psiFile = null;
        PsiFile[] applicationProperties = FilenameIndex.getFilesByName(module.getProject(),
                fileName,
                GlobalSearchScope.moduleScope(module));

        if (applicationProperties.length > 0) {
            psiFile = applicationProperties[0];
        }

        return psiFile;
    }

    /**
     * ref: org.springframework.beans.factory.config.YamlProcessor
     */
    protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap();
        this.buildFlattenedMap(result, source, null);
        return result;
    }

    private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        Iterator iterator = source.entrySet().iterator();

        while(true) {
            while(iterator.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry)iterator.next();
                String key = entry.getKey();
                if (path != null && !path.isBlank()) {
                    if (key.startsWith("[")) {
                        key = path + key;
                    } else {
                        key = path + '.' + key;
                    }
                }

                Object value = entry.getValue();
                if (value instanceof String) {
                    result.put(key, value);
                } else if (value instanceof Map) {
                    Map<String, Object> map = (Map)value;
                    this.buildFlattenedMap(result, map, key);
                } else if (value instanceof Collection) {
                    Collection<Object> collection = (Collection)value;
                    int count = 0;
                    Iterator var10 = collection.iterator();

                    while(var10.hasNext()) {
                        Object object = var10.next();
                        this.buildFlattenedMap(result, Collections.singletonMap("[" + count++ + "]", object), key);
                    }
                } else {
                    result.put(key, value != null ? value : "");
                }
            }

            return;
        }
    }

}
