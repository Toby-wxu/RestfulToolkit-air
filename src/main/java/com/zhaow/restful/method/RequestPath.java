package com.zhaow.restful.method;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RequestPath {
    String path;
    String method;

    public RequestPath(String path, String method) {
        this.path = path;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * {@code @RequestMapping("/x")} on the class and {@code @GetMapping("y")} on the method must
     * produce {@code /x/y} regardless of which side carries the slash.
     */
    @NotNull
    public static String join(@Nullable String ctrlPath, @Nullable String methodPath) {
        if (ctrlPath == null || ctrlPath.isEmpty()) {
            ctrlPath = "/";
        }
        if (!ctrlPath.startsWith("/")) {
            ctrlPath = "/" + ctrlPath;
        }
        if (!ctrlPath.endsWith("/")) {
            ctrlPath = ctrlPath + "/";
        }
        if (methodPath == null) {
            methodPath = "";
        }
        if (methodPath.startsWith("/")) {
            methodPath = methodPath.substring(1);
        }
        return ctrlPath + methodPath;
    }
}
