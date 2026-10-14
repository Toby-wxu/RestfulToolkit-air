package com.zhaow.restful.navigation.action;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.text.MatcherHolder;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.zhaow.restful.method.HttpMethod;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Compact New UI row: method badge, highlighted URL and a separate controller column. */
final class RestServiceCellRenderer extends JPanel implements ListCellRenderer<Object> {
    private final ListCellRenderer<Object> fallback;
    private final JLabel method = new JLabel("", SwingConstants.CENTER) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = antialiased(g);
            g2.setColor(getBackground());
            int arc = JBUI.scale(8);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
            g2.dispose();
            super.paintComponent(g);
        }
    };
    private final SimpleColoredComponent path = new SimpleColoredComponent();
    private final JLabel controller = new JLabel();
    private final JPanel content = new JPanel(new BorderLayout(JBUI.scale(12), 0));
    private final Font codeFont;
    private Color rowBackground = UIUtil.getListBackground();
    private boolean selectedRow;

    @SuppressWarnings("unchecked")
    RestServiceCellRenderer(ListCellRenderer<?> fallback) {
        super(new BorderLayout(JBUI.scale(10), 0));
        this.fallback = (ListCellRenderer<Object>) fallback;
        setOpaque(false);
        setBorder(JBUI.Borders.empty(7, 10));
        var scheme = EditorColorsManager.getInstance().getGlobalScheme();
        codeFont = JBFont.create(new Font(scheme.getEditorFontName(), Font.PLAIN, scheme.getEditorFontSize()));
        method.setOpaque(false);
        method.setBorder(JBUI.Borders.empty(2, 6));
        method.setPreferredSize(JBUI.size(60, 22)); // Includes OPTIONS and CONNECT.
        content.setOpaque(false);
        path.setOpaque(false);
        path.setIpad(JBUI.insets(0));
        content.add(path, BorderLayout.CENTER);
        content.add(controller, BorderLayout.EAST);
        add(method, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }

    /** The selection is an inset rounded band, so the panel itself stays transparent. */
    @Override
    protected void paintComponent(Graphics g) {
        if (!selectedRow) {
            return;
        }
        Graphics2D g2 = antialiased(g);
        g2.setColor(rowBackground);
        int inset = JBUI.scale(4);
        int arc = JBUI.scale(12);
        g2.fill(new RoundRectangle2D.Float(inset, 1, getWidth() - inset * 2, getHeight() - 2, arc, arc));
        g2.dispose();
    }

    private static Graphics2D antialiased(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g2;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                   boolean selected, boolean focused) {
        // Preserve the platform's "more results" and other non-service rows.
        if (!(value instanceof RestServiceItem item)) {
            return fallback.getListCellRendererComponent(list, value, index, selected, focused);
        }

        selectedRow = selected;
        rowBackground = selected ? UIUtil.getListSelectionBackground(true) : UIUtil.getListBackground();
        Color foreground = selected ? UIUtil.getListSelectionForeground(true) : UIUtil.getListForeground();
        path.clear();
        path.setFont(codeFont);
        path.setForeground(foreground);
        path.setBackground(rowBackground);
        String url = item.getUrl() == null ? "" : item.getUrl();
        SpeedSearchUtil.appendColoredFragmentForMatcher(url, path,
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, foreground),
                MatcherHolder.getAssociatedMatcher(list), rowBackground, selected);

        HttpMethod httpMethod = item.getMethod();
        method.setText(httpMethod == null ? "ANY" : httpMethod.name());
        method.setFont(UIUtil.getLabelFont().deriveFont(Font.PLAIN,
                Math.max(11f, UIUtil.getLabelFont().getSize2D() - 1)));
        method.setForeground(methodForeground(httpMethod));
        method.setBackground(methodBackground(httpMethod));

        String location = sourceLocation(item);
        int separator = location.lastIndexOf('#');
        controller.setText(separator < 0 ? location : location.substring(0, separator));
        controller.setFont(codeFont);
        controller.setForeground(selected ? foreground : UIUtil.getContextHelpForeground());
        setToolTipText(method.getText() + " " + url + "  " + location);
        getAccessibleContext().setAccessibleName(getToolTipText());
        return this;
    }

    static String sourceLocation(RestServiceItem item) {
        return ReadAction.compute(() -> {
            if (item.getPsiElement() == null || !item.getPsiElement().isValid()) return "";
            String location = item.getPresentation().getLocationString();
            if (location == null) return "";
            return location.startsWith("(") && location.endsWith(")")
                    ? location.substring(1, location.length() - 1) : location;
        });
    }

    private static Color methodForeground(HttpMethod method) {
        if (method == HttpMethod.GET) return new JBColor(0x166B42, 0x83CBA7);
        if (method == HttpMethod.DELETE) return new JBColor(0xAE3535, 0xF29A9A);
        if (method == HttpMethod.PUT || method == HttpMethod.PATCH) return new JBColor(0x805C08, 0xE8C477);
        return new JBColor(0x285CC0, 0x8AB0FA);
    }

    private static Color methodBackground(HttpMethod method) {
        if (method == HttpMethod.GET) return new JBColor(0xE8F5ED, 0x253D31);
        if (method == HttpMethod.DELETE) return new JBColor(0xFBECEC, 0x4A3033);
        if (method == HttpMethod.PUT || method == HttpMethod.PATCH) return new JBColor(0xFFF5DA, 0x463D27);
        return new JBColor(0xEAF0FF, 0x293B59);
    }
}
