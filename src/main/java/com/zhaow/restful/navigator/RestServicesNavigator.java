package com.zhaow.restful.navigator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import com.zhaow.utils.RestfulToolkitBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;

@Service(Service.Level.PROJECT)
public final class RestServicesNavigator {

	public static final Logger LOG = Logger.getInstance(RestServicesNavigator.class);

	public static final String TOOL_WINDOW_ID = "RestfulToolkit";

	protected final Project myProject;

	protected RestServiceStructure myStructure;

	private SimpleTree myTree;

	private ToolWindow myToolWindow;

	private RestServiceProjectsManager myProjectsManager;

	// Reset on hide so that re-opening the window refreshes again.
	private boolean refreshedSinceHidden = false;

	public RestServicesNavigator(Project myProject) {
		this.myProject = myProject;
		myProjectsManager = RestServiceProjectsManager.getInstance(myProject);
	}

	public static RestServicesNavigator getInstance(Project p) {
		return p.getService(RestServicesNavigator.class);
	}

	private void initTree() {
		myTree = new SimpleTree();
		myTree.getEmptyText().clear();
		myTree.getEmptyText().appendText(
				RestfulToolkitBundle.message("toolkit.navigator.nothing.to.display", "Refresh Services"));

		myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	}

	/**
	 * Called by {@link Factory} when the tool window is first shown.
	 */
	void createToolWindowContent(@NotNull ToolWindow toolWindow) {
		myToolWindow = toolWindow;
		initTree();
		myStructure = new RestServiceStructure(myProject, myProjectsManager, myTree);

		JPanel panel = new RestServicesNavigatorPanel(myProject, myTree);
		Content content = ContentFactory.getInstance().createContent(panel, "", false);
		toolWindow.getContentManager().addContent(content);

		scheduleStructureUpdate();
	}

	@Nullable
	public RestServiceStructure getStructure() {
		return myStructure;
	}

	public void stateChanged() {
		if (myToolWindow == null || myToolWindow.isDisposed()) {
			return;
		}
		boolean visible = myToolWindow.isVisible();
		if (!visible) {
			refreshedSinceHidden = false;
			return;
		}
		if (!refreshedSinceHidden) {
			refreshedSinceHidden = true;
			scheduleStructureUpdate();
		}
	}

	public void scheduleStructureUpdate() {
		if (myToolWindow == null || myStructure == null) {
			return;
		}
		ApplicationManager.getApplication().invokeLater(() -> {
			if (myToolWindow == null || !myToolWindow.isVisible() || myProject.isDisposed()) {
				return;
			}
			myStructure.update();
		});
	}

	/**
	 * Declarative registration via the {@code <toolWindow>} extension point replaces the
	 * deprecated {@code ToolWindowManagerEx.registerToolWindow}.
	 */
	public static final class Factory implements ToolWindowFactory, DumbAware {

		@Override
		public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
			RestServicesNavigator.getInstance(project).createToolWindowContent(toolWindow);
		}
	}
}
