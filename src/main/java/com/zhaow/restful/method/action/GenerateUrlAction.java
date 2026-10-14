package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.zhaow.restful.annotations.JaxrsHttpMethodAnnotation;
import com.zhaow.restful.annotations.JaxrsRequestAnnotation;
import com.zhaow.restful.common.PsiMethodHelper;

import java.util.List;

/**
 * 生成并复制restful url
 */
public class GenerateUrlAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiMethod psiMethod = resolvePsiMethod(e);
        if (psiMethod == null) {
            return;
        }

        PsiMethodHelper helper = PsiMethodHelper.create(psiMethod);
        List<String> servicePaths = isJaxrsResource(psiMethod)
                ? helper.buildServiceUriPaths()
                : helper.buildServiceUriPathsWithParams();
        String servicePath = String.join("\n", servicePaths);

        copyAndNotify(e, servicePath);
    }

    /**
     * JAX-RS 资源同样可用，因此比 Spring 判定多接受 @Path 与 JAX-RS 方法注解。
     */
    @Override
    protected boolean isRestMethod(PsiMethod psiMethod) {
        return super.isRestMethod(psiMethod) || isJaxrsResource(psiMethod);
    }

    private boolean isJaxrsResource(PsiMethod psiMethod) {
        if (psiMethod == null) {
            return false;
        }
        return isPathAnnotated(psiMethod.getContainingClass())
                || hasJaxrsHttpAnnotation(psiMethod);
    }

    private boolean isPathAnnotated(PsiClass psiClass) {
        PsiModifierList modifierList = psiClass == null ? null : psiClass.getModifierList();
        return modifierList != null
                && modifierList.findAnnotation(JaxrsRequestAnnotation.PATH.getQualifiedName()) != null;
    }

    private boolean hasJaxrsHttpAnnotation(PsiMethod psiMethod) {
        PsiModifierList modifierList = psiMethod.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            for (JaxrsHttpMethodAnnotation candidate : JaxrsHttpMethodAnnotation.values()) {
                if (candidate.getQualifiedName().equals(qualifiedName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
