package com.zhaow.restful.navigation.action;

import com.intellij.openapi.application.ApplicationInfo;

/**
 * The compact caption toolbar and the two-column list are offered from IDEA 2022.3 (build 223) up.
 *
 * <p>The obvious gate would be {@code ExperimentalUI#isNewUI}, but that class is
 * {@link org.jetbrains.annotations.ApiStatus.Internal} and the platform exposes no public replacement
 * for it, so the plugin verifier rejects it. Reading the properties {@code ExperimentalUI} keeps
 * itself would only look public while depending on another class's private state. Build baseline is
 * public and stable; the cost is that Classic UI on 223+ gets the same layout, which is cosmetic.
 */
final class RestSearchUi {
    private RestSearchUi() {
    }

    static boolean useToolbar() {
        // 2022.2 had an experimental preview; intentionally retain its classic popup.
        return ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 223;
    }
}
