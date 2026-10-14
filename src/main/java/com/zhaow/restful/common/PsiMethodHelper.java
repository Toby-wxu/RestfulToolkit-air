package com.zhaow.restful.common;


import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.zhaow.restful.annotations.JaxrsRequestAnnotation;
import com.zhaow.restful.annotations.SpringControllerAnnotation;
import com.zhaow.restful.common.jaxrs.JaxrsAnnotationHelper;
import com.zhaow.restful.common.spring.RequestMappingAnnotationHelper;
import com.zhaow.restful.method.RequestPath;
import com.zhaow.restful.method.Parameter;
import com.zhaow.restful.method.action.ModuleHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zhaow.restful.annotations.SpringRequestParamAnnotations.PATH_VARIABLE;
import static com.zhaow.restful.annotations.SpringRequestParamAnnotations.REQUEST_BODY;
import static com.zhaow.restful.annotations.SpringRequestParamAnnotations.REQUEST_PARAM;

/**
 * PsiMethod处理类
 */
public class PsiMethodHelper {
    PsiMethod psiMethod;
    Project myProject;
    Module myModule;

    private String pathSeparator = "/";

    public static PsiMethodHelper create(@NotNull PsiMethod psiMethod) {
        return new PsiMethodHelper(psiMethod);
    }

    public PsiMethodHelper withModule(Module module) {
        this.myModule = module;
        return this;
    }

    protected PsiMethodHelper(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }

    @NotNull
    protected Project getProject() {
        myProject = psiMethod.getProject();
        return myProject;
    }

    /**
     * 构建URL参数 key value
     *
     * @return
     */
    public String buildParamString() {

//        boolean matchedGet = matchGetMethod();
        // 没指定method 标示支持所有method

        StringBuilder param = new StringBuilder("");
        Map<String, Object> baseTypeParamMap = getBaseTypeParameterMap();

        if (baseTypeParamMap != null && baseTypeParamMap.size() > 0) {
            baseTypeParamMap.forEach((s, o) -> param.append(s).append("=").append(o).append("&"));
        }

        return param.length() > 0 ? param.deleteCharAt(param.length() - 1).toString() : "";
    }

    /*获取方法中基础类型（primitive和string、date等以及这些类型数组）*/
    @NotNull
    public Map<String, Object> getBaseTypeParameterMap() {
        List<Parameter> parameterList = getParameterList();

        Map<String, Object> baseTypeParamMap = new LinkedHashMap();

        // 拼接参数
        for (Parameter parameter : parameterList) {
//跳过标注 RequestBody 注解的参数
            if (parameter.isRequestBodyFound()) {
                continue;
            }

            // todo 判断类型
            // 8 PsiPrimitiveType
            // 8 boxed types; String,Date:PsiClassReferenceType == field.getType().getPresentableText()
            String shortTypeName = parameter.getShortTypeName();
            Object defaultValue = PsiClassHelper.getJavaBaseTypeDefaultValue(shortTypeName);
            //简单常用类型
            if (defaultValue != null) {
                baseTypeParamMap.put(parameter.getParamName(), (defaultValue));
                continue;
            }

            PsiClassHelper psiClassHelper = PsiClassHelper.create(psiMethod.getContainingClass());
            PsiClass psiClass = psiClassHelper.findOnePsiClassByClassName(parameter.getParamType(), getProject());

            if (psiClass != null) {
                PsiField[] fields = psiClass.getFields();
                for (PsiField field : fields) {
                    if ("serialVersionUID".equals(field.getName())) {
                        continue;
                    }
                    Object fieldDefaultValue = PsiClassHelper.getJavaBaseTypeDefaultValue(field.getType().getPresentableText());
                    if (fieldDefaultValue != null) {
                        baseTypeParamMap.put(field.getName(), fieldDefaultValue);
                    }
                }
            }

          /*  PsiClass psiClass2 = psiClassHelper.findOnePsiClassByClassName2(parameter.getParamType(), getProject());
            if (psiClass2 != null) {
                PsiField[] fields = psiClass2.getFields();
                for (PsiField field : fields) {
                    Object fieldDefaultValue  = PsiClassHelper.getJavaBaseTypeDefaultValue(field.getType().getPresentableText());
                    if(fieldDefaultValue != null)
                        baseTypeParamMap.put(field.getName(), fieldDefaultValue);
                }
            }*/
        }
        return baseTypeParamMap;
    }

    /* 基础类型默认值 */
    @Nullable
    public Map<String, Object> getJavaBaseTypeDefaultValue(String paramName, String paramType) {
        Map<String, Object> paramMap = new LinkedHashMap<>();
        Object paramValue = null;
        paramValue = PsiClassHelper.getJavaBaseTypeDefaultValue(paramType);
        if (paramValue != null) {
            paramMap.put(paramType, paramValue);
        }
        return paramMap;
    }

    @NotNull
    public List<Parameter> getParameterList() {
        List<Parameter> parameterList = new ArrayList<>();

        PsiParameterList psiParameterList = psiMethod.getParameterList();
        PsiParameter[] psiParameters = psiParameterList.getParameters();
        for (PsiParameter psiParameter : psiParameters) {
            //忽略 request response
            // Spring Boot 3 moved the servlet API from javax.servlet to jakarta.servlet.
            String paramType = psiParameter.getType().getCanonicalText();
            if (paramType.endsWith(".servlet.http.HttpServletRequest")
                    || paramType.endsWith(".servlet.http.HttpServletResponse")
                    || paramType.endsWith(".servlet.http.HttpSession")) {
                continue;
            }
            //必传参数 @RequestParam
            PsiModifierList modifierList = psiParameter.getModifierList();
            boolean requestBodyFound =
                    findAnnotation(modifierList, REQUEST_BODY.getQualifiedName()) != null;
            // 没有 RequestParam 注解, 有注解使用注解value
            String paramName = psiParameter.getName();
            String requestName = null;


            PsiAnnotation pathVariableAnno =
                    findAnnotation(modifierList, PATH_VARIABLE.getQualifiedName());
            if (pathVariableAnno != null) {
                requestName = getAnnotationValue(pathVariableAnno);
                Parameter parameter = new Parameter(paramType, requestName != null ? requestName : paramName).setRequired(true).requestBodyFound(requestBodyFound);
                parameterList.add(parameter);
            }

            PsiAnnotation requestParamAnno =
                    findAnnotation(modifierList, REQUEST_PARAM.getQualifiedName());
            if (requestParamAnno != null) {
                requestName = getAnnotationValue(requestParamAnno);
                Parameter parameter = new Parameter(paramType, requestName != null ? requestName : paramName).setRequired(true).requestBodyFound(requestBodyFound);
                parameterList.add(parameter);
            }

            if (pathVariableAnno == null && requestParamAnno == null) {
                Parameter parameter = new Parameter(paramType, paramName).requestBodyFound(requestBodyFound);
                parameterList.add(parameter);
            }
        }
        return parameterList;
    }

    @Nullable
    private static PsiAnnotation findAnnotation(@Nullable PsiModifierList modifierList,
                                                @NotNull String qualifiedName) {
        return modifierList == null ? null : modifierList.findAnnotation(qualifiedName);
    }

    @Nullable
    public String getAnnotationValue(PsiAnnotation annotation) {
        String paramName = null;
        PsiAnnotationMemberValue attributeValue = annotation.findDeclaredAttributeValue("value");

        if (attributeValue instanceof PsiLiteralExpression) {
            Object value = ((PsiLiteralExpression) attributeValue).getValue();
            paramName = value == null ? null : value.toString();
        }
        return paramName;
    }

    /**
     * 构建RequestBody json 参数
     *
     * @param parameter
     * @return
     */
    public String buildRequestBodyJson(Parameter parameter) {
//        JavaFullClassNameIndex.getInstance();

        Project project = psiMethod.getProject();
        final String className = parameter.getParamType();

        String queryJson = PsiClassHelper.create(psiMethod.getContainingClass()).withModule(myModule).convertClassToJSON(className, project);
        return queryJson;
    }


    public String buildRequestBodyJson() {
        List<Parameter> parameterList = this.getParameterList();
        for (Parameter parameter : parameterList) {
            if (parameter.isRequestBodyFound()) {
                return buildRequestBodyJson(parameter);
            }
        }
        return null;
    }

    @NotNull
    public String buildServiceUriPath() {
        List<String> paths = buildServiceUriPaths();
        if (!paths.isEmpty()) {
            return paths.get(0);
        }
        return "/";
    }

    @NotNull
    public List<String> buildServiceUriPaths() {
        List<String> paths = new ArrayList<>();
        String ctrlPath = null;
        String methodPath = null;

        //判断rest服务提供方式 spring or jaxrs
        PsiClass containingClass = psiMethod.getContainingClass();
        if (isSpringRestSupported(containingClass)) {
            List<RequestPath> classRequestPaths = RequestMappingAnnotationHelper.getRequestPaths(containingClass);
            RequestPath[] methodRequestPaths = RequestMappingAnnotationHelper.getRequestPaths(psiMethod);
            if (classRequestPaths != null && methodRequestPaths != null) {
                for (RequestPath classRequestPath : classRequestPaths) {
                    for (RequestPath methodRequestPath : methodRequestPaths) {
                        paths.add(joinPath(classRequestPath.getPath(), methodRequestPath.getPath()));
                    }
                }
            }
            return deduplicate(paths);
        } else if (isJaxrsRestSupported(containingClass)) {
            ctrlPath = JaxrsAnnotationHelper.getClassUriPath(containingClass);
            RequestPath[] methodRequestPaths = JaxrsAnnotationHelper.getRequestPaths(psiMethod);
            if (methodRequestPaths != null) {
                for (RequestPath methodRequestPath : methodRequestPaths) {
                    paths.add(joinPath(ctrlPath, methodRequestPath.getPath()));
                }
            } else {
                methodPath = JaxrsAnnotationHelper.getMethodUriPath(psiMethod);
                paths.add(joinPath(ctrlPath, methodPath));
            }
            return deduplicate(paths);
        }

        if (ctrlPath == null) {
            return paths;
        }
        paths.add(joinPath(ctrlPath, methodPath));
        return deduplicate(paths);
    }

    @NotNull
    public String buildServiceUriPathWithParams() {
        String params = PsiMethodHelper.create(psiMethod).buildParamString();
        List<String> serviceUriPaths = buildServiceUriPaths();
        if (serviceUriPaths.isEmpty()) {
            return "/";
        }
        String serviceUriPath = serviceUriPaths.get(0);
        return appendQuery(serviceUriPath, params);
    }

    @NotNull
    public List<String> buildServiceUriPathsWithParams() {
        String params = PsiMethodHelper.create(psiMethod).buildParamString();
        List<String> serviceUriPaths = buildServiceUriPaths();
        List<String> urlWithParams = new ArrayList<>();
        for (String serviceUriPath : serviceUriPaths) {
            urlWithParams.add(appendQuery(serviceUriPath, params));
        }
        return deduplicate(urlWithParams);
    }

    //包含 "RestController" "Controller"
    public static boolean isSpringRestSupported(@Nullable PsiClass containingClass) {
        PsiModifierList modifierList = containingClass == null ? null : containingClass.getModifierList();
        if (modifierList == null) {
            return false;
        }

        return modifierList.findAnnotation(SpringControllerAnnotation.REST_CONTROLLER.getQualifiedName()) != null ||
                modifierList.findAnnotation(SpringControllerAnnotation.CONTROLLER.getQualifiedName()) != null;
    }

    //包含 "RestController" "Controller"
    public static boolean isJaxrsRestSupported(@Nullable PsiClass containingClass) {
        PsiModifierList modifierList = containingClass == null ? null : containingClass.getModifierList();
        if (modifierList == null) {
            return false;
        }

        return modifierList.findAnnotation(JaxrsRequestAnnotation.PATH.getQualifiedName()) != null;
    }


    /* 生成完整 URL , 附带参数 */
    @NotNull
    public String buildFullUrlWithParams() {

        String fullUrl = buildFullUrl();

        String params = buildParamString();

        // RequestMapping 注解设置了 param
        if (!params.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder(fullUrl);
            return urlBuilder.append(fullUrl.contains("?") ? "&" : "?").append(params).toString();
        }
        return fullUrl;
    }

    @NotNull
    public String buildFullUrl() {

        String hostUri = myModule != null ? ModuleHelper.create(myModule).getServiceHostPrefix() : ModuleHelper.DEFAULT_URI;

        String servicePath = buildServiceUriPath();

        return hostUri.concat(servicePath);
    }

    @NotNull
    private static String joinPath(String ctrlPath, String methodPath) {
        return RequestPath.join(ctrlPath, methodPath);
    }

    @NotNull
    private static String appendQuery(String url, String params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        return new StringBuilder(url).append(url.contains("?") ? "&" : "?").append(params).toString();
    }

    @NotNull
    private static List<String> deduplicate(List<String> paths) {
        List<String> result = new ArrayList<>();
        for (String path : paths) {
            if (!result.contains(path)) {
                result.add(path);
            }
        }
        return result;
    }

}
