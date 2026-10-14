package com.zhaow.restful.navigator;


import com.intellij.openapi.module.Module;
import com.zhaow.restful.navigation.action.RestServiceItem;

import java.util.List;

/**
 * One module's worth of discovered REST endpoints, as shown by a node in the Services tree.
 */
public class RestServiceProject {
    private final String moduleName;

    final List<RestServiceItem> serviceItems;

    public RestServiceProject(Module module, List<RestServiceItem> serviceItems) {
        this.moduleName = module.getName();
        this.serviceItems = serviceItems;
    }

    public String getModuleName() {
        return moduleName;
    }
}
