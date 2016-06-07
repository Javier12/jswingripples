package org.incha.ui.stats;

import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.incha.core.jswingripples.GraphBuilder;
import org.incha.ui.JSwingRipplesApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 6/2/16.
 * Part of org.incha.ui.stats.
 */
public class ImpactGraphVisualizationAction implements ActionListener {

    public ImpactGraphVisualizationAction() { super(); }

    private void showGraph()
    {
        /*Graph graph = GraphBuilder.getInstance().getImpactSetGraph();

        Viewer v = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);

        ViewerPipe pipe = v.newViewerPipe();
        pipe.addViewerListener(new ImpactGraphViewerListener(pipe));
        pipe.addSink(graph);

        v.enableAutoLayout();*/

        Viewer v = GraphBuilder.getInstance().getImpactViewer();
        ViewPanel view =  v.addDefaultView(false);
        JInternalFrame frame = new JInternalFrame("Graph: Impact Set");
        frame.getContentPane().setLayout(new BorderLayout());


        JDesktopPane viewArea = JSwingRipplesApplication.getInstance().getViewArea();
        frame.setBounds(0, 0, viewArea.getWidth(), viewArea.getHeight());
        frame.setClosable(true);
        frame.setMaximizable(true);
        frame.setVisible(true);
        frame.setResizable(true);
        frame.add(view, BorderLayout.CENTER);
        viewArea.add(frame);
        frame.moveToFront();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        showGraph();
    }
}
