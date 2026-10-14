package com.zhaow.restful.navigation.action;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.gotoByName.ChooseByNameFilter;
import com.intellij.ide.util.gotoByName.ChooseByNameItemProvider;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.zhaow.restful.method.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.util.Arrays;
import java.util.List;


public class GotoRequestMappingAction extends GotoActionBase implements DumbAware {
    private static final String ALL_MODULES = "All Modules";
    private static final String MODULE_SCOPE_KEY = "GoToRestService.ModuleScope";

    public GotoRequestMappingAction() {
    }

    @Override
    protected void gotoActionPerformed(AnActionEvent e) {
        //进入导航
        Project project = e.getProject();
        if (project == null) return;

        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.service");

        GotoRequestMappingContributor contributor = new GotoRequestMappingContributor();
        restorePersistedScope(project, contributor);

        final GotoRequestMappingModel model = new GotoRequestMappingModel(project, contributor);

//        GotoRequestMappingCallback callback = new GotoRequestMappingCallback();

        GotoActionCallback<HttpMethod> callback = new GotoActionCallback<HttpMethod>() {
            @Override
            protected ChooseByNameFilter<HttpMethod> createFilter(@NotNull ChooseByNamePopup popup) {
                return new GotoRequestMappingFilter(popup, model, project);
            }

            @Override
            public void elementChosen(ChooseByNamePopup chooseByNamePopup, Object element) {
                if (element instanceof RestServiceItem) {
                    RestServiceItem navigationItem = (RestServiceItem) element;
                    if (navigationItem.canNavigate()) {
                        navigationItem.navigate(true);
                    }
                }
            }
        };

//        this.showNavigationPopup(e, model, callback, false);
        GotoRequestMappingProvider provider = new GotoRequestMappingProvider(getPsiContext(e));
        showNavigationPopup(e, model, callback, "Request Mapping Url matching pattern", true, true, (ChooseByNameItemProvider)provider);
//        showNavigationPopup(callback,"Request Mapping Url matching pattern",);

    }


   @Override
    protected <T> void showNavigationPopup(AnActionEvent e,
                                           ChooseByNameModel model,
                                           final GotoActionCallback<T> callback,
                                           @Nullable final String findUsagesTitle,
                                           boolean useSelectionFromEditor,
                                           final boolean allowMultipleSelection,
                                           final ChooseByNameItemProvider itemProvider) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        boolean mayRequestOpenInCurrentWindow = model.willOpenEditor() && FileEditorManagerEx.getInstanceEx(project).hasSplitOrUndockedWindows();
        Pair<String, Integer> start = getInitialText(useSelectionFromEditor, e);
        /*showNavigationPopup(callback, findUsagesTitle,
                ChooseByNamePopup.createPopup(project, model, itemProvider, start.first,
                        mayRequestOpenInCurrentWindow,
                        start.second), allowMultipleSelection);*/

        String copiedURL = tryFindCopiedURL();

        String predefinedText = start.first == null ? copiedURL : start.first;

        RestServiceChooseByNamePopup popup = RestServiceChooseByNamePopup.createPopup(project, model, itemProvider,
                predefinedText, mayRequestOpenInCurrentWindow, start.second);
        // Not ChooseByNameBase#setToolArea: ChooseByNameFilter claims that slot in its own
        // constructor, and the slot accepts a single component before invoke().
        popup.setModuleSelector(createModuleSelector(popup, ((GotoRequestMappingModel) model).getContributor(), project));

        showNavigationPopup(callback, findUsagesTitle, popup, allowMultipleSelection);
    }

    /**
     * Module drop-down for the popup's tool row. Index 0 means "search the whole project"; the
     * choice is remembered per project so a scoped search survives reopening the popup.
     */
    @Nullable
    private static JComponent createModuleSelector(ChooseByNamePopup popup,
                                                   GotoRequestMappingContributor contributor,
                                                   @NotNull Project project) {
        List<Module> modules = Arrays.asList(ModuleManager.getInstance(project).getModules());
        if (modules.size() < 2) {
            return null;
        }

        String[] labels = new String[modules.size() + 1];
        labels[0] = ALL_MODULES;
        for (int i = 0; i < modules.size(); i++) {
            labels[i + 1] = modules.get(i).getName();
        }

        JComboBox<String> selector = new JComboBox<>(labels);
        Module scope = contributor.getScope();
        int initial = scope == null ? 0 : modules.indexOf(scope) + 1;
        selector.setSelectedIndex(Math.max(initial, 0));
        selector.setToolTipText("Limit the search to one module");

        Dimension preferred = selector.getPreferredSize();
        Dimension size = new Dimension(Math.min(preferred.width, 180), preferred.height);
        selector.setPreferredSize(size);
        selector.setMaximumSize(size);

        selector.addActionListener(event -> {
            int index = selector.getSelectedIndex();
            Module selected = index <= 0 ? null : modules.get(index - 1);
            contributor.setScope(selected);
            PropertiesComponent.getInstance(project)
                    .setValue(MODULE_SCOPE_KEY, selected == null ? null : selected.getName());
            popup.rebuildList(false);
        });

        return selector;
    }

    private static void restorePersistedScope(@NotNull Project project, GotoRequestMappingContributor contributor) {
        String name = PropertiesComponent.getInstance(project).getValue(MODULE_SCOPE_KEY);
        if (name == null) {
            return;
        }
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (module.getName().equals(name)) {
                contributor.setScope(module);
                return;
            }
        }
    }

    private String tryFindCopiedURL() {
        String contents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
        if (contents == null) {
            return null;
        }

        contents = contents.trim();
        if (contents.startsWith("http")) {
            if (contents.length() <= 120) {
                return contents;
            }else {
                return contents.substring(0, 120);
            }
        }

        return null;
    }

/*
    private class GotoRequestMappingCallback extends GotoActionCallback{
//        定位选择文件
        @Override
        public void elementChosen(ChooseByNamePopup chooseByNamePopup, Object element) {
            if (element instanceof RestServiceItem) {
                RestServiceItem navigationItem = (RestServiceItem) element;
                if (navigationItem.canNavigate()) {
                    navigationItem.navigate(true);
                }
            }
        }
    }*/

    protected static class GotoRequestMappingFilter extends ChooseByNameFilter<HttpMethod> {
        GotoRequestMappingFilter(final ChooseByNamePopup popup, GotoRequestMappingModel model, final Project project) {
            super(popup, model, GotoRequestMappingConfiguration.getInstance(project), project);
        }

        @Override
        @NotNull
        protected List<HttpMethod> getAllFilterValues() {
//            List<HttpMethod> elements = new ArrayList<>();
            /*elements.add(HttpMethod.GET);
            elements.add(HttpMethod.POST);
            elements.add(HttpMethod.DELETE);
            elements.add(HttpMethod.PATCH);*/
            List<HttpMethod> elements = Arrays.asList(HttpMethod.values());

            return elements;
        }

        @Override
        protected String textForFilterValue(@NotNull HttpMethod value) {
            return value.name();
        }

        @Override
        protected Icon iconForFilterValue(@NotNull HttpMethod value) {
//            return value.getIcon();
            return null;
        }
    }


    //找到文件
    private PsiElement getElement(PsiElement element, ChooseByNamePopup chooseByNamePopup) {
        return null;
    }
}
