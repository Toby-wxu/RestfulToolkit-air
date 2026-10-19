package com.zhaow.restful.navigation.action;

import com.intellij.ide.util.gotoByName.CustomMatcherModel;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.zhaow.restful.common.spring.AntPathMatcher;
import com.zhaow.restful.method.HttpMethod;
import com.zhaow.utils.RestfulToolkitBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * Model for "Go to | File" action
 */
public class GotoRequestMappingModel extends FilteringGotoByModel<HttpMethod> implements DumbAware, CustomMatcherModel {

    private final GotoRequestMappingContributor myContributor;
    private final boolean myToolbarUi;

    /** Null means "no method filter"; see {@link #setVisibleMethods}. */
    @Nullable
    private Collection<HttpMethod> myVisibleMethods;

    protected GotoRequestMappingModel(@NotNull Project project, @NotNull GotoRequestMappingContributor contributor) {
        super(project, new ChooseByNameContributor[]{contributor});
        this.myContributor = contributor;
        this.myToolbarUi = RestSearchUi.useToolbar();
    }

    public boolean usesToolbarUi() {
        return myToolbarUi;
    }

    /** The popup's module drop-down writes its selection through this contributor. */
    @NotNull
    public GotoRequestMappingContributor getContributor() {
        return myContributor;
    }

    /**
     * {@code FilteringGotoByModel#setFilterItems} cannot express "show every method" — an empty
     * collection hides all of them — so the filter state lives here, with null meaning unfiltered.
     */
    public synchronized void setVisibleMethods(@Nullable Collection<HttpMethod> visibleMethods) {
        myVisibleMethods = visibleMethods;
    }

    @Nullable
    @Override
    protected HttpMethod filterValueFor(NavigationItem item) {
        if (item instanceof RestServiceItem) {
            return ((RestServiceItem) item).getMethod();
        }

        return null;
    }

    @Nullable
    @Override
    protected synchronized Collection<HttpMethod> getFilterItems() {
        return myVisibleMethods;
    }

    @Override
    public String getPromptText() {
        return myToolbarUi ? "RESTful Toolkit" : "Enter service URL path :";
    }

    /**
     * Own wording: {@code IdeBundle} became {@code @ApiStatus.Internal} in 2026.2, and the platform
     * key used here is itself deprecated. {@code RestfulToolkitBundle_zh_CN} keeps the Chinese text
     * that the language pack used to supply.
     */
    @Override
    public String getNotInMessage() {
        return RestfulToolkitBundle.message("toolkit.goto.no.matches.in.project");
    }

    @Override
    public String getNotFoundMessage() {
        return RestfulToolkitBundle.message("toolkit.goto.no.matches");
    }

    @Nullable
    @Override
    public String getFullName(Object element) {
        return getElementName(element);
    }

    // 截取 Separators 后面pattern
    @NotNull
    @Override
    public String[] getSeparators() {
//        return new String[]{":","?"};
        return new String[]{"/","?"};
    }

    /**
     * Returning null hides the check box: module scoping is the drop-down in the popup now.
     */
    @Nullable
    @Override
    public String getCheckBoxName() {
        return null;
    }

    /**
     * Abstract on ChooseByNameModel even though no check box is shown, so they stay as no-ops;
     * the old "GoToRestService.OnlyCurrentModule" value is no longer read or written.
     */
    @Override
    public boolean loadInitialCheckBoxState() {
        return false;
    }

    @Override
    public void saveInitialCheckBoxState(boolean state) {
    }


    @Override
    public boolean willOpenEditor() {
        return true;
    }

//    CustomMatcherModel 接口，Allows to implement custom matcher for matching items from ChooseByName popup
    // todo: resolve PathVariable annotation
    /**
     * {@code matches} runs once per popup item per keystroke, so the name matcher is built per
     * pattern instead of per item. AntPathMatcher caches tokenised patterns internally.
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private String cachedPattern;
    private MinusculeMatcher cachedMatcher;

    @Override
    public boolean matches(@NotNull String popupItem, @NotNull String userPattern) {
        if (userPattern.equals("/")) return true;

        if (matcherFor(userPattern).matches(popupItem)) {
            return true;
        }
        // REST style paths: /product/{productId} matches /product/42
        return PATH_MATCHER.match(popupItem, userPattern);
    }

    private MinusculeMatcher matcherFor(String pattern) {
        if (!pattern.equals(cachedPattern)) {
            cachedPattern = pattern;
            // MatchingCaseSensitivity is scheduled for removal, but its replacement
            // com.intellij.util.text.matching.MatchingMode (buildMatcher(String, MatchingMode.IGNORE_CASE))
            // only exists from 2026.x, while this plugin is loadable from build 222. Switch once sinceBuild moves up.
            cachedMatcher = NameUtil.buildMatcher("*" + pattern, NameUtil.MatchingCaseSensitivity.NONE);
        }
        return cachedMatcher;
    }

// 没用 ？
    @NotNull
    @Override
    public String removeModelSpecificMarkup(@NotNull String pattern) {
        return super.removeModelSpecificMarkup(pattern);
//        return "demo";
    }

    /* TODO :重写渲染*/
    @Override
    public ListCellRenderer getListCellRenderer() {

        return myToolbarUi ? new RestServiceCellRenderer(super.getListCellRenderer()) : super.getListCellRenderer();
    }



}
