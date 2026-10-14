package com.zhaow.restful.navigator;


import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import com.zhaow.restful.navigation.action.RestServiceItem;
import com.zhaow.utils.RestServiceDataKeys;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;


public class RestServicesNavigatorPanel extends SimpleToolWindowPanel implements DataProvider {

    private static final String SERVICE_MENU_ID = "Toolkit.NavigatorServiceMenu";

    private final Project myProject;
    private final SimpleTree myTree;
    RestServiceDetail myRestServiceDetail;

    private Splitter servicesContentPaneSplitter;


    public RestServicesNavigatorPanel(Project project, SimpleTree tree) {
        super(true, true);

        myProject = project;
        myTree = tree;
        myRestServiceDetail = RestServiceDetail.getInstance(project);

        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("RestToolkit Navigator Toolbar",
                (DefaultActionGroup) actionManager.getAction("Toolkit.NavigatorActionsToolbar"),
                true);
        setToolbar(actionToolbar.getComponent());

        myTree.setBorder(BorderFactory.createLineBorder(new Color(36, 38, 39)));
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);

        servicesContentPaneSplitter = new Splitter(true, .5f);
        servicesContentPaneSplitter.setShowDividerControls(true);
        servicesContentPaneSplitter.setDividerWidth(10);
        servicesContentPaneSplitter.setFirstComponent(scrollPane);
        servicesContentPaneSplitter.setSecondComponent(myRestServiceDetail);

        setContent(servicesContentPaneSplitter);

        myTree.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                RestServiceStructure structure = currentStructure();
                if (structure == null || !structure.hasSelection()) {
                    return;
                }
                ActionGroup actionGroup = (ActionGroup) actionManager.getAction(SERVICE_MENU_ID);
                if (actionGroup != null) {
                    actionManager.createActionPopupMenu("", actionGroup)
                            .getComponent()
                            .show(comp, x, y);
                }
            }
        });
    }

    @Nullable
    private RestServiceStructure currentStructure() {
        return RestServicesNavigator.getInstance(myProject).getStructure();
    }

    @Nullable
    public Object getData(@NonNls String dataId) {
        if (RestServiceDataKeys.SERVICE_ITEMS.is(dataId)) {
            RestServiceStructure structure = currentStructure();
            return structure == null ? List.of() : structure.getSelectedServices();
        }

        return super.getData(dataId);
    }
}
