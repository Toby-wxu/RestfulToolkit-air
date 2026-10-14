package com.zhaow.restful.method.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiMethod;
import com.zhaow.restful.common.PsiMethodHelper;
import com.zhaow.restful.method.Parameter;
import com.zhaow.utils.RestfulToolkitBundle;

/**
 * 生成Request Body JSON 字符串
 */
public class GenerateQueryParamJsonAction extends SpringAnnotatedMethodAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiMethod psiMethod = resolvePsiMethod(e);
        if (psiMethod == null) {
            return;
        }

        PsiMethodHelper psiMethodHelper = PsiMethodHelper.create(psiMethod);
        String queryJson = null;
        for (Parameter parameter : psiMethodHelper.getParameterList()) {
            if (parameter.isRequestBodyFound()) {
                queryJson = psiMethodHelper.buildRequestBodyJson(parameter);
                break;
            }
        }

        Editor myEditor = e.getData(CommonDataKeys.EDITOR);
        if (queryJson == null || queryJson.isEmpty()) {
            if (myEditor != null) {
                showPopupBalloon(RestfulToolkitBundle.message("toolkit.action.no.requestbody"), myEditor);
            }
            return;
        }

        copyAndNotify(e, queryJson);
    }
}
