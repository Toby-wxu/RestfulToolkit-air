package com.zhaow.restful.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;

import java.util.Collection;
import java.util.Collections;

// 处理 实体自关联，第二层自关联字段
public class KtClassHelper {
    KtClass psiClass;

    private static int  autoCorrelationCount=0; //标记实体递归
    private int listIterateCount = 0; //标记List递归
    private Module myModule;

    protected KtClassHelper(@NotNull KtClass psiClass) {
        this.psiClass = psiClass;
    }

    @NotNull
    protected Project getProject() {
        return psiClass.getProject();
    }

    @Nullable
    public KtClassOrObject findOnePsiClassByClassName(String className, Project project) {
        if (psiClass == null) {
            return null;
        }
        if (psiClass.getFqName() != null && className.equals(psiClass.getFqName().asString())) {
            return psiClass;
        }
        return null;
    }


//PsiShortNamesCache : PsiClass:Demo    KtLightClassImpl:data class Greeting(val id: Long, val content: String) { 代码体 }
    public Collection<KtClassOrObject> tryDetectPsiClassByShortClassName(Project project, String shortClassName) {
        return Collections.emptyList();
    }

    public static KtClassHelper create(@NotNull KtClass psiClass) {
        return new KtClassHelper(psiClass);
    }


}
