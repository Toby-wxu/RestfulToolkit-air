package com.zhaow.restful.method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The class-level and method-level mapping paths each may or may not carry a leading and
 * trailing slash; this was previously implemented three times with slightly different results.
 */
class RequestPathTest {

    @Test
    void joinsSlashPrefixedClassPathWithBareMethodPath() {
        assertEquals("/product/list", RequestPath.join("/product", "list"));
    }

    @Test
    void collapsesDoubleSlashWhenBothSidesCarryOne() {
        assertEquals("/product/list", RequestPath.join("/product/", "/list"));
    }

    @Test
    void addsLeadingSlashWhenClassPathOmitsIt() {
        assertEquals("/product/list", RequestPath.join("product", "list"));
    }

    @Test
    void nullClassPathBehavesAsRoot() {
        assertEquals("/list", RequestPath.join(null, "list"));
    }

    @Test
    void emptyClassPathBehavesAsRoot() {
        assertEquals("/list", RequestPath.join("", "list"));
    }

    @Test
    void nullMethodPathYieldsClassPathWithTrailingSlash() {
        assertEquals("/product/", RequestPath.join("/product", null));
    }

    @Test
    void rootMappingWithRootMethod() {
        assertEquals("/", RequestPath.join("/", "/"));
    }
}
