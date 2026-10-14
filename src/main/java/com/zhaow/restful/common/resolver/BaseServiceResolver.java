package com.zhaow.restful.common.resolver;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.zhaow.restful.method.RequestPath;
import com.zhaow.restful.method.action.ModuleHelper;
import com.zhaow.restful.navigation.action.RestServiceItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseServiceResolver implements ServiceResolver{
    Module myModule;
    Project myProject;

    // Resolved once per resolver: reading server.port touches the file index.
    private String hostPrefix;
    private boolean hostPrefixResolved;

    @Override
    @NotNull
    public List<RestServiceItem> findAllSupportedServiceItemsInModule() {
        if (myModule == null) {
            return new ArrayList<>();
        }
        return getRestServiceItemList(myModule.getProject(), GlobalSearchScope.moduleScope(myModule));
    }

    @NotNull
    public abstract List<RestServiceItem> getRestServiceItemList(@NotNull Project project,
                                                                @NotNull GlobalSearchScope globalSearchScope);

    @Override
    @NotNull
    public List<RestServiceItem> findAllSupportedServiceItemsInProject() {
        if (myProject == null && myModule != null) {
            myProject = myModule.getProject();
        }
        if (myProject == null) {
            return new ArrayList<>();
        }
        return getRestServiceItemList(myProject, GlobalSearchScope.projectScope(myProject));
    }

    @NotNull
    protected RestServiceItem createRestServiceItem(PsiElement psiMethod,
                                                    @Nullable String classUriPath,
                                                    RequestPath requestMapping) {
        String requestPath = RequestPath.join(classUriPath, requestMapping.getPath());

        RestServiceItem item = new RestServiceItem(psiMethod, requestMapping.getMethod(), requestPath);
        if (myModule != null) {
            item.setModule(myModule);
            item.setHostPrefix(resolveHostPrefix());
        }
        return item;
    }

    /**
     * Resolved during the scan, where index reads are allowed.
     */
    @Nullable
    private String resolveHostPrefix() {
        if (!hostPrefixResolved) {
            hostPrefixResolved = true;
            hostPrefix = ModuleHelper.create(myModule).getServiceHostPrefix();
        }
        return hostPrefix;
    }
}
