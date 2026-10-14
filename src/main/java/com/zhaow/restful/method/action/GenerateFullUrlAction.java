package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiMethod;
import com.zhaow.restful.common.PsiMethodHelper;

/**
 * 生成并复制restful url
 */
public class GenerateFullUrlAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiMethod psiMethod = resolvePsiMethod(e);
        if (psiMethod == null) {
            return;
        }

        Module module = myModule(e);
        String url = PsiMethodHelper.create(psiMethod).withModule(module).buildFullUrlWithParams();

        copyAndNotify(e, url);
    }

}
