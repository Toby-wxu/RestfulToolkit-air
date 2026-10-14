/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhaow.restful.navigation.action;

import com.intellij.ide.util.gotoByName.ChooseByNameItemProvider;
import com.intellij.ide.util.gotoByName.ChooseByNameModel;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.zhaow.utils.ToolkitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

public class RestServiceChooseByNamePopup extends ChooseByNamePopup {
  public static final Key<RestServiceChooseByNamePopup> CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY = new Key<>("ChooseByNamePopup");

  protected RestServiceChooseByNamePopup(@Nullable Project project, @NotNull ChooseByNameModel model, @NotNull ChooseByNameItemProvider provider, @Nullable ChooseByNamePopup oldPopup, @Nullable String predefinedText, boolean mayRequestOpenInCurrentWindow, int initialIndex) {
    super(project, model, provider, oldPopup, predefinedText, mayRequestOpenInCurrentWindow, initialIndex);
  }

  private @NotNull List<JComponent> myToolComponents = List.of();
  private boolean myToolbarInstalled;

  private boolean usesToolbarUi() {
    return getModel() instanceof GotoRequestMappingModel model && model.usesToolbarUi();
  }

  /**
   * Drop-downs for the caption row, in display order. {@code ChooseByNameBase#setToolArea} cannot be
   * used: {@code ChooseByNameFilter} claims that single slot from its own constructor.
   */
  public void setToolComponents(@NotNull List<JComponent> components) {
    myToolComponents = components;
  }

  @Override
  protected void showTextFieldPanel() {
    insertIntoToolsRow();
    if (usesToolbarUi()) {
      installToolbarLayout();
    }
    super.showTextFieldPanel();
  }

  private void installToolbarLayout() {
    if (myToolbarInstalled) return;
    Container parent = getTextField().getParent();
    if (!(parent instanceof JComponent textPanel) || parent.getComponentCount() == 0
            || !(parent.getComponent(0) instanceof JPanel caption)
            || !(caption.getLayout() instanceof BorderLayout layout)) return;

    Component title = layout.getLayoutComponent(BorderLayout.WEST);
    if (title instanceof JLabel label) {
      label.setFont(UIUtil.getLabelFont().deriveFont(Font.PLAIN));
    }
    caption.setBorder(JBUI.Borders.empty(8, 4, 10, 0));
    caption.setBackground(JBColor.namedColor("Popup.Header.background", UIUtil.getPanelBackground()));
    textPanel.setBackground(caption.getBackground());
    textPanel.setBorder(JBUI.Borders.empty(8, 12, 10, 12));
    // Use a minimum rather than a fixed popup size; long paths and IDE scaling remain usable.
    Dimension minimum = textPanel.getPreferredSize();
    textPanel.setMinimumSize(new Dimension(Math.max(JBUI.scale(640), minimum.width), minimum.height));
    getTextField().setColumns(56);
    getTextField().getAccessibleContext().setAccessibleName("Search service URL");
    getTextField().setToolTipText("Search a service URL; use the toolbar to filter modules and HTTP methods");
    myToolbarInstalled = true;
  }

  @Override
  public String getAdText() {
    if (!usesToolbarUi()) return super.getAdText();
    Object selected = myList.getSelectedValue();
    String location = selected instanceof RestServiceItem item ? RestServiceCellRenderer.sourceLocation(item) : "";
    long count = 0;
    for (int index = 0; index < myList.getModel().getSize(); index++) {
      if (myList.getModel().getElementAt(index) instanceof RestServiceItem) count++;
    }
    return "<html>" + StringUtil.escapeXmlEntities(location.isEmpty() ? "Select an endpoint" : location)
            + "<br>↑ ↓ Navigate &nbsp; Enter Go to source &nbsp; Alt ↑ / ↓ History &nbsp; · &nbsp; "
            + count + " endpoints</html>";
  }

  @Override
  protected void chosenElementMightChange() {
    super.chosenElementMightChange();
    updateToolbarFooter();
  }

  @Override
  protected void showList() {
    super.showList();
    updateToolbarFooter();
  }

  private void updateToolbarFooter() {
    if (usesToolbarUi() && myDropdownPopup != null && !myDropdownPopup.isDisposed()) {
      myDropdownPopup.setAdText(getAdText(), SwingConstants.LEFT);
    }
  }

  /**
   * The selectors get a row of their own in the caption's East slot. The platform's tool row cannot
   * host them: it also carries an empty 24px tool-area placeholder and the nowrap action toolbar, and
   * the width those claim leaves the selectors well short of the right edge.
   */
  private void insertIntoToolsRow() {
    if (myToolComponents.isEmpty()) {
      return;
    }
    Container textPanel = getTextField().getParent();
    if (textPanel.getComponentCount() == 0 || !(textPanel.getComponent(0) instanceof JPanel caption)) {
      return;
    }
    if (!(caption.getLayout() instanceof BorderLayout layout)) {
      return;
    }
    JPanel selectors = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
    selectors.setOpaque(false);
    for (JComponent component : myToolComponents) {
      if (component.getParent() == null) {
        selectors.add(component);
      }
    }
    Component platformTools = layout.getLayoutComponent(BorderLayout.EAST);
    if (platformTools != null) {
      caption.remove(platformTools);
    }
    caption.add(selectors, BorderLayout.EAST);
  }

  public static RestServiceChooseByNamePopup createPopup(final Project project,
                                                         @NotNull final ChooseByNameModel model,
                                                         @NotNull ChooseByNameItemProvider provider,
                                                         @Nullable final String predefinedText,
                                                         boolean mayRequestOpenInCurrentWindow,
                                                         final int initialIndex) {
      if (!StringUtil.isEmptyOrSpaces(predefinedText)) {
          return new RestServiceChooseByNamePopup(project, model, provider, null, predefinedText, mayRequestOpenInCurrentWindow, initialIndex);
      }

    final RestServiceChooseByNamePopup oldPopup = project == null ? null : project.getUserData(CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY);
    if (oldPopup != null) {
      oldPopup.close(false);
    }
    RestServiceChooseByNamePopup newPopup = new RestServiceChooseByNamePopup(project, model, provider, oldPopup, predefinedText, mayRequestOpenInCurrentWindow, initialIndex);

    if (project != null) {
      project.putUserData(CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, newPopup);
    }
    return newPopup;
  }

  @Override
  public String transformPattern(String pattern) {
    final ChooseByNameModel model = getModel();
    return getTransformedPattern(pattern, model);
  }

//TODO: resolve PathVariable
  public static String getTransformedPattern(String pattern, ChooseByNameModel model) {
    if (! (model instanceof GotoRequestMappingModel) ) {
      return pattern;
    }

    pattern = ToolkitUtil.removeRedundancyMarkup(pattern);;
    return pattern;
  }


  @Nullable
  public String getMemberPattern() {
    final String enteredText = getTrimmedText();
    final int index = enteredText.lastIndexOf('#');
    if (index == -1) {
      return null;
    }

    String name = enteredText.substring(index + 1).trim();
    return StringUtil.isEmptyOrSpaces(name) ? null : name;
  }

/*  public void repaintList() {
    myRepaintQueue.cancelAllUpdates();
    myRepaintQueue.queue(new Update(this) {
      @Override
      public void run() {
        RestServiceChooseByNamePopup.this.repaintListImmediate();
      }
    });
  }*/

}
