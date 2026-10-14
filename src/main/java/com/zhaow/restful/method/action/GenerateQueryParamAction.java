package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiMethod;
import com.zhaow.restful.common.PsiMethodHelper;

/**
 * 生成查询参数
 */
public class GenerateQueryParamAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiMethod psiMethod = resolvePsiMethod(e);
        if (psiMethod == null) {
            return;
        }

        String params = PsiMethodHelper.create(psiMethod).buildParamString();

        copyAndNotify(e, params);
    }

}
