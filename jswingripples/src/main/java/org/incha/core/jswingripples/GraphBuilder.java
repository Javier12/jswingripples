package org.incha.core.jswingripples;

import org.apache.commons.logging.LogFactory;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerPipe;
import org.incha.core.jswingripples.eig.*;
import org.incha.ui.JSwingRipplesApplication;
import org.incha.ui.TaskProgressMonitor;
import org.incha.ui.jripples.EIGStatusMarks;
import org.incha.ui.stats.DependencyGraphViewerListener;
import org.incha.ui.stats.ImpactGraphViewerListener;

/**
 * Created by Manuel Olguín (molguin@dcc.uchile.cl) on 4/26/2016.
 * Part of org.incha.core.jswingripples.
 */

public class GraphBuilder implements JSwingRipplesEIGListener{

    private static GraphBuilder instance = null; // singleton

    private JSwingRipplesEIG eig;
    private Graph graph;
    private Graph impactSetGraph;
    private Viewer impactViewer;
    private Viewer dependencyViewer;
    private DependencyGraphViewerListener depListener;
    private ImpactGraphViewerListener impListener;

    /**
     * Private constructor for singleton instance.
     */
    private GraphBuilder()
    {
        eig = null;
        resetGraphs();
    }

    /**
     * Resets the internal graphs.
     */
    public void resetGraphs()
    {

        //stop viewers
        if(depListener != null)
            depListener.stopPumpThread();
        if(impListener != null)
            impListener.stopPumpThread();

        graph = new DefaultGraph("Dependencies");
        impactSetGraph = new DefaultGraph("Impact Set");
        try {
            graph.addAttribute("ui.stylesheet", "url(file://" + GraphBuilder.class.getClassLoader()
                    .getResource("graph.css").toString().substring(5) + ")");
            impactSetGraph.addAttribute("ui.stylesheet", "url(file://" + GraphBuilder.class.getClassLoader()
                    .getResource("graph.css").toString().substring(5) + ")");
        }
        catch (NullPointerException e) {
            LogFactory.getLog(this.getClass()).error("Missing graph stylesheet! - graph.css");
            System.exit(1);
        }

        impactViewer = new Viewer(impactSetGraph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        ViewerPipe pipe = impactViewer.newViewerPipe();
        impListener = new ImpactGraphViewerListener(pipe);
        pipe.addViewerListener(impListener);
        pipe.addSink(impactSetGraph);
        impactViewer.enableAutoLayout();

        dependencyViewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        ViewerPipe piped = dependencyViewer.newViewerPipe();
        depListener = new DependencyGraphViewerListener(piped);
        piped.addViewerListener(depListener);
        piped.addSink(graph);
        dependencyViewer.enableAutoLayout();

    }

    public Viewer getImpactViewer()
    {
        return impactViewer;
    }

    public Viewer getDependencyViewer()
    {
        return dependencyViewer;
    }

    /**
     * Returns the current instance of the singleton GraphBuilder object.
     * @return GraphBuilder object instance.
     */
    public static GraphBuilder getInstance() {
        if ( instance == null )
        {
            instance = new GraphBuilder();
        }

        return instance;
    }

    /**
     * Associates this GraphBuilder instance with the provided EIG.
     * @param eig The EIG from which we want to build a visual graph.
     */
    public void addEIG(JSwingRipplesEIG eig) { this.eig = eig; }

    /**
     * Constructs the Graph objects from the previously provided EIG instance.
     */
    public void prepareGraphs()
    {

        if ( eig == null )
            return;

        //resetGraphs();

        JSwingRipplesEIGNode[] eigNodes = eig.getAllNodes();
        JSwingRipplesEIGEdge[] eigEdges = eig.getAllEdges();

        final TaskProgressMonitor monitor = JSwingRipplesApplication.getInstance().getProgressMonitor();
        monitor.beginTask("Building graph: Adding nodes.", eigNodes.length);
        for ( int i = 0; i < eigNodes.length; i++ ) {
            JSwingRipplesEIGNode node = eigNodes[i];
            monitor.worked(i);
            monitor.setTaskName("Adding node " + node.getShortName());
            if (graph.getNode(node.getFullName()) == null) {
                Node n = graph.addNode(node.getFullName());
                n.addAttribute("label", node.getShortName());
            }
        }
        monitor.done();

        monitor.beginTask("Building graph: Adding edges.", eigEdges.length);
        for ( int i = 0; i < eigEdges.length; i++ )
        {
            JSwingRipplesEIGEdge edge = eigEdges[i];
            monitor.worked(i);
            String eid = edge.getFromNode().getFullName() + " -> " + edge.getToNode().getFullName();
            monitor.setTaskName("Adding edge " + eid);
            if ( graph.getEdge(eid) == null)
                graph.addEdge(eid, edge.getFromNode().getFullName(), edge.getToNode().getFullName());
        }
        monitor.done();
    }

    public Graph getDependencyGraph() { return graph; }
    public Graph getImpactSetGraph() { return impactSetGraph; }

    @Override
    public void jRipplesEIGChanged(JSwingRipplesEIGEvent event) {

        if (!event.hasNodeEvents()) return;

        final JSwingRipplesEIGNodeEvent[] nodeEvents=event.getNodeTypedEvents(
                new int[] {JSwingRipplesEIGNodeEvent.NODE_MARK_CHANGED, JSwingRipplesEIGNodeEvent.NODE_REMOVED,
                        JSwingRipplesEIGNodeEvent.NODE_ADDED});
        if (nodeEvents.length==0) return;

        for (int i=0;i<nodeEvents.length;i++) {
            final JSwingRipplesEIGNode changedNode = nodeEvents[i].getSource();
            Node graphn = graph.getNode(changedNode.getFullName());
            Node impactn = impactSetGraph.getNode(changedNode.getFullName());
            switch (nodeEvents[i].getEventType()) {
                case JSwingRipplesEIGNodeEvent.NODE_REMOVED: {
                    //TODO
                    break;
                }
                case JSwingRipplesEIGNodeEvent.NODE_ADDED: {
                    //TODO
                    break;
                }
                case JSwingRipplesEIGNodeEvent.NODE_MARK_CHANGED: {
                    final String mark=changedNode.getMark();
                    if (mark!=null)
                    {
                        switch (mark)
                        {
                            case EIGStatusMarks.CHANGED:
                                graphn.setAttribute("ui.class", "changed");

                                if ( impactn == null ) {
                                    impactn = impactSetGraph.addNode(changedNode.getFullName());
                                    impactn.setAttribute("ui.class", "changed");
                                    impactn.setAttribute("label", changedNode.getShortName());
                                }
                                else
                                    impactn.setAttribute("ui.class", "changed");
                                break;
                            case EIGStatusMarks.IMPACTED:
                                graphn.setAttribute("ui.class", "impacted");

                                if ( impactn == null ) {
                                    impactn = impactSetGraph.addNode(changedNode.getFullName());
                                    impactn.setAttribute("ui.class", "impacted");
                                    impactn.setAttribute("label", changedNode.getShortName());
                                }
                                else
                                    impactn.setAttribute("ui.class", "impacted");

                                break;
                            case EIGStatusMarks.NEXT_VISIT:
                                graphn.setAttribute("ui.class", "next");

                                if ( impactn == null ) {
                                    impactn = impactSetGraph.addNode(changedNode.getFullName());
                                    impactn.setAttribute("ui.class", "next");
                                    impactn.setAttribute("label", changedNode.getShortName());
                                }
                                else
                                    impactn.setAttribute("ui.class", "next");

                                break;
                            case EIGStatusMarks.VISITED_CONTINUE:
                                graphn.setAttribute("ui.class", "propagating");

                                if ( impactn == null ) {
                                    impactn = impactSetGraph.addNode(changedNode.getFullName());
                                    impactn.setAttribute("ui.class", "propagating");
                                    impactn.setAttribute("label", changedNode.getShortName());
                                }
                                else
                                    impactn.setAttribute("ui.class", "propagating");

                                break;
                            default:
                                graphn.setAttribute("ui.class", "blank");

                                if ( impactn != null ) {
                                    impactSetGraph.removeNode(impactn);
                                    impactn = null;
                                }
                                break;
                        }

                    }
                    break;
                }
            }

            updateImpactSetEdges();
        }
    }

    private void updateImpactSetEdges()
    {
        JSwingRipplesEIGEdge[] edges = eig.getAllEdges();
        for(JSwingRipplesEIGEdge edge: edges)
        {
            String toID = edge.getToNode().getFullName();
            String fromID = edge.getFromNode().getFullName();
            String eID = fromID + " -> " + toID;
            if (impactSetGraph.getEdge(eID) != null) // edge exists
            {
                if(impactSetGraph.getNode(toID) == null || impactSetGraph.getNode(fromID) == null)
                    // edge exists but nodes don't -> remove edge
                    impactSetGraph.removeEdge(eID);
            }
            else // edge doesn't exist
            {
                if(impactSetGraph.getNode(toID) != null && impactSetGraph.getNode(fromID) != null)
                    // nodes exist -> add edge
                    impactSetGraph.addEdge(eID, fromID, toID);
            }

        }
    }
}
