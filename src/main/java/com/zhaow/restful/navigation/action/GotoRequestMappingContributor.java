package com.zhaow.restful.navigation.action;


import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.zhaow.restful.common.ServiceHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

//
public class GotoRequestMappingContributor implements ChooseByNameContributor {
    // Selected from the module drop-down in the popup; null searches the whole project.
    private volatile Module myScopeModule;

    // Written by getNames, read by getItemsByName: publish it as an immutable snapshot.
    private volatile List<RestServiceItem> navItem = Collections.emptyList();

    public @Nullable Module getScope() {
        return myScopeModule;
    }

    public void setScope(@Nullable Module scopeModule) {
        this.myScopeModule = scopeModule;
    }

    //Returns the list of names for the specified project to which it is possible to navigate by name. 所有该类型的文件列表
    @NotNull
    @Override
    public String[] getNames(Project project, boolean includeNonProjectItems) {
        Module scope = myScopeModule;

        // Resolving mappings reads the Java annotation index, so it has to wait for smart mode.
        List<RestServiceItem> itemList = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            if (scope != null) {
                return ServiceHelper.buildRestServiceItemListUsingResolver(scope);
            }
            return ServiceHelper.buildRestServiceItemListUsingResolver(project);
        });

        navItem = itemList;

        return itemList.stream().map(NavigationItem::getName).toArray(String[]::new);
    }

    //Returns the list of navigation items matching the specified name. 匹配，对比
    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        return navItem.stream().filter(item -> item.getName().equals(name)).toArray(NavigationItem[]::new);
    }
}
