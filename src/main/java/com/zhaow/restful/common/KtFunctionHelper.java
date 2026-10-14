package com.zhaow.restful.common;


import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.util.List;

/**
 * KtFunction处理类：把 Kotlin 声明适配成可用于现有解析逻辑的 light {@link PsiMethod}。
 */
public class KtFunctionHelper extends PsiMethodHelper {

    /**
     * @return null when the function has no backing light method (e.g. a top-level function),
     * which cannot be modelled as a {@code PsiMethod}.
     */
    @Nullable
    public static KtFunctionHelper create(@NotNull KtNamedFunction function) {
        List<PsiMethod> psiMethods = LightClassUtilsKt.toLightMethods(function);
        if (psiMethods.isEmpty()) {
            return null;
        }
        return new KtFunctionHelper(psiMethods.get(0));
    }

    private KtFunctionHelper(@NotNull PsiMethod psiMethod) {
        super(psiMethod);
    }
}
