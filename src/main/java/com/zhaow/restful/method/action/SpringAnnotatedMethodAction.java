package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.util.PsiTreeUtil;
import com.zhaow.restful.action.AbstractBaseAction;
import com.zhaow.restful.annotations.SpringControllerAnnotation;
import com.zhaow.restful.annotations.SpringRequestMethodAnnotation;
import com.zhaow.utils.RestfulToolkitBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * Restful method （restful 方法添加方法 ）
 */
public abstract class SpringAnnotatedMethodAction extends AbstractBaseAction {

    /**
     * Resolve the element under the caret to a {@link PsiMethod}, bridging Kotlin declarations
     * through their light classes. Returns null when the caret is not on a method.
     */
    @Nullable
    protected PsiMethod resolvePsiMethod(@NotNull AnActionEvent e) {
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);

        if (psiElement instanceof PsiMethod) {
            return (PsiMethod) psiElement;
        }

        if (psiElement instanceof KtNamedFunction) {
            KtNamedFunction function = (KtNamedFunction) psiElement;
            // A top-level function's parent chain ends at the KtFile, so walk by type instead.
            KtClassOrObject owner = PsiTreeUtil.getParentOfType(function, KtClassOrObject.class);
            if (owner == null) {
                return null;
            }
            List<PsiMethod> lightMethods = LightClassUtilsKt.toLightMethods(function);
            // A function that produces no light methods (overloads against a stub) is not navigable.
            return lightMethods.isEmpty() ? null : lightMethods.get(0);
        }

        return null;
    }

    /**
     * Copy the generated text and confirm it on screen.
     */
    protected void copyAndNotify(@NotNull AnActionEvent e, @NotNull String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            showPopupBalloon(RestfulToolkitBundle.message("toolkit.action.copied"), editor);
        }
    }

    /**
     * spring rest 方法被选中才触发
     */
    @Override
    public void update(AnActionEvent e) {
        setActionPresentationVisible(e, isRestMethod(resolvePsiMethod(e)));
    }

    protected boolean isRestMethod(@Nullable PsiMethod psiMethod) {
        if (psiMethod == null) {
            return false;
        }
        return isRestController(psiMethod.getContainingClass()) || isRestfulMethod(psiMethod);
    }

    //包含 "RestController" "Controller"
    protected static boolean isRestController(@Nullable PsiClass psiClass) {
        PsiModifierList modifierList = psiClass == null ? null : psiClass.getModifierList();
        if (modifierList == null) {
            return false;
        }
        return modifierList.findAnnotation(SpringControllerAnnotation.REST_CONTROLLER.getQualifiedName()) != null ||
                modifierList.findAnnotation(SpringControllerAnnotation.CONTROLLER.getQualifiedName()) != null;
    }

    protected static boolean isRestfulMethod(@NotNull PsiMethod psiMethod) {
        PsiModifierList modifierList = psiMethod.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            for (SpringRequestMethodAnnotation candidate : SpringRequestMethodAnnotation.values()) {
                if (candidate.getQualifiedName().equals(qualifiedName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
