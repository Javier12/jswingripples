package org.incha.ui.classview;

import java.awt.Color;

import javax.swing.JLabel;

import org.incha.core.jswingripples.eig.JSwingRipplesEIGNode;
import org.incha.core.search.Highlight;
import org.incha.ui.jripples.EIGStatusMarks;

public class ClassTreeRenderer extends AbstractMemberRenderer {
    private static final long serialVersionUID = 5646472411943179112L;

    /**
     * Default constructor.
     */
    public ClassTreeRenderer() { super(); }

    /**
     * @param label
     * @param node
     * @param column
     */
    @Override
    protected void renderOtherColumn(final JLabel label, final JSwingRipplesEIGNode node,
            final int column) {
        final String mark = node.getMark();
        switch (column) {
            case 0:
                label.setBackground(Highlight.getColor(node.getShortName()));
            break;
            case 1:
                if (mark != null && !mark.isEmpty()) {
                    final Color color = EIGStatusMarks.getColorForMark(mark);
                    label.setBackground(color);
                    label.setText(mark);
                } else {
                    label.setText("");
                }
                label.setIcon(null);
            break;
            case 2:
                label.setIcon(null);
                label.setText(node.getProbability());
            break;
            case 3:
                label.setIcon(null);
                label.setText(getFullName(node));
            break;

            default:
            break;
        }
    }
}
