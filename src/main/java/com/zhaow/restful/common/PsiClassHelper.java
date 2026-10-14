package com.zhaow.restful.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.index.JavaShortClassNameIndex;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

// 处理 实体自关联，第二层自关联字段
public class PsiClassHelper {
    PsiClass psiClass;

    private Module myModule;

    protected PsiClassHelper(@NotNull PsiClass psiClass) {
        this.psiClass = psiClass;
    }

    public static PsiClassHelper create(@NotNull PsiClass psiClass) {
        return new PsiClassHelper(psiClass);
    }

    @NotNull
    protected Project getProject() {
        return psiClass.getProject();
    }


    public String convertClassToJSON(String className, Project project) {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        String queryJson;
        if (className.contains("List<")) { //参数为 List
            List<Map<String,Object>> jsonList = new ArrayList<>();

            // 没处理泛型嵌套，
            String entityName = className.substring(className.indexOf("<")+1,className.lastIndexOf(">"));

            // build RequestBody Json
            Map<String, Object> jsonMap = assembleClassToMap(entityName, project);
            jsonList.add(jsonMap);
            queryJson = gson.toJson(jsonList);
        } else {
            // build RequestBody Json
            queryJson = convertPojoEntityToJSON(className, project);
        }
        return queryJson;
    }

    private String convertPojoEntityToJSON(String className, Project project) {
        String queryJson;
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        Gson gson = gsonBuilder.create() ;
        Map<String, Object> jsonMap = assembleClassToMap(className, project);
        queryJson =  gson.toJson(jsonMap) ;
        return queryJson;
    }


    public String convertClassToJSON(Project project , boolean prettyFormat) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        if(prettyFormat) gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create() ;
        Map<String, Object> jsonMap = new HashMap<>();

        if(psiClass != null){
            jsonMap = assembleClassToMap(psiClass, project);
        }
        String queryJson =  gson.toJson(jsonMap) ;
        return queryJson;
    }


    @Nullable
    public static Object getJavaBaseTypeDefaultValue(String paramType) {
        Object paramValue = null;
//        todo: using map later
        switch (paramType.toLowerCase()) {
            case "byte": paramValue = Byte.valueOf("1");break;
            case "char": paramValue = Character.valueOf('Z');break;
            case "character": paramValue = Character.valueOf('Z');break;
            case "boolean": paramValue = Boolean.TRUE;break;
            case "int": paramValue = Integer.valueOf(1);break;
            case "integer": paramValue = Integer.valueOf(1);break;
            case "double": paramValue = Double.valueOf(1);break;
            case "float": paramValue = Float.valueOf(1.0F);break;
            case "long": paramValue = Long.valueOf(1L);break;
            case "short": paramValue = Short.valueOf("1");break;
            case "bigdecimal": return BigDecimal.ONE;
            case "string": paramValue = "demoData";break;
            case "date": paramValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());break; // todo: format date
            case "localdatetime": paramValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());break; // todo: format date
//            default: paramValue = paramType;
        }
        return paramValue;
    }


    private boolean isJavaBaseType(String typeName) {
        return getJavaBaseTypeDefaultValue(typeName) != null;
    }


    @Nullable
    public PsiClass findOnePsiClassByClassName(String qualifiedClassName, Project project) {
//        return findOnePsiClassByClassName_deprecated(className, project);
//        return findOnePsiClassByClassName2(className, project);
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, GlobalSearchScope.allScope(project));
        return psiClass;
    }

    @Nullable
    public PsiClass findOnePsiClassByClassName2(String className, Project project) {
        PsiClass psiClass = null;

        String shortClassName = className.substring(className.lastIndexOf(".") + 1, className.length());

//        psiClass.getPrimaryConstructor().getText(); // (val id: Long, val content: String)
//        psiClass.getFqName(); // class fullQualifiedName :org.jetbrains.kotlin.demo.Greeting

        PsiClass[] psiClasses = tryDetectPsiClassByShortClassName2( shortClassName,project);
        if (psiClasses.length == 0) {

            return null;
        }
        if (psiClasses.length == 1) {
            psiClass = psiClasses[0];
            return psiClass;
        }

        if (psiClasses.length > 1) {
            Optional<PsiClass> any = Arrays.stream(psiClasses).filter(tempPsiClass -> tempPsiClass.getQualifiedName().equals(className)).findAny();
            if (any.isPresent()) {
                psiClass = any.get();
            }

            for (PsiClass aClass : psiClasses) {

            }


            //找import中对应的class
//            psiClass = psiClassCollection.stream().filter(tempKtClass -> tempPsiClass.getQualifiedName().equals(className)).findFirst().get();

            /*Optional<PsiClass> any = psiClassCollection.stream().filter(tempPsiClass -> tempPsiClass.getQualifiedName().equals(className)).findAny();

            if (any.isPresent()) {
                psiClass = any.get();
            }*/

           /* for (KtClassOrObject ktClassOrObject : ktClassOrObjects) {
//                ktClassOrObject.
            }*/

        }
        return psiClass;
    }
    public PsiClass[] tryDetectPsiClassByShortClassName2(String shortClassName,Project project) {

        PsiClass[] psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName(shortClassName, GlobalSearchScope.allScope(project));// 所有的

        if (psiClasses != null && psiClasses.length > 0) {
            return psiClasses;
        }

        if(myModule != null) {
            psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName(shortClassName, GlobalSearchScope.allScope(project));// 所有的
            if (psiClasses != null && psiClasses.length > 0) {
                return psiClasses;
            }
        }

        return new PsiClass[0];
    }


    public Map<String, Object> assembleClassToMap(String className, Project project) {
        PsiClass psiClass = findOnePsiClassByClassName(className, project);

        Map<String, Object> jsonMap = new HashMap<>();
        if(psiClass != null){
            jsonMap = assembleClassToMap(psiClass, project);
        }
        return jsonMap;
    }

    public Map<String, Object> assembleClassToMap(PsiClass psiClass, Project project) {
        int defaultRecursiveCount = 1;
        return assembleClassToMap(psiClass, project, defaultRecursiveCount);
    }

    public Map<String, Object> assembleClassToMap(PsiClass psiClass, Project project,int recursiveCount) {

        Map<String, Object> map = new LinkedHashMap<>();
        PsiField[] fields = psiClass.getFields();
        for (PsiField field : fields) {
            PsiType psiFieldType = field.getType();
            String typeName = psiFieldType.getPresentableText();

            String fieldName = field.getName();

//            common base type
            if (isJavaBaseType(typeName)) {
                map.put(fieldName, getJavaBaseTypeDefaultValue(typeName));
                continue;
            }

            if (psiFieldType instanceof PsiArrayType) {
                PsiType psiType = ((PsiArrayType) psiFieldType).getComponentType();

                Object baseTypeDefaultValue = getJavaBaseTypeDefaultValue( psiType.getPresentableText() );
                if (baseTypeDefaultValue != null) {
                    List<Object> objects = new ArrayList<>();
                    objects.add(baseTypeDefaultValue);
                    map.put(fieldName, objects );
                }

                continue;
            }

            PsiClass resolveClass = psiFieldType instanceof PsiClassReferenceType
                    ? ((PsiClassReferenceType) psiFieldType).resolve() : null;
            if (resolveClass == null) {
                map.put(fieldName, typeName);
                continue;
            }

            if (isEnum(psiFieldType)) {
                PsiField[] enumFields = resolveClass.getFields();
                if (enumFields.length > 0) {
                    map.put(fieldName, enumFields[0].getName());
                }
                continue;
            }

//            self recursion
            if (Objects.equals(resolveClass.getQualifiedName(), psiClass.getQualifiedName())) {
                if (recursiveCount > 0) {
                    Map<String, Object> objectMap = assembleClassToMap(resolveClass, project,0);
                    map.put(fieldName, objectMap);
                    continue;
                }
            }

//            recursive

            if (isListFieldType(psiFieldType)) {
                PsiType[] parameters = ((PsiClassReferenceType) psiFieldType).getParameters();
                if (parameters != null && parameters.length > 0) {
                    PsiType parameter = parameters[0];
// 自关联
                    if (recursiveCount <= 0 )  {
                        continue;
                    }

                    if (parameter.getPresentableText().equals(psiClass.getName())) {
                        Map<String, Object> objectMap = assembleClassToMap(psiClass, project, 0);
                        map.put(fieldName, objectMap);
                        continue;
                    }

                    Object baseTypeDefaultValue = getJavaBaseTypeDefaultValue( parameter.getPresentableText() );
                    if (baseTypeDefaultValue != null) {
                        List<Object> objects = new ArrayList<>();
                        objects.add(baseTypeDefaultValue);
                        map.put(fieldName, objects );
                        continue;
                    }

                    // Resolve List<T> object element (including generic declaration text).
                    if (parameter instanceof PsiClassReferenceType) {
                        String canonicalText = parameter.getCanonicalText();
                        if (canonicalText == null || canonicalText.isEmpty()) {
                            continue;
                        }

                        int genericStart = canonicalText.indexOf('<');
                        if (genericStart > 0) {
                            canonicalText = canonicalText.substring(0, genericStart);
                        }
                        PsiClass onePsiClassByClassName = findOnePsiClassByClassName(canonicalText, project);
                        if (onePsiClassByClassName == null) {
                            continue;
                        }

                        Map<String, Object> objectMap = assembleClassToMap(onePsiClassByClassName, project, 0);
                        map.put(fieldName, objectMap);
                        continue;
                    }
                }

            }
        }

        return map;
    }


    /* 字段是否为List 类型*/
    private static boolean isListFieldType(PsiType psiFieldType) {
        if (! (psiFieldType instanceof PsiClassReferenceType)) {
            return false;
        }

        PsiClass resolvePsiClass = ((PsiClassReferenceType) psiFieldType).resolve();
        if (resolvePsiClass == null) {
            return false;
        }
        if ("java.util.List".equals(resolvePsiClass.getQualifiedName())) {
            return true ;
        }

        /*if (resolvePsiClass.getSuperClass().getQualifiedName().equals("java.util.List")) {
            return true ;
        }*/

        for (PsiType psiType : ((PsiClassReferenceType) psiFieldType).rawType().getSuperTypes()) {
            if (psiType.getCanonicalText().equals("java.util.List")) {
                return true;
            }
        }

        return false;

/*        ((PsiClassReferenceType) psiFieldType).rawType().getCanonicalText().equals("java.util.List");

        resolvePsiClass.getInterfaces();*/

//        rawType().getSuperTypes()/*
    }

    /* 字段是否为List 类型*/
    private static boolean isEnum(PsiType psiFieldType) {
        if (! (psiFieldType instanceof PsiClassReferenceType)) {
            return false;
        }
        PsiClass resolveClass = ((PsiClassReferenceType) psiFieldType).resolve();
        return resolveClass != null && resolveClass.isEnum();
    }


    public PsiClassHelper withModule(Module module) {
        this.myModule = module;
        return this;
    }
}
