package com.zhaow.restful.navigation.action;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.PropertiesComponent;
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
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.zhaow.restful.method.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GotoRequestMappingAction extends GotoActionBase implements DumbAware {
    private static final String ALL_MODULES = "All Modules";
    private static final String ALL_METHODS = "All Methods";
    private static final String MODULE_SCOPE_KEY = "GoToRestService.ModuleScope";
    private static final String METHOD_FILTER_KEY = "GoToRestService.MethodFilter";

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
        HttpMethod persistedMethod = persistedMethod(project);
        model.setVisibleMethods(persistedMethod == null ? null : List.of(persistedMethod));

        GotoActionCallback<HttpMethod> callback = new GotoActionCallback<HttpMethod>() {
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
        // A null find-usages title is the platform's own switch for hiding the "show all in view" button.
        showNavigationPopup(e, model, callback, null, true, true, (ChooseByNameItemProvider)provider);
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
        GotoRequestMappingModel gotoModel = (GotoRequestMappingModel) model;
        List<JComponent> tools = new ArrayList<>();
        JComponent moduleSelector = createModuleSelector(popup, gotoModel.getContributor(), project, gotoModel.usesToolbarUi());
        if (moduleSelector != null) {
            tools.add(moduleSelector);
        }
        tools.add(createMethodSelector(popup, gotoModel, project));
        popup.setToolComponents(tools);

        showNavigationPopup(callback, findUsagesTitle, popup, allowMultipleSelection);
    }

    /**
     * Single-select HTTP method filter. The platform's own funnel is not used: in this popup its
     * action never reaches {@code actionPerformed} because ActionManager rejects it with a context
     * component that is not showing.
     */
    private static JComponent createMethodSelector(ChooseByNamePopup popup,
                                                   GotoRequestMappingModel model,
                                                   @NotNull Project project) {
        List<HttpMethod> methods = Arrays.asList(HttpMethod.values());
        String[] labels = new String[methods.size() + 1];
        labels[0] = ALL_METHODS;
        for (int i = 0; i < methods.size(); i++) {
            labels[i + 1] = methods.get(i).name();
        }

        JComboBox<String> selector = model.usesToolbarUi() ? new ComboBox<>(labels) : new JComboBox<>(labels);
        HttpMethod current = persistedMethod(project);
        selector.setSelectedIndex(current == null ? 0 : methods.indexOf(current) + 1);
        selector.setToolTipText("Show only one HTTP method");
        applyCompactWidth(selector);
        selector.getAccessibleContext().setAccessibleName("HTTP method filter");
        if (model.usesToolbarUi()) {
            applyToolbarWidth(selector, 112);
            updateMethodFilterAppearance(selector);
        }

        selector.addActionListener(event -> {
            int index = selector.getSelectedIndex();
            HttpMethod method = index <= 0 ? null : methods.get(index - 1);
            model.setVisibleMethods(method == null ? null : List.of(method));
            PropertiesComponent.getInstance(project)
                    .setValue(METHOD_FILTER_KEY, method == null ? null : method.name());
            if (model.usesToolbarUi()) updateMethodFilterAppearance(selector);
            popup.rebuildList(false);
        });

        return selector;
    }

    @Nullable
    private static HttpMethod persistedMethod(@NotNull Project project) {
        String name = PropertiesComponent.getInstance(project).getValue(METHOD_FILTER_KEY);
        if (name == null) {
            return null;
        }
        for (HttpMethod method : HttpMethod.values()) {
            if (method.name().equals(name)) {
                return method;
            }
        }
        return null;
    }

    private static void applyCompactWidth(JComponent component) {
        Dimension preferred = component.getPreferredSize();
        Dimension size = new Dimension(Math.min(preferred.width, 180), preferred.height);
        component.setPreferredSize(size);
        component.setMaximumSize(size);
    }

    private static void applyToolbarWidth(JComponent component, int width) {
        Dimension size = new Dimension(JBUI.scale(width), Math.max(JBUI.scale(32), component.getPreferredSize().height));
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
    }

    private static void updateMethodFilterAppearance(JComboBox<String> selector) {
        boolean filtered = selector.getSelectedIndex() > 0;
        selector.setForeground(filtered
                ? JBColor.namedColor("Link.activeForeground", new JBColor(0x3574F0, 0x6C95EB))
                : UIUtil.getLabelForeground());
        selector.setToolTipText(filtered ? "HTTP method: " + selector.getSelectedItem() + "; choose All Methods to clear"
                : "Show all HTTP methods");
    }

    /**
     * Module drop-down for the popup's tool row. Index 0 means "search the whole project"; the
     * choice is remembered per project so a scoped search survives reopening the popup.
     */
    @Nullable
    private static JComponent createModuleSelector(ChooseByNamePopup popup,
                                                   GotoRequestMappingContributor contributor,
                                                   @NotNull Project project, boolean toolbarUi) {
        List<Module> modules = Arrays.asList(ModuleManager.getInstance(project).getModules());
        if (modules.size() < 2) {
            return null;
        }

        String[] labels = new String[modules.size() + 1];
        labels[0] = ALL_MODULES;
        for (int i = 0; i < modules.size(); i++) {
            labels[i + 1] = modules.get(i).getName();
        }

        JComboBox<String> selector = toolbarUi ? new ComboBox<>(labels) : new JComboBox<>(labels);
        Module scope = contributor.getScope();
        int initial = scope == null ? 0 : modules.indexOf(scope) + 1;
        selector.setSelectedIndex(Math.max(initial, 0));
        selector.setToolTipText("Limit the search to one module");
        applyCompactWidth(selector);
        selector.getAccessibleContext().setAccessibleName("Module scope");
        if (toolbarUi) applyToolbarWidth(selector, 160);

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

    //找到文件
    private PsiElement getElement(PsiElement element, ChooseByNamePopup chooseByNamePopup) {
        return null;
    }
}
