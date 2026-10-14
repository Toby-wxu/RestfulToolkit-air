package com.zhaow.restful.method.action;


import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

//PathAndQuery  AbsolutePath AbsoluteUri Query
public class ModuleHelper {
    Module module;

    // URL
    private static final String SCHEME = "http://"; //PROTOCOL
    private static final String HOST = "localhost";
    private static final String PORT = "8080"; // int
    public static final String DEFAULT_URI = SCHEME + HOST + ":" + PORT;

    PropertiesHandler propertiesHandler;

    public ModuleHelper(Module module) {
        this.module = module;
        propertiesHandler = new PropertiesHandler(module);
    }


    public static ModuleHelper create(Module module) {
        return new ModuleHelper(module);
    }

    @NotNull
    public String getServiceHostPrefix() {
        if (module == null) {
            return DEFAULT_URI;
        }

        String port = propertiesHandler.getServerPort();
        if (port == null || port.isEmpty()) port = PORT;

        String contextPath = propertiesHandler.getContextPath();
        return new StringBuilder(SCHEME).append(HOST).append(":").append(port).append(contextPath).toString();
    }

}
