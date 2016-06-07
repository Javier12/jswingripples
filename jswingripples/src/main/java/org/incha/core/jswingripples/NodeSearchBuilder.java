package org.incha.core.jswingripples;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.incha.core.jswingripples.eig.JSwingRipplesEIG;
import org.incha.core.jswingripples.eig.JSwingRipplesEIGEdge;
import org.incha.core.jswingripples.eig.JSwingRipplesEIGNode;

public class NodeSearchBuilder {

    private JSwingRipplesEIG eig;
    private static NodeSearchBuilder instance = null; // singleton
    private Graph graph;
    
    private JSwingRipplesEIGNode[] eigNodes;
    
   private NodeSearchBuilder()
    {
       
	   graph = new DefaultGraph("Search Graph"); 

    }
   
   
   public void setSearch(String s){
	  // search = s;
	   System.out.println("PREPARING GRAPH");
	   prepareGraph(s);
	   System.out.println("GRAPH PREPARED");
   }
   
   public void clearGraph(){
	   graph.clear();
   }
   
   public static NodeSearchBuilder getInstance() {
       if ( instance == null )
       {
           instance = new NodeSearchBuilder();
       }

       return instance;
   }


    public void addEIG(JSwingRipplesEIG eig) throws CloneNotSupportedException
    {
    	
        this.eig = (JSwingRipplesEIG)eig.clone();
        eigNodes = eig.getAllNodes();
    }
    
    private JSwingRipplesEIGNode[] addRelatedNodes(JSwingRipplesEIGNode node){
        addNode(node);
    	JSwingRipplesEIGNode[] neighbors = eig.getAllAnyNodeNeighbors(node);
    	for (JSwingRipplesEIGNode n : neighbors){
    		//if (graph.getNode(n.getShortName()) == null){
    			addNode(n);
    		//}
    	}
    	return neighbors;
    }
    
    private void addNode(JSwingRipplesEIGNode node){
    	System.out.println("Current Nodes : " + String.valueOf(graph.getNodeCount()));
    	if (graph.getNode(node.getShortName()) == null){
    	Node n = graph.addNode(node.getShortName());
    	n.addAttribute("label", node.getShortName());
    	}
    	System.out.println("After add : " +  String.valueOf(graph.getNodeCount()));
    }
    
    private void addEdgeIfExisting(JSwingRipplesEIGNode node1, JSwingRipplesEIGNode node2){
    	JSwingRipplesEIGEdge edge = eig.getEdge(node1, node2);
    	if (edge != null){
    		String eid = node1.getShortName() + " -> " + node2.getShortName();
    		if (graph.getEdge(eid) == null){
    		graph.addEdge(eid, graph.getNode(node1.getShortName()), graph.getNode(node2.getShortName()));
    		}
    	}
    }
    
    private void computeAndAddEdges(JSwingRipplesEIGNode node, JSwingRipplesEIGNode[] neighbors){
    	for (JSwingRipplesEIGNode n : neighbors){
    		addEdgeIfExisting(node,n);
    	}
    	for (JSwingRipplesEIGNode n : neighbors){
    		for (JSwingRipplesEIGNode m : neighbors){
    			addEdgeIfExisting(n,m);
    		}
    	}
    }

    private void prepareGraph(String name)
    {
    	System.out.println("Looking at EIG nodes");
    	if (eig == null){
    		System.out.println("This is empty");
    	}
        //JSwingRipplesEIGNode[] eigNodes = eig.getAllNodes();
        System.out.println("Looking at EIG nodes");
        for ( JSwingRipplesEIGNode node : eigNodes ){
        	System.out.println("Name of node is " + node.getShortName());
            	if ( name.equals(node.getShortName())  ){
            		System.out.println("GOT A MATCH");
                computeAndAddEdges(node,addRelatedNodes(node));
            	}}
        System.out.println("No more to look");
    }
    
    public Graph getGraph(){
    	return graph;
    }


}

