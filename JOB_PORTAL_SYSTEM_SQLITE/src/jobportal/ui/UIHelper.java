package jobportal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import jobportal.model.Application;
import jobportal.util.TextUtil;

/**
 * Shared colours, fonts and small UI helper methods, so every screen looks the same.
 */
public final class UIHelper {

    // ---------- colours ----------
    public static final Color PRIMARY = new Color(30, 64, 124);
    public static final Color SIDEBAR = new Color(24, 38, 66);
    public static final Color SIDEBAR_ACTIVE = new Color(50, 82, 140);
    public static final Color BACKGROUND = new Color(240, 243, 248);
    public static final Color SUCCESS = new Color(40, 140, 70);
    public static final Color DANGER = new Color(190, 50, 50);
    public static final Color WARNING = new Color(220, 120, 20);
    public static final Color GRAY = new Color(100, 110, 125);
    public static final Color TEXT_DARK = new Color(33, 37, 41);
    public static final Color TEXT_MUTED = new Color(110, 117, 130);
    public static final Color BORDER = new Color(218, 223, 231);

    // ---------- fonts ----------
    public static final Font FONT_NORMAL = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);

    private UIHelper() {
    }

    // ------------------------------------------------------------------
    // Buttons and labels
    // ------------------------------------------------------------------

    // Method overloading: button with a colour, or button with the default colour
    public static JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton createButton(String text) {
        return createButton(text, PRIMARY);
    }

    public static JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_TITLE);
        label.setForeground(PRIMARY);
        return label;
    }

    // ------------------------------------------------------------------
    // Panels
    // ------------------------------------------------------------------

    /** An empty page with a title on top. The caller adds the content in the CENTER. */
    public static JPanel createPagePanel(String title) {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        page.add(createTitle(title), BorderLayout.NORTH);
        return page;
    }

    public static JPanel createButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        return bar;
    }

    /** A white box with a coloured left edge, a small title and a big number. */
    public static JPanel createStatCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, color),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15))));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_NORMAL);
        titleLabel.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLabel.setForeground(color);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    /** White panel with a border and a heading, used to group content. */
    public static JPanel createSection(String heading, Component content) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel label = new JLabel(heading);
        label.setFont(FONT_HEADING);
        label.setForeground(TEXT_DARK);
        section.add(label, BorderLayout.NORTH);
        section.add(content, BorderLayout.CENTER);
        return section;
    }

    /** Adds "label + field" as one row of a GridBagLayout form. */
    public static void addFormRow(JPanel form, int row, String labelText, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        JLabel label = new JLabel(labelText);
        label.setFont(FONT_BOLD);
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(label, gbc);

        field.setFont(FONT_NORMAL);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, gbc);
    }

    // ------------------------------------------------------------------
    // Tables
    // ------------------------------------------------------------------

    /** Table model whose cells cannot be edited (we override isCellEditable). */
    public static DefaultTableModel createTableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT_NORMAL);
        table.setForeground(TEXT_DARK);
        table.setRowHeight(28);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(196, 214, 244));
        table.setSelectionForeground(TEXT_DARK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 32));
        return table;
    }

    public static JScrollPane wrapTable(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setColumnHeaderView(table.getTableHeader());     // show the column names on top
        return scroll;
    }

    /** Sets the preferred width of every column (widths is an array). */
    public static void setColumnWidths(JTable table, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Shows the status text in colour (green = selected, red = rejected ...). */
    public static void applyStatusRenderer(JTable table, int columnIndex) {
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(new StatusRenderer());
    }

    /** Cell renderer: a child of DefaultTableCellRenderer that overrides one method. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(FONT_BOLD);
            String status = String.valueOf(value);
            if (status.equals(Application.STATUS_SELECTED) || status.equals("ACTIVE") || status.equals("OPEN")) {
                setForeground(SUCCESS);
            } else if (status.equals(Application.STATUS_REJECTED) || status.equals("BLOCKED") || status.equals("CLOSED")) {
                setForeground(DANGER);
            } else if (status.equals(Application.STATUS_INTERVIEW) || status.equals(Application.STATUS_SHORTLISTED)) {
                setForeground(WARNING);
            } else {
                setForeground(PRIMARY);
            }
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Message dialogs
    // ------------------------------------------------------------------

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Job Portal", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showDbError(Component parent, SQLException e) {
        showError(parent, "Database error:\n" + e.getMessage()
                + "\n\nCheck that the SQLite database file (data/job_portal.db) is not locked or read-only.");
    }

    public static boolean confirm(Component parent, String message) {
        int choice = JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    // ------------------------------------------------------------------
    // Simple bar chart (custom painting)
    // ------------------------------------------------------------------

    private static final Color[] BAR_COLORS = {
        new Color(52, 110, 190), new Color(230, 150, 40), new Color(150, 90, 190),
        new Color(50, 160, 90), new Color(200, 70, 70)
    };

    /** A JPanel that draws bars for a Map (label -> number). It overrides paintComponent. */
    public static class BarChart extends JPanel {

        private final String chartTitle;
        private Map<String, Integer> data = new LinkedHashMap<>();

        public BarChart(String chartTitle) {
            this.chartTitle = chartTitle;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(380, 230));
        }

        public void setData(Map<String, Integer> newData) {
            this.data = newData;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setFont(FONT_HEADING);
            g2.setColor(TEXT_DARK);
            g2.drawString(chartTitle, 12, 22);

            if (data.isEmpty()) {
                return;
            }

            int largest = 1;
            for (int value : data.values()) {
                if (value > largest) {
                    largest = value;
                }
            }

            int left = 15;
            int chartTop = 55;
            int baseLine = getHeight() - 30;
            int maxBarHeight = baseLine - chartTop;
            int slotWidth = (getWidth() - 2 * left) / data.size();
            int barWidth = Math.min(50, slotWidth - 14);

            int index = 0;
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int value = entry.getValue();
                int barHeight = value * maxBarHeight / largest;
                int x = left + index * slotWidth + (slotWidth - barWidth) / 2;
                int y = baseLine - barHeight;

                g2.setColor(BAR_COLORS[index % BAR_COLORS.length]);
                g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);

                g2.setColor(TEXT_DARK);
                g2.setFont(FONT_BOLD);
                FontMetrics metrics = g2.getFontMetrics();
                String valueText = String.valueOf(value);
                g2.drawString(valueText, x + (barWidth - metrics.stringWidth(valueText)) / 2, y - 5);

                g2.setColor(TEXT_MUTED);
                g2.setFont(FONT_SMALL);
                metrics = g2.getFontMetrics();
                String labelText = TextUtil.capitalizeWords(entry.getKey());
                g2.drawString(labelText, x + (barWidth - metrics.stringWidth(labelText)) / 2, baseLine + 17);

                index++;
            }
            g2.setColor(BORDER);
            g2.drawLine(left, baseLine, getWidth() - left, baseLine);
        }
    }
}
