package org.incha.ui.stats;


import org.graphstream.graph.Graph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.incha.core.jswingripples.NodeSearchBuilder;
import org.incha.core.jswingripples.eig.JSwingRipplesEIG;
import org.incha.ui.JSwingRipplesApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NodeSearchView implements ActionListener{

	JSwingRipplesEIG e;

    public NodeSearchView(){super();}

    public void showGraph()
    {
    	System.out.println("SHOWING GRAPH");
        NodeSearchBuilder NS = NodeSearchBuilder.getInstance();
        NS.setSearch("Node");
        //NS.setSearch("Node");
        Graph graph = NS.getGraph();
        Viewer v = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        v.enableAutoLayout();
        ViewPanel view =  v.addDefaultView(false);
        JInternalFrame frame = new JInternalFrame("Search Graph");
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
        System.out.println("GRAPH SHOWN");

    }
    

    @Override
    public void actionPerformed(ActionEvent e) {
        showGraph();
    }
}