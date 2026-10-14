package com.zhaow.restful.common.spring;


import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.zhaow.restful.annotations.SpringRequestMethodAnnotation;
import com.zhaow.restful.common.PsiAnnotationHelper;
import com.zhaow.restful.common.RestSupportedAnnotationHelper;
import com.zhaow.restful.method.RequestPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RequestMappingAnnotationHelper implements RestSupportedAnnotationHelper {


    /**
     * {@code getModifierList()} is null for anonymous classes and for some Kotlin light classes.
     */
    @NotNull
    private static PsiAnnotation[] annotationsOf(@Nullable PsiModifierListOwner owner) {
        if (owner == null) {
            return PsiAnnotation.EMPTY_ARRAY;
        }
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return PsiAnnotation.EMPTY_ARRAY;
        }
        PsiAnnotation[] annotations = modifierList.getAnnotations();
        return annotations == null ? PsiAnnotation.EMPTY_ARRAY : annotations;
    }

    /**
     * 过滤所有注解
     * @param psiClass
     * @return never null
     */
    @NotNull
    public static List<RequestPath> getRequestPaths(PsiClass psiClass) {
        PsiAnnotation[] annotations = annotationsOf(psiClass);

        List<RequestPath> list = new ArrayList<>();
        for (PsiAnnotation annotation : annotations) {
            for (SpringRequestMethodAnnotation mappingAnnotation : SpringRequestMethodAnnotation.values()) {
//            for (PathMappingAnnotation mappingAnnotation : PathMappingAnnotation.allPathMappingAnnotations) {
                if (mappingAnnotation.getQualifiedName().equals(annotation.getQualifiedName())) {
                    List<RequestPath> requestMappings = getRequestMappings(annotation, "");
                    if (!requestMappings.isEmpty()) {
                        list.addAll(requestMappings);
                    }
                }
            }
        }

        if (list.isEmpty()) {
            // TODO : 继承 RequestMapping
            PsiClass superClass = psiClass.getSuperClass();
            if (superClass != null && !"java.lang.Object".equals(superClass.getQualifiedName())) {
                list = getRequestPaths(superClass);
            } else {
                list.add(new RequestPath("/", null));
            }

        }

        return list;
    }

    @NotNull
    public static String[] getRequestMappingValues(PsiClass psiClass) {
        PsiAnnotation[] annotations = annotationsOf(psiClass);

        for (PsiAnnotation annotation : annotations) {
            if (annotation.getQualifiedName().equals(SpringRequestMethodAnnotation.REQUEST_MAPPING.getQualifiedName())) {
                return getRequestMappingValues(annotation);
            }
        }

        return new String[]{"/"};
    }

    /**
     * @param annotation
     * @param defaultValue
     * @return
     */
    private static List<RequestPath> getRequestMappings(PsiAnnotation annotation, String defaultValue) {
        List<RequestPath> mappingList = new ArrayList<>();

        SpringRequestMethodAnnotation requestAnnotation = SpringRequestMethodAnnotation.getByQualifiedName(annotation.getQualifiedName());

        if (requestAnnotation==null) {
            return new ArrayList<>();
        }

        List<String> methodList ;
        if (requestAnnotation.methodName() != null) {
            methodList = Arrays.asList(requestAnnotation.methodName()) ;
        } else { // RequestMapping 如果没有指定具体method，不写的话，默认支持所有HTTP请求方法
            methodList = PsiAnnotationHelper.getAnnotationAttributeValues(annotation, "method");
        }

        List<String> pathList = getPathValues(annotation);
        List<String> paramsList = PsiAnnotationHelper.getAnnotationAttributeValues(annotation, "params");

        // 没有设置 value，默认方法名
        if (pathList.size() == 0) {
            pathList.add(defaultValue);
        }

        // todo: 处理没有设置 value 或 path 的 RequestMapping

//        List<String> finalPathList = pathList;
//        methodList.forEach(method-> finalPathList.forEach(path->mappingList.add(new RequestMapping(path,method))));

        if (methodList.size() > 0) {
            for (String method : methodList) {
                for (String path : pathList) {
                    mappingList.add(new RequestPath(appendParams(path, paramsList), method));
                }
            }
        } else {
            for (String path : pathList) {
                mappingList.add(new RequestPath(appendParams(path, paramsList), null));
            }
        }

        return mappingList;
    }

    /**
     * 过滤所有注解
     * @param psiMethod
     * @return never null
     */
    @NotNull
    public static RequestPath[] getRequestPaths(PsiMethod psiMethod) {
        PsiAnnotation[] annotations = annotationsOf(psiMethod);

        List<RequestPath> list = new ArrayList<>();

        for (PsiAnnotation annotation : annotations) {
            for (SpringRequestMethodAnnotation mappingAnnotation : SpringRequestMethodAnnotation.values()) {
//            for (PathMappingAnnotation mappingAnnotation : PathMappingAnnotation.allPathMappingAnnotations) {
                if (mappingAnnotation.getQualifiedName().equals(annotation.getQualifiedName())) {

//                    String defaultValue = psiMethod.getName();
                    String defaultValue = "/";
                    List<RequestPath> requestMappings = getRequestMappings(annotation, defaultValue);
                    if (requestMappings.size()>0) {
                        list.addAll(requestMappings);
                    }
                }
            }
        }

        return list.toArray(new RequestPath[0]);
    }


    private static String getRequestMappingValue(PsiAnnotation annotation) {
        List<String> values = getPathValues(annotation);
        if (!values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    public static String[] getRequestMappingValues(PsiAnnotation annotation) {
        List<String> values = getPathValues(annotation);
        return values.toArray(new String[values.size()]);
    }

    @NotNull
    private static List<String> getPathValues(PsiAnnotation annotation) {
        List<String> values = new ArrayList<>(PsiAnnotationHelper.getAnnotationAttributeValues(annotation, "value"));
        List<String> pathValues = PsiAnnotationHelper.getAnnotationAttributeValues(annotation, "path");
        for (String pathValue : pathValues) {
            if (!values.contains(pathValue)) {
                values.add(pathValue);
            }
        }
        return values;
    }

    @NotNull
    private static String appendParams(String path, List<String> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            return path;
        }
        List<String> queryParts = new ArrayList<>();
        for (String param : paramsList) {
            if (param == null || param.isEmpty()) {
                continue;
            }
            // Only keep key=value constraints as URL query parts.
            if (param.contains("=")) {
                queryParts.add(param);
            }
        }
        if (queryParts.isEmpty()) {
            return path;
        }
        String separator = path.contains("?") ? "&" : "?";
        return path + separator + String.join("&", queryParts);
    }


    public static String getOneRequestMappingPath(PsiClass psiClass) {
        // todo: 有必要 处理 PostMapping,GetMapping 么？
        PsiAnnotation annotation = psiClass.getModifierList().findAnnotation(SpringRequestMethodAnnotation.REQUEST_MAPPING.getQualifiedName());

        String path = null;
        if (annotation != null) {
            path = RequestMappingAnnotationHelper.getRequestMappingValue(annotation);
        }

        return path != null ? path : "";
    }


    public static String getOneRequestMappingPath(PsiMethod psiMethod) {
//        System.out.println("psiMethod:::::::" + psiMethod);
        SpringRequestMethodAnnotation requestAnnotation = null;

        List<SpringRequestMethodAnnotation> springRequestAnnotations = Arrays.stream(SpringRequestMethodAnnotation.values()).filter(annotation ->
                psiMethod.getModifierList().findAnnotation(annotation.getQualifiedName()) != null
        ).collect(Collectors.toList());

       /* if (springRequestAnnotations.size() == 0) {
            requestAnnotation = null;
        }*/

        if (springRequestAnnotations.size() > 0) {
            requestAnnotation = springRequestAnnotations.get(0);
        }

        String mappingPath;
        if(requestAnnotation != null){
            PsiAnnotation annotation = psiMethod.getModifierList().findAnnotation(requestAnnotation.getQualifiedName());
            mappingPath = RequestMappingAnnotationHelper.getRequestMappingValue(annotation);
        }else {
            String methodName = psiMethod.getName();
            mappingPath = Introspector.decapitalize(methodName);
        }

        return mappingPath;
    }



}
