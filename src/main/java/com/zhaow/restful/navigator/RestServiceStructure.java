package com.zhaow.restful.navigator;


import com.intellij.icons.AllIcons;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.OpenSourceUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.zhaow.restful.common.KtFunctionHelper;
import com.zhaow.restful.common.PsiMethodHelper;
import com.zhaow.restful.common.ToolkitIcons;
import com.zhaow.restful.method.HttpMethod;
import com.zhaow.restful.navigation.action.RestServiceItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtNamedFunction;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeModelListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Backing model of the Services tree.
 * <p>
 * The tree used to extend {@code SimpleTreeStructure} without ever installing a builder that
 * renders it, so nothing was displayed. It now exposes a plain lazy {@link TreeModel} wrapped in
 * an {@link AsyncTreeModel}, which fetches children off the EDT.
 */
public class RestServiceStructure {
    public static final Logger LOG = Logger.getInstance(RestServiceStructure.class);

    private final Project myProject;
    private final RestServiceProjectsManager myProjectsManager;
    private final SimpleTree myTree;
    private final RestServiceDetail myRestServiceDetail;
    private final ServiceTreeModel myModel = new ServiceTreeModel();
    private final AsyncTreeModel myAsyncModel;

    public RestServiceStructure(@NotNull Project project,
                                @NotNull RestServiceProjectsManager projectsManager,
                                @NotNull SimpleTree tree) {
        myProject = project;
        myProjectsManager = projectsManager;
        myTree = tree;
        myRestServiceDetail = RestServiceDetail.getInstance(project);

        myAsyncModel = new AsyncTreeModel(myModel, project);
        tree.setModel(myAsyncModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new ServiceRenderer());
        tree.setToggleClickCount(2);

        tree.addTreeSelectionListener(e -> showSelectedServiceDetail());
        // Enter navigates; the platform's own double-click handling covers the mouse case.
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateToSelected();
                }
            }
        });
    }

    /**
     * Scanning every module reads indexes and can take seconds, so it runs off the EDT.
     * {@code coalesceBy} keeps only the most recent request when the tool window is
     * toggled repeatedly.
     */
    public void update() {
        ReadAction.nonBlocking(this::scanServiceProjects)
                .coalesceBy(this, myProject)
                .expireWith(myProject)
                .inSmartMode(myProject)
                .finishOnUiThread(ModalityState.defaultModalityState(), this::updateProjects)
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    @NotNull
    private List<RestServiceProject> scanServiceProjects() {
        return myProjectsManager.getServiceProjects();
    }

    public void updateProjects(@NotNull List<RestServiceProject> projects) {
        myModel.setModules(projects);
        myAsyncModel.treeStructureChanged(new TreePath(myModel.getRoot()));
        if (projects.isEmpty()) {
            myRestServiceDetail.resetRequestTabbedPane();
        }
    }

    @NotNull
    public List<RestServiceItem> getSelectedServices() {
        List<RestServiceItem> result = new ArrayList<>();
        for (TreePath path : myTree.getSelectionPaths()) {
            if (path != null && path.getLastPathComponent() instanceof ServiceNode) {
                result.add(((ServiceNode) path.getLastPathComponent()).item);
            }
        }
        return result;
    }

    public boolean hasSelection() {
        return !getSelectedServices().isEmpty();
    }

    /**
     * Called when the user selects a leaf: fills the request panel below the tree.
     */
    public void showSelectedServiceDetail() {
        List<RestServiceItem> selected = getSelectedServices();
        if (selected.size() != 1) {
            resetDetail();
            return;
        }
        showServiceDetail(selected.get(0));
    }

    /**
     * Called on double-click / Enter: opens the declaration.
     */
    public void navigateToSelected() {
        List<RestServiceItem> selected = getSelectedServices();
        if (selected.isEmpty()) {
            return;
        }
        RestServiceItem item = selected.get(0);
        PsiElement psiElement = item.getPsiElement();

        if (psiElement == null || !psiElement.isValid()) {
            // Navigating into a stale element throws PsiInvalidElementAccessException;
            // rebuild the tree instead and let the user retry against live PSI.
            LOG.warn("service element is no longer valid, refreshing tree: " + item.getUrl());
            RestServicesNavigator.getInstance(myProject).scheduleStructureUpdate();
            return;
        }

        if (psiElement.getLanguage() == JavaLanguage.INSTANCE && psiElement instanceof PsiMethod) {
            OpenSourceUtil.navigate((PsiMethod) psiElement);
        } else if (psiElement instanceof KtNamedFunction) {
            OpenSourceUtil.navigate((KtNamedFunction) psiElement);
        }
    }

    /**
     * 显示服务详情，url
     */
    private void showServiceDetail(@NotNull RestServiceItem serviceItem) {
        myRestServiceDetail.resetRequestTabbedPane();

        String method = serviceItem.getMethod() != null
                ? String.valueOf(serviceItem.getMethod()) : HttpMethod.GET.name();
        myRestServiceDetail.setMethodValue(method);
        myRestServiceDetail.setUrlValue(serviceItem.getFullUrl());

        String requestParams = "";
        String requestBodyJson = "";
        PsiElement psiElement = serviceItem.getPsiElement();

        if (psiElement == null || !psiElement.isValid()) {
            // The controller was edited or deleted since the tree was built.
            myRestServiceDetail.addRequestParamsTab(requestParams);
            return;
        }

        if (psiElement instanceof PsiMethod) {
            PsiMethodHelper helper = PsiMethodHelper.create((PsiMethod) psiElement)
                    .withModule(serviceItem.getModule());
            requestParams = helper.buildParamString();
            requestBodyJson = helper.buildRequestBodyJson();
        } else if (psiElement instanceof KtNamedFunction) {
            KtFunctionHelper helper = KtFunctionHelper.create((KtNamedFunction) psiElement);
            if (helper != null) {
                helper.withModule(serviceItem.getModule());
                requestParams = helper.buildParamString();
                requestBodyJson = helper.buildRequestBodyJson();
            }
        }

        myRestServiceDetail.addRequestParamsTab(requestParams);
        if (requestBodyJson != null && !requestBodyJson.isBlank()) {
            myRestServiceDetail.addRequestBodyTabPanel(requestBodyJson);
        }
    }

    /**
     * Called when the user selects the root or a module node: clears the request panel.
     */
    public void resetDetail() {
        myRestServiceDetail.resetRequestTabbedPane();
        myRestServiceDetail.setMethodValue(HttpMethod.GET.name());
        myRestServiceDetail.setUrlValue("URL");
        myRestServiceDetail.initTab();
    }

    // ---------------------------------------------------------------- node types

    /** Root node, labelled with the number of discovered services. */
    static final class RootNode {
    }

    static final class ModuleNode {
        final RestServiceProject project;

        ModuleNode(RestServiceProject project) {
            this.project = project;
        }
    }

    static final class ServiceNode {
        final RestServiceItem item;

        ServiceNode(RestServiceItem item) {
            this.item = item;
        }
    }

    // ---------------------------------------------------------------- tree model

    /**
     * Children are created on demand, so expanding a module only allocates the nodes the user
     * actually opens. {@link AsyncTreeModel} calls into this from a pooled thread.
     */
    private final class ServiceTreeModel implements TreeModel {
        private final List<TreeModelListener> myListeners = new ArrayList<>();
        private final RootNode myRoot = new RootNode();
        private List<ModuleNode> myModules = List.of();
        private int myServiceCount;

        void setModules(@NotNull List<RestServiceProject> projects) {
            List<ModuleNode> modules = new ArrayList<>(projects.size());
            int count = 0;
            for (RestServiceProject project : projects) {
                modules.add(new ModuleNode(project));
                count += project.serviceItems.size();
            }
            myModules = List.copyOf(modules);
            myServiceCount = count;
        }

        @Override
        public Object getRoot() {
            return myRoot;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent == myRoot) {
                return myModules.get(index);
            }
            if (parent instanceof ModuleNode) {
                return new ServiceNode(((ModuleNode) parent).project.serviceItems.get(index));
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent == myRoot) {
                return myModules.size();
            }
            if (parent instanceof ModuleNode) {
                return ((ModuleNode) parent).project.serviceItems.size();
            }
            return 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof ServiceNode;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            // read-only tree
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent == myRoot && child instanceof ModuleNode) {
                return myModules.indexOf(child);
            }
            if (parent instanceof ModuleNode && child instanceof ServiceNode) {
                return ((ModuleNode) parent).project.serviceItems.indexOf(((ServiceNode) child).item);
            }
            return -1;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            myListeners.add(l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            myListeners.remove(l);
        }

        String rootLabel() {
            return myServiceCount > 0 ? String.format("Found %d services ", myServiceCount) : null;
        }
    }

    // ---------------------------------------------------------------- renderer

    private final class ServiceRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(@NotNull javax.swing.JTree tree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean leaf,
                                          int row,
                                          boolean hasFocus) {
            if (value instanceof RootNode) {
                String label = myModel.rootLabel();
                append(label == null ? "No services" : label, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                setIcon(AllIcons.Nodes.ModuleGroup);
                return;
            }
            if (value instanceof ModuleNode) {
                append(((ModuleNode) value).project.getModuleName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                setIcon(ToolkitIcons.MODULE);
                return;
            }
            if (value instanceof ServiceNode) {
                RestServiceItem item = ((ServiceNode) value).item;
                append(item.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                setIcon(ToolkitIcons.METHOD.get(item.getMethod()));
                String location = item.getPresentation() == null
                        ? null : item.getPresentation().getLocationString();
                if (location != null) {
                    setToolTipText(location);
                }
            }
        }
    }
}
