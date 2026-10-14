package com.zhaow.restful.navigation.action;


import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.ChooseByNameViewModel;
import com.intellij.ide.util.gotoByName.CustomMatcherModel;
import com.intellij.ide.util.gotoByName.DefaultChooseByNameItemProvider;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.zhaow.utils.ToolkitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The default provider runs a second pass over the names it already accepted
 * ({@code matchQualifiedName}), comparing them with a plain {@code MinusculeMatcher}. That pass
 * knows nothing about path variables, so every Ant hit between a declared {@code /x/{id}} and the
 * concrete {@code /x/2} the user typed was thrown away. Matching is decided once, by the model.
 */
public class GotoRequestMappingProvider extends DefaultChooseByNameItemProvider {

    public GotoRequestMappingProvider(@Nullable PsiElement context) {
        super(context);
    }

    /**
     * Kept alongside the {@link FindSymbolParameters} overload on purpose: recent builds route every
     * {@code filterElements} entry point through that one, but the String-shaped entry still exists and
     * older builds are free to reach it directly.
     */
    @Override
    public boolean filterElementsWithWeights(@NotNull ChooseByNameViewModel base,
                                             @NotNull String pattern,
                                             boolean everywhere,
                                             @NotNull ProgressIndicator indicator,
                                             @NotNull Processor<? super FoundItemDescriptor<?>> consumer) {
        return filterServices(base, ToolkitUtil.removeRedundancyMarkup(pattern), everywhere, indicator, consumer);
    }

    @Override
    public boolean filterElementsWithWeights(@NotNull ChooseByNameViewModel base,
                                             @NotNull FindSymbolParameters parameters,
                                             @NotNull ProgressIndicator indicator,
                                             @NotNull Processor<? super FoundItemDescriptor<?>> consumer) {
        return filterServices(base,
                ToolkitUtil.removeRedundancyMarkup(parameters.getCompletePattern()),
                parameters.isSearchInLibraries(),
                indicator,
                consumer);
    }

    private static boolean filterServices(@NotNull ChooseByNameViewModel base,
                                          @NotNull String pattern,
                                          boolean everywhere,
                                          @NotNull ProgressIndicator indicator,
                                          @NotNull Processor<? super FoundItemDescriptor<?>> consumer) {
        ChooseByNameModel model = base.getModel();

        for (String name : model.getNames(everywhere)) {
            indicator.checkCanceled();
            if (!matches(model, name, pattern)) {
                continue;
            }
            for (Object item : model.getElementsByName(name, everywhere, pattern)) {
                if (!consumer.process(new FoundItemDescriptor<>(item, 0))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matches(@NotNull ChooseByNameModel model, @NotNull String name, @NotNull String pattern) {
        return model instanceof CustomMatcherModel
                ? ((CustomMatcherModel) model).matches(name, pattern)
                : name.contains(pattern);
    }
}
