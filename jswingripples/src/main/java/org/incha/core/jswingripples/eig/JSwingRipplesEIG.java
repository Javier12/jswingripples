/*
 * Created on Oct 20, 2005
 *
 */
package org.incha.core.jswingripples.eig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.incha.core.JavaProject;
import org.incha.core.jswingripples.eig.history.History;

/**
 * <p>A software system can be viewed as a set of components and their interoperations and
 * can be formally modeled as graph. In a nutshell, in this
 * graph components are represented as nodes and interactions are represented as edges
 * among the nodes. As the software evolves and changes, its components change too.
 * Moreover, a change, introduced in a one component, may propagate through the
 * interoperation dependencies to other components as well. To reflect the change
 * process in the graph, we use marks.</p>
 * <p>The described graph, which contains components, their interoperations, marks and
 * has a set of propagation rules defined, is called <b>Evolving Interoperation Graph (EIG)</b>.</p>
 * <p>JRipples EIG is created by
 * <ol>
 * <li>setting up a project under analysis,
 * <li>setting up a main class to an analysis from,
 * <li>adding IMember Java Elements of the project under analysis to wrapped with JRipples EIG nodes
 * <li>adding edges between created nodes
 * </ol>
 * </p>
 * <p>There is only one instance of JRipplesEIG is created during JRipples plug-in
 * lifecycle and thus only one project can be analyzed in the same time. Consequently, most of the
 * JRipplesEIG methods are static and refer to the same dependency graph.</p>
 * <p>Even though storing in the same way, JRipples EIG classifies every node as a top node or
 * a member node based on whether underlying IMember object is a top in a class nesting hierarchy.</p>
 * <p>
 * Here is a short example on how JRipples EIG is typically used:
 * <pre>
 * <code>
 *
 * JRipplesEIG.initEIG();
 *
 * JRipplesEIG.setProject((IProject) someProject);
 * JRipplesEIG.setMainClass((IType) someType);
 *
 *JRipplesEIG.doLock(this);
 * JRipplesEIGNode node1=JRippelsEIG.addNode((IMember) member1);
 * JRipplesEIGNode node2=JRippelsEIG.addNode((IMember) member2);
 * JRipplesEIGNode node3=JRippelsEIG.addNode((IMember) member3);
 *
 * JRippelsEIGEdge edge1=JRippelsEIG.addEdge(node1, node2);
 * JRippelsEIGEdge edge1=JRippelsEIG.addEdge(node1, JRipplesEIG.getNode((IMember)member2));
 * JRippelsEIGEdge edge1=JRippelsEIG.addEdge(JRipplesEIG.getNode((IMember)member3), JRipplesEIG.getNode((IMember)member2));
 * JRipplesEIG.doUnLock(this); //All the EIG listeners will be notified of the changes
 *
 * ...some logic...
 *
 * JRipplesEIG.doLock(this);
 *
 * node1.setMark("Changed");
 * JRipplesEIG.removeNode(node2);
 * node3.setMark("Visited");
 *
 * JRipplesEIG.doUnLock(this); //All the EIG listeners will be notified of the changes
 *
 * node3.setProbability("12"); //All the EIG listeners will be notified of the change
 *
 * </code>
 * </pre>
 * </p>
 *
 * @author Maksym Petrenko
 * @see #initEIG()
 * @see #setProject(IProject)
 * @see #setMainClass(IType)
 * @see #addNode(IMember)
 * @see #addEdge(JSwingRipplesEIGNode, JSwingRipplesEIGNode)
 * @see JSwingRipplesEIGNode
 * @see JSwingRipplesEIGEdge
 *
 */
public final class JSwingRipplesEIG implements Cloneable{
	private Map<IMember, JSwingRipplesEIGNode> nodes;
	private Set<JSwingRipplesEIGNode> topNodes;
	private Map<JSwingRipplesEIGNode, HashSet<JSwingRipplesEIGNode>> members; //Node -> Set of members

	private Collection<JSwingRipplesEIGEdge> edges;
	private Map<JSwingRipplesEIGNode, HashMap<JSwingRipplesEIGNode, JSwingRipplesEIGEdge>> edgesAdjacency; //Node -> Set of related nodes
	private Map<JSwingRipplesEIGNode, JSwingRipplesEIGEdge> nodesAdjacency; //Node and its direct related edges, find missing edges

	private String mainClass;

	private Set<JSwingRipplesEIGListener> eigListeners = Collections.synchronizedSet(new  HashSet<JSwingRipplesEIGListener>());

    private final JavaProject projectName;
	boolean redoInProgress = false;

	protected static final int UNDOABLE = 1;
	protected static final int REDOABLE = 2;
	protected static final int NONEABLE = 4;

    private final History history = new History();

	// ------------------------------- EIG General ------------------------------

	public JSwingRipplesEIG(final JavaProject projectName)  {
	    initEIG();
	    this.projectName = projectName;
	}
	public Object clone()throws CloneNotSupportedException{  
		return super.clone();  
		}  
	/**
	 * Reinitializes JRipples EIG for a new analysis - that is, it deletes all nodes and edges from the database and clears undo / redo history.
	 * <br>Please note, that project and main class are not reset.
	 */
	public void initEIG() {
		if  (edges!=null)
			synchronized (edges) {
				for (final Iterator<JSwingRipplesEIGEdge> iter=edges.iterator();iter.hasNext();) {
					fireJRipplesEIGChanged(iter.next(), JSwingRipplesEIGEdgeEvent.EDGE_REMOVED);
				}
			}

		if  (nodes!=null)
			synchronized (nodes) {
				for (final Iterator<JSwingRipplesEIGNode> iter=nodes.values().iterator();iter.hasNext();) {
					fireJRipplesEIGChanged(iter.next(),
					        JSwingRipplesEIGNodeEvent.NODE_REMOVED, null, null);
				}

			}

		/*
		if (nodes!=null)
			if (nodes.values()!=null)
				batchRemoveNodes( new LinkedHashSet (nodes.values()));
		 */

		if (nodes!=null) {
			final JSwingRipplesEIGNode []nodesArr=getAllNodes();
			nodes.clear();
			for (int i=0;i<nodesArr.length;i++) {
				nodesArr[i]=null;
			}

		}
		if (edges!=null) {
			final JSwingRipplesEIGEdge []edgesArr=getAllEdges();
			edges.clear();
			for (int i=0;i<edgesArr.length;i++) {
				edgesArr[i]=null;
			}
		}


		if (nodes == null)
			nodes = Collections.synchronizedMap(new  HashMap<IMember, JSwingRipplesEIGNode>());

		if (topNodes!=null) topNodes.clear();
		else topNodes = Collections.synchronizedSet(new HashSet<JSwingRipplesEIGNode>());

		if (members!=null) members.clear();
		else members=Collections.synchronizedMap(new  HashMap<JSwingRipplesEIGNode, HashSet<JSwingRipplesEIGNode>>());


		if (edges == null)
			edges = Collections.synchronizedSet(new HashSet<JSwingRipplesEIGEdge>());

		if (edgesAdjacency!=null) edgesAdjacency.clear();
		else edgesAdjacency = Collections.synchronizedMap(new  HashMap<JSwingRipplesEIGNode, HashMap<JSwingRipplesEIGNode, JSwingRipplesEIGEdge>>());

		//keep step with edgesAdjacency
		if (nodesAdjacency!=null) nodesAdjacency.clear();
		else nodesAdjacency = Collections.synchronizedMap(new  HashMap<JSwingRipplesEIGNode, JSwingRipplesEIGEdge>());
	}

	/**
	 * Checks whether the JRipples EIG is inialized for analysis - that is, whether a main project and main class are set, and whether EIG has at least one node registered with it.
	 * @return
	 * <code>true</code> if the JRipples EIG is inialized for analysis, <br><code>null</code> otherwise
	 * @see #getProject()
	 * @see #getMainClass()
	 * @see #getAllNodes()
	 */
	public boolean isInitialized() {
		if (getMainClass() == null) {
			return false;
		}
		if (nodes == null) {
			return false;
		}
		;
		if (nodes.size() == 0) {
			return false;
		}

		return true;
	}
	/**
	 * Sets a main class (a class to start the analysis from) of a project under analysis, usually chosen through "JRipples > Start" menu.
	 * @param type
	 * a main class of a project this JRipples EIG analysis reffers to
	 * @see #getProject()
	 * @see #setProject(IProject)
	 * @see #getMainClass()
	 *
	 */
	public void setMainClass(final String type) {
		mainClass = type;
	}

	/**
	 * Returns the main class (a class to start the analysis from) of a project under analysis, usually chosen through "JRipples > Start" menu.
	 * @return
	 * a main class of a project this JRipples EIG analysis reffers to
	 * @see #getProject()
	 * @see #setProject(IProject)
	 * @see #setMainClass(IType)
	 */
	public String getMainClass() {
		return mainClass;
	}

	// ------------------------------------- EIG Node operations -------------------------


	/**
	 * Creates and adds to the JRipples EIG a node that wraps a supplied IMember object
	 * @return
	 *  an existing node if one found in JRipples EIG,<br>
	 *  a created node if there was no such node declared before,<br>
	 *  <code>null</code> if the supplied IMember object is <code>null</code>
	 */
	public JSwingRipplesEIGNode addNode(final IMember nodeIMember) {

		if (nodeIMember==null) return null;
		if (nodes.containsKey(nodeIMember)) return nodes.get(nodeIMember);
		final JSwingRipplesEIGNode node=new JSwingRipplesEIGNode(this, nodeIMember);

		if (!members.keySet().contains(node)) {
			final HashSet<JSwingRipplesEIGNode> nodeMembers=new HashSet<JSwingRipplesEIGNode>();
			members.put(node,nodeMembers);
		}

		try {
			if (node.isTop()) {
				topNodes.add(node);
			} else {

				JSwingRipplesEIGNode parentNode=findParentNodeForMemberNode(node);
				if (parentNode==null) parentNode=addNode(JSwingRipplesIMemberServices.getMemberParent(nodeIMember));

				if (parentNode!=null) {
					members.get(parentNode).add(node);
				}
			}
		} catch (final Exception e) {
			//do nothing
		}

		nodes.put(nodeIMember, node);

		fireJRipplesEIGChanged(node, JSwingRipplesEIGNodeEvent.NODE_ADDED, null, null);
		return node;
	}


	/**
	 * Removes a node from the JRipples EIG. It also removes all edges,
	 * associated with this node. If the node is top, the same set of
	 * actions will be applied to all member node of this node as well -
	 * the member nodes together with their associated edges will
	 * be removed from the JRipples EIG.
	 * @param node
	 * 	node to remove
	 * @see #getNodeMembers(JSwingRipplesEIGNode)
	 * @see JSwingRipplesEIGNode#isTop()
	 * @see #removeEdge(JSwingRipplesEIGEdge)
	 */

	public void removeNode(final JSwingRipplesEIGNode node) {
		//Remove members if any
		LinkedHashSet<JSwingRipplesEIGNode> nodesToBeRemoved=new LinkedHashSet<JSwingRipplesEIGNode>();
		nodesToBeRemoved.add(node);
		if (node.isTop()) topNodes.remove(node);
		walkMembers(node,nodesToBeRemoved);

		batchRemoveNodes(nodesToBeRemoved); //Remove encountered nodes and edges
		nodesToBeRemoved.clear();
		nodesToBeRemoved=null;
	}

	/**
	 * Walks through the members of the node and adds them to the list of the nodes to be removed in the batch
	 * @param node
	 * @param nodesToBeRemoved
	 */
	private void walkMembers(final JSwingRipplesEIGNode node, final LinkedHashSet<JSwingRipplesEIGNode> nodesToBeRemoved) {
		HashSet<JSwingRipplesEIGNode> membersSet=members.get(node);
		if (membersSet.size()>0) {
			for (final Iterator <JSwingRipplesEIGNode>iter = membersSet.iterator();iter.hasNext();) {
				final JSwingRipplesEIGNode nodeTmp=iter.next();
				nodesToBeRemoved.add(nodeTmp);
				walkMembers(nodeTmp, nodesToBeRemoved);
			}
			membersSet.clear();
			members.remove(node);
			membersSet=null;
		}
	}

	/**
	 * Removes nodes and their edges in a batch
	 * @param nodesToBeRemoved
	 */
	private void batchRemoveNodes(final LinkedHashSet<JSwingRipplesEIGNode> nodesToBeRemoved) {

		if (nodesToBeRemoved==null) return;
		if (nodesToBeRemoved.size()==0) return;

		Collection<JSwingRipplesEIGEdge> tmpEdges = new HashSet<JSwingRipplesEIGEdge>();
		synchronized (edges) {
			for (final Iterator<JSwingRipplesEIGEdge> iter = edges.iterator(); iter.hasNext();) {
				final JSwingRipplesEIGEdge edge = iter.next();
				if ((edge.getFromNode() != null) && ((edge.getToNode() != null)))
					if ((nodesToBeRemoved.contains(edge.getFromNode()))
							|| (nodesToBeRemoved.contains(edge.getToNode())))
						tmpEdges.add(edge);
			}
		}

		if (tmpEdges.size() > 0) {
			for (final Iterator<JSwingRipplesEIGEdge> iter=tmpEdges.iterator();iter.hasNext();) {
				removeEdge(iter.next());
			}
			tmpEdges.clear();
		}
		tmpEdges=null;

		for (final Iterator<JSwingRipplesEIGNode> iter=nodesToBeRemoved.iterator();iter.hasNext();) {
			JSwingRipplesEIGNode nodeTmp=iter.next();

			nodes.remove(nodeTmp.getNodeIMember());

			fireJRipplesEIGChanged(nodeTmp, JSwingRipplesEIGNodeEvent.NODE_REMOVED, null, null);
			nodeTmp = null;
		}

	}

	/**
	 * Checks whether JRipples EIG database contains a node with provided
	 * IMember object.
	 * @param nodeMember
	 * underlying IMember object of a node to find
	 * @return
	 * <code>true</code> if JRipples EIG database contains a node with provided
	 * IMember object, <br> <code>false</code> otherwise
	 * @see JSwingRipplesEIGNode#getNodeIMember()
	 * @see #getNode(IMember)
	 *
	 */
	public boolean existsNode(final IMember nodeMember) {
		if (nodeMember==null) return false;
		return nodes.keySet().contains(nodeMember);
	}

	/**
	 * Checks whether JRipples EIG database contains a node with provided
	 * IMember object and returns it if any.
	 * @param nodeMember
	 * underlying IMember object of a node to return
	 * @return
	 * a node with supplied underlying IMember object if JRipples EIG database contains such a node
	 * , <br> <code>null</code> otherwise
	 * @see #existsNode(IMember)
	 * @see JSwingRipplesEIGNode#getNodeIMember()
	 */
	public JSwingRipplesEIGNode getNode(final IMember nodeMember) {
		if (nodeMember==null) return null;
		if (!existsNode(nodeMember)) return null;
		return nodes.get(nodeMember);
	}


	/**
	 * Returns all the nodes, registered with this EIG (that is, created with {@link #addNode(IMember)} method).
	 * @return
	 * all the nodes, registered with this EIG (that is, created with {@link #addNode(IMember)} method),<br> or empty array if no node was found.
	 */
	public JSwingRipplesEIGNode[] getAllNodes() {
		if (nodes == null) return new JSwingRipplesEIGNode[0];
		final Collection<JSwingRipplesEIGNode> values = nodes.values();
        return values.toArray(new JSwingRipplesEIGNode[values.size()]);
	}

	/**
	 * Returns all the top nodes (nodes, whose underlying IMember object is top class hierarchy), registered with this EIG.
	 * @return
	 * all the top nodes, registered with this EIG, <br> or empty array if no top node was found.
	 */
	public JSwingRipplesEIGNode[] getTopNodes() {
		if (topNodes == null) return new JSwingRipplesEIGNode[0];
		return topNodes.toArray(new JSwingRipplesEIGNode[topNodes.size()]);
	}


	//===============Member's operations ============================================


	/**
	 * Returns member nodes, whose underlying IMember Java elements are defined within IMember Java element of a supplied top node.
	 * @param node
	 * node, whose member nodes should be returned
	 * @return
	 * nodes, whose underlying IMember Java elements are defined within IMember Java element of a supplied top node, if any, <br>
	 * empty array otherwise
	 * @see JSwingRipplesEIGNode#isTop()
	 * @see #getTopNodes()
	 * @see #findTopNodeForMemberNode(JSwingRipplesEIGNode)
	 */
	public JSwingRipplesEIGNode[] getNodeMembers(final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		if (!members.keySet().contains(node)) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> membersSet=members.get(node);
		return membersSet.toArray(new JSwingRipplesEIGNode[membersSet.size()]);
	}



	/**
	 * Finds a top node, whose underlying IMember object declares the supplied member parameter.
	 * @param member
	 * member object, for which a top node should be found
	 * @return
	 * a node, if any, whose underlying IMember object declares the supplied member parameter, <br>or <code>null</code> otherwise
	 * @see #getTopNodes()
	 * @see #getNodeMembers(JSwingRipplesEIGNode)
	 * @see #findTopNodeForMemberNode(JSwingRipplesEIGNode)
	 * @see JSwingRipplesEIGNode#isTop()
	 */
	public JSwingRipplesEIGNode findTopNodeForIMember(final IMember member) {
		final IType declaringType=JSwingRipplesIMemberServices.getTopDeclaringType(member);
		if (declaringType==null) return null;
		final JSwingRipplesEIGNode topNode=getNode(declaringType);
		return topNode;
	}

	/**
	 * Finds a parent node, whose underlying IMember object declares the supplied member parameter.
	 * @param member
	 * member object, for which a top node should be found
	 * @return
	 * a parent node, if any, whose underlying IMember object declares the supplied member parameter, <br>or <code>null</code> otherwise
	 * @see #getTopNodes()
	 * @see #getNodeMembers(JSwingRipplesEIGNode)
	 * @see #findTopNodeForMemberNode(JSwingRipplesEIGNode)
	 * @see JSwingRipplesEIGNode#isTop()
	 */
	private JSwingRipplesEIGNode findParentNodeForIMember(final IMember member) {
		final IMember parent=JSwingRipplesIMemberServices.getMemberParent(member);
		if (parent==null) return null;
		final JSwingRipplesEIGNode topNode=getNode(parent);
		return topNode;
	}

	/**
	 * Finds a top node for a supplied member node.
	 * @param node
	 * @return
	 * the supplied node if it is top,<br>
	 * a top node, whose underlying IMember Java element declares underlying IMember Java element of the supplied node, <br>
	 *<code>null</code> otherwise
	 * @see JSwingRipplesEIGNode#isTop()
	 * @see #getNodeMembers(JSwingRipplesEIGNode)
	 * @see #getTopNodes()
	 */
	public JSwingRipplesEIGNode findTopNodeForMemberNode(final JSwingRipplesEIGNode node) {
		//if (node.isTop()) return node;
		return findTopNodeForIMember(node.getNodeIMember());
	}


	/**
	 * Finds a parent node for a supplied member node.
	 * @param node
	 * @return
	 * the supplied node if it is top,<br>
	 * a parent node, whose underlying IMember Java element declares underlying IMember Java element of the supplied node, <br>
	 *<code>null</code> otherwise
	 * @see JSwingRipplesEIGNode#isTop()
	 * @see #getNodeMembers(JSwingRipplesEIGNode)
	 * @see #getTopNodes()
	 */
	public JSwingRipplesEIGNode findParentNodeForMemberNode(final JSwingRipplesEIGNode node) {
		return findParentNodeForIMember(node.getNodeIMember());
	}

	//	-----------------------------------Node Neigbors -----------------------------------------

	/**
	 * direction constant indicating that only calling nodes (nodes, that call the supplied centralNode) should be returned
	 * <br>to be used with {@link #edgesToNeigbors(Set, int, int)}
	 */

	public static final int DIRECTION_CONSIDERED_CALLING_NODES_ONLY=-1;
	/**
	 * direction constant indicating that both called and calling nodes should be returned
	 * <br>to be used with {@link #edgesToNeigbors(Set, int, int)}
	 */
	public static final int DIRECTION_CONSIDERED_BOTH_CALLING_AND_CALLED=0;
	/**
	 * direction constant indicating that only called nodes (nodes, that are called from the supplied centralNode) should be returned
	 * <br>to be used with {@link #edgesToNeigbors(Set, int, int)}
	 */
	public static final int DIRECTION_CONSIDERED_CALLED_NODES_ONLY=1;
	/**
	 * nesting constant indicating that only top nodes should be returned
	 * <br>to be used with {@link #edgesToNeigbors(Set, int, int)}
	 */
	public static final int NESTING_CONSIDERED_TOP_NODES_ONLY=-1;
	/**
	 * nesting constant indicating that both top and member nodes should be returned
	 * <br>to be used with {@link #edgesToNeigbors(Set, int, int)}
	 */
	public static final int NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES=0;
	/**
	 * nesting constant indicating that only member nodes should be returned
	 * <br>to be used with {@link #edgesToNeigbors(Set, int, int)}
	 */
	public static final int NESTING_CONSIDERED_MEMBER_NODES_ONLY=1;


	/**
	 *Based on supplied parameters, returns a set of JRippelsEIGNodes that contain
	 *<ul>
	 *<li>either top nodes, or member nodes, or both
	 *<li>that
	 *<li>either call, or are called, or both
	 *<li>by
	 *<li>any of the supplied nodes (directly, but not transitively through the nodes's members)
	 *</ul>
	 * <br>Direction constants:
	 * <ul>
	 * <li>{@link #DIRECTION_CONSIDERED_BOTH_CALLING_AND_CALLED}
	 * <li>{@link #DIRECTION_CONSIDERED_CALLING_NODES_ONLY}
	 * <li>{@link #DIRECTION_CONSIDERED_CALLED_NODES_ONLY}
	 * </ul>
	 * <br>Nesting constants:
	 * <ul>
	 * <li>{@link #NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES}
	 * <li>{@link #NESTING_CONSIDERED_TOP_NODES_ONLY}
	 * <li>{@link #NESTING_CONSIDERED_MEMBER_NODES_ONLY}
	 * </ul>
	 *
	 * @param nodes
	 * 	Set of {@link JSwingRipplesEIGNode} nodes, of which direct (not transitive through the node's members) neighbors should be returned
	 * @param directionConsidered
	 * 	whether to return neighbors that only call, are called by, or do both to the supplied nodes
	 * @param nestingConsidered
	 * 	whether to return neighbors that are top nodes, member nodes, or both
	 * @return
	 * 	Set of {@link JSwingRipplesEIGNode} nodes that satisfy specified requirements; set may be empty
	 * @see #getAllAnyNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getAllTopNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getAllMemberNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getIncomingAnyNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getIncomingTopNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getIncomingMemberNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getOutgoingAnyNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getOutgoingTopNodeNeighbors(JSwingRipplesEIGNode)
	 * @see #getOutgoingMemberNodeNeighbors(JSwingRipplesEIGNode)
	 *
	 *
	 */
	public HashSet<JSwingRipplesEIGNode> edgesToNeigbors(final Set<JSwingRipplesEIGNode> nodes, final int directionConsidered, final int nestingConsidered ) {
		//TODO Can be done as bit masks and enums

		final HashSet<JSwingRipplesEIGNode> result=new HashSet<JSwingRipplesEIGNode>();
		if (nodes==null) return result;
		if (nodes.size()==0) return result;

		synchronized (edges) {
			for (final Iterator<JSwingRipplesEIGEdge> iter = edges.iterator(); iter.hasNext();) {
				final JSwingRipplesEIGEdge edge = iter.next();
				if ((edge.getFromNode() != null) && ((edge.getToNode() != null))) {
					//fromNode->centralNode (centralNode==toNode)
					if ((nodes.contains(edge.getToNode()))) {
						if ((directionConsidered==DIRECTION_CONSIDERED_CALLING_NODES_ONLY) || (directionConsidered==DIRECTION_CONSIDERED_BOTH_CALLING_AND_CALLED)) {
							if (topNodes.contains(edge.getFromNode())) {
								if ((nestingConsidered==NESTING_CONSIDERED_TOP_NODES_ONLY) || (nestingConsidered==NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES))
									result.add(edge.getFromNode());
							} else	{
								if ((nestingConsidered==NESTING_CONSIDERED_MEMBER_NODES_ONLY) || (nestingConsidered==NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES))
									result.add(edge.getFromNode());
								if ((nestingConsidered==NESTING_CONSIDERED_TOP_NODES_ONLY) || (nestingConsidered==NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES))
									result.add(findTopNodeForMemberNode(edge.getFromNode()));
							}
						}
					}
					//centralNode->toNode (centralNode==fromNode)
					if ((nodes.contains(edge.getFromNode()))) {
						if ((directionConsidered==DIRECTION_CONSIDERED_CALLED_NODES_ONLY) || (directionConsidered==DIRECTION_CONSIDERED_BOTH_CALLING_AND_CALLED)) {
							//add missing edges
							if(edge.getToNode().getNodeIMember().getElementType()==IJavaElement.FIELD){
								final JSwingRipplesEIGEdge parentEdge = nodesAdjacency.get(edge.getToNode());
								if(parentEdge!=null)/*this expression can solve when calling an array but that array is not initialized, JRipples halts*/
									result.add(parentEdge.getToNode());
							}
							if (topNodes.contains(edge.getToNode())) {
								if ((nestingConsidered==NESTING_CONSIDERED_TOP_NODES_ONLY) || (nestingConsidered==NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES))
									result.add(edge.getToNode());
							} else	{
								if ((nestingConsidered==NESTING_CONSIDERED_MEMBER_NODES_ONLY) || (nestingConsidered==NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES))
									result.add(edge.getToNode());
								if ((nestingConsidered==NESTING_CONSIDERED_TOP_NODES_ONLY) || (nestingConsidered==NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES))
									result.add(findTopNodeForMemberNode(edge.getToNode()));
							}
						}
					}
				}
			}
		}

		return result;
	}


	//-----Node-to-TopNode neigbors ----

	/**
	 * Returns top nodes that both call and are called by both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */
	public JSwingRipplesEIGNode[] getAllTopNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_BOTH_CALLING_AND_CALLED,NESTING_CONSIDERED_TOP_NODES_ONLY);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}


	/**
	 * Returns top nodes that call both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 *  neighboring nodes
	 */

	public JSwingRipplesEIGNode[] getIncomingTopNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_CALLING_NODES_ONLY,NESTING_CONSIDERED_TOP_NODES_ONLY);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}

	/**
	 * Returns top nodes that are called by both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */


	public JSwingRipplesEIGNode[] getOutgoingTopNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_CALLED_NODES_ONLY,NESTING_CONSIDERED_TOP_NODES_ONLY);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}


	//-----Node-to-MemberNode neigbors ----

	/**
	 * Returns member nodes that both call and are called by both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */
	public JSwingRipplesEIGNode[] getAllMemberNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_BOTH_CALLING_AND_CALLED,NESTING_CONSIDERED_MEMBER_NODES_ONLY);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}

	/**
	 * Returns member nodes that call both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */
	public JSwingRipplesEIGNode[] getIncomingMemberNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_CALLING_NODES_ONLY,NESTING_CONSIDERED_MEMBER_NODES_ONLY);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}


	/**
	 * Returns member nodes that are called by both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */

	public JSwingRipplesEIGNode[] getOutgoingMemberNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_CALLED_NODES_ONLY,NESTING_CONSIDERED_MEMBER_NODES_ONLY);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}

	//-----Node-to-Any Node neigbors ----

	/**
	 * Returns both top and member nodes that both call and are called by both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */
	public JSwingRipplesEIGNode[] getAllAnyNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_BOTH_CALLING_AND_CALLED,NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}


	/**
	 * Returns both top and member nodes that call both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */

	public JSwingRipplesEIGNode[] getIncomingAnyNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);
		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes, DIRECTION_CONSIDERED_CALLING_NODES_ONLY,NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}



	/**
	 * Returns both top and member nodes that are called by both the supplied node and it's member nodes if any
	 * <br>Works by calling {@link #edgesToNeigbors(Set, int, int)} method with predefined parameters
	 * @param node
	 * 	node, whose neighboring nodes should be returned
	 * @return
	 * 	neighboring nodes
	 */

	public JSwingRipplesEIGNode[] getOutgoingAnyNodeNeighbors(
			final JSwingRipplesEIGNode node) {
		if (node==null) return new JSwingRipplesEIGNode[0];
		final HashSet<JSwingRipplesEIGNode> nodes=new HashSet<JSwingRipplesEIGNode>();
		nodes.add(node);

		if (members.containsKey(node))
			nodes.addAll(members.get(node));
		final Collection<JSwingRipplesEIGNode> neighbors=edgesToNeigbors(nodes,DIRECTION_CONSIDERED_CALLED_NODES_ONLY,NESTING_CONSIDERED_BOTH_TOP_AND_MEMBER_NODES);
		return  neighbors
		.toArray(new JSwingRipplesEIGNode[neighbors.size()]);
	}



	// ------------------------------------- EIG edge operations ------------------------

	/**
	 * Creates and adds to the JRipples EIG an edge connecting two supplied nodes:
	 * <br>( fromNode O-------------------------> toNode )<br>
	 * Please note, that the edge is omni-directional, that is  fromNode->toNode does not imply toNode->fromNode.
	 * @param nodeFrom
	 * 	a node, from which this dependency originates
	 * @param nodeTo
	 * 	a node, to which which this dependency points
	 * @return
	 *  an existing edge if one found in JRipples EIG,<br>
	 *  a created edge if there was no such edge declared before,<br>
	 *  <code>null</code> if one or both of the supplied nodes is <code>null</code>, or nodes are equal, or nodes are declared within the same top class
	 */
	public JSwingRipplesEIGEdge addEdge(final JSwingRipplesEIGNode nodeFrom,
			final JSwingRipplesEIGNode nodeTo) {

		if ((nodeTo == null) || (nodeFrom == null)) return null;
		if (nodeTo == nodeFrom ) return null;

		//enforces dependencies between different classes only
		//if (JRipplesEIG.findTopNodeForMemberNode(nodeTo) == JRipplesEIG.findTopNodeForMemberNode(nodeFrom)) return null;

		final JSwingRipplesEIGEdge oldEdge=getEdge(nodeFrom,nodeTo);
		if (oldEdge!=null) {
			oldEdge.setCount(oldEdge.getCount()+1);
			return oldEdge;
		}

		final JSwingRipplesEIGEdge edge = new JSwingRipplesEIGEdge(this, nodeFrom, nodeTo);
		//Add to the list of edges

		edges.add(edge);

		//Add to the adjacency matrix
		if (!edgesAdjacency.containsKey(edge.getFromNode())) edgesAdjacency.put(edge.getFromNode(), new HashMap<JSwingRipplesEIGNode, JSwingRipplesEIGEdge>());
		if (!nodesAdjacency.containsKey(edge.getFromNode())) nodesAdjacency.put(edge.getFromNode(), edge);//this one only records all adjacent nodes
		final HashMap<JSwingRipplesEIGNode, JSwingRipplesEIGEdge> refferedNodes=edgesAdjacency.get(edge.getFromNode());
		refferedNodes.put(edge.getToNode(),edge);

		fireJRipplesEIGChanged(edge, JSwingRipplesEIGEdgeEvent.EDGE_ADDED);
		return edge;


	}

	public void flattenEIG() {

		final JSwingRipplesEIGEdge edges[]=getAllEdges();
		final Set<JSwingRipplesEIGNode> nodesFrom=new HashSet<JSwingRipplesEIGNode>();
		final Set<JSwingRipplesEIGNode> nodesTo=new HashSet<JSwingRipplesEIGNode>();
		for (int i=0;i<edges.length;i++) {
			nodesFrom.clear();
			nodesTo.clear();
			nodesFrom.add(edges[i].getFromNode());
			nodesTo.add(edges[i].getToNode());
			getAllParents(edges[i].getFromNode(),nodesFrom);
			getAllParents(edges[i].getToNode(),nodesTo);

			for (final Iterator <JSwingRipplesEIGNode> iterFrom=nodesFrom.iterator();iterFrom.hasNext();) {
				final JSwingRipplesEIGNode nodeFrom=iterFrom.next();
				for (final Iterator <JSwingRipplesEIGNode> iterTo=nodesTo.iterator();iterTo.hasNext();) {
					final JSwingRipplesEIGNode nodeTo=iterTo.next();
					addEdge(nodeFrom, nodeTo);
				}
			}
		}
	}


	private void getAllParents (final JSwingRipplesEIGNode node, final Set<JSwingRipplesEIGNode> parents) {
		if (node.isTop()) {
			return;
		} else {
			final JSwingRipplesEIGNode parent=findParentNodeForMemberNode(node);
			parents.add(parent);
			getAllParents(parent,parents);
		}
	}

	/**
	 * Returns previously created edge between two nodes, if one found in JRipples EIG.<br>
	 * Please note, that the edge is omni-directional, that is request for fromNode->toNode edge will not return toNode->fromNode
	 * edge even if such exists.
	 * @param nodeFrom
	 * a node, from which this dependency originates
	 * @param nodeTo
	 * a node, to which which this dependency points
	 * @return
	 * previously created edge between two nodes, if one found in JRipples EIG, <br>
	 * <code>null</code> otherwise
	 */
	public JSwingRipplesEIGEdge getEdge(final JSwingRipplesEIGNode nodeFrom,
			final JSwingRipplesEIGNode nodeTo) {
		if ((nodeFrom == null) || (nodeTo == null)) return null;

		if (!edgesAdjacency.containsKey(nodeFrom)) return null;

		final HashMap<JSwingRipplesEIGNode, JSwingRipplesEIGEdge> refferedNodes=edgesAdjacency.get(nodeFrom);
		if (!refferedNodes.containsKey(nodeTo)) return null;

		return refferedNodes.get(nodeTo);
	}





	/**
	 * Returns all the edges, registered with this EIG (that is, created with {@link #addEdge(JSwingRipplesEIGNode, JSwingRipplesEIGNode)} method).
	 * @return
	 * all the edges, registered with this EIG (that is, created with {@link #addEdge(JSwingRipplesEIGNode, JSwingRipplesEIGNode)} method).
	 */
	public JSwingRipplesEIGEdge[] getAllEdges() {
		if (edges==null) return new JSwingRipplesEIGEdge[0];
		return edges.toArray(new JSwingRipplesEIGEdge[edges.size()]);
	}



	/**
	 * Removes an edge from the JRipples EIG.
	 * @param edge
	 * 	edge to remove
	 */
	public void removeEdge(final JSwingRipplesEIGEdge edge) {
		if (edges.contains(edge)) {
			fireJRipplesEIGChanged(edge, JSwingRipplesEIGEdgeEvent.EDGE_REMOVED);

			//Remove from common list

			edges.remove(edge);


			//Remove from Adjacency matrix
			if (!edgesAdjacency.containsKey(edge.getFromNode())) return;
			final HashMap<JSwingRipplesEIGNode, JSwingRipplesEIGEdge> refferedNodes=edgesAdjacency.get(edge.getFromNode());
			if (!refferedNodes.containsKey(edge.getToNode())) return;
			refferedNodes.remove(edge.getToNode());

		}
	}



	/**
	 * Checks whether JRipples EIG contains an edge between two given nodes. <br>
	 * Please note, that this operation is omni-directional, that is <code>existsEdge(A,B)==true</code> does not imply <code>existsEdge(B,A)==true</code>
	 * @param nodeFrom
	 * 	a node, from which this edge originates
	 * @param nodeTo
	 *  a node, to which this to which which this dependency points
	 * @return
	 * <code>true</code> if an edge between two given nodes exists, <br><code>false</code> otherwise
	 */
	public boolean existsEdge(final JSwingRipplesEIGNode nodeFrom,
			final JSwingRipplesEIGNode nodeTo) {
		if ((nodeFrom == null) || (nodeTo == null)) return false;

		if (!edgesAdjacency.containsKey(nodeFrom)) return false;

		final HashMap <JSwingRipplesEIGNode, JSwingRipplesEIGEdge> refferedNodes=edgesAdjacency.get(nodeFrom);
		if (!refferedNodes.containsKey(nodeTo)) return false;

		return true;
	}






	// ------------------------------------ EIG listener------------------------------------------------


	/**
	 * Registers a {@link JSwingRipplesEIGListener} to receive updates on lyficycle and content events of JRipples EIG nodes and edges
	 * @param listener
	 * 	Listener to register
	 */
	public void addJRipplesEIGListener(
			final JSwingRipplesEIGListener listener) {
	    synchronized(eigListeners) {
	        if (!eigListeners.contains(listener)) {
	            eigListeners.add(listener);
	        }
	    }
	}

	/**
	 * Unregisters a listener, previously registered with {@link #addJRipplesEIGListener(JSwingRipplesEIGListener)}.
	 * @param listener
	 * 	Listener to unregister
	 */
	public void removeJRipplesEIGListener(
			final JSwingRipplesEIGListener listener) {
	    synchronized (eigListeners) {
	        eigListeners.remove(listener);
	    }
	}

    protected void fireJRipplesEIGChanged(final JSwingRipplesEIGNode item,
            final int type, final String oldValue, final String newValue) {
        final JSwingRipplesEIGNodeEvent event = new JSwingRipplesEIGNodeEvent(item,
                type, oldValue, newValue);

        notifyListeners(new JSwingRipplesEIGEvent(this,
                new JSwingRipplesEvent[]{event}));
    }

	protected void fireJRipplesEIGChanged(final JSwingRipplesEIGEdge item,
			final int type) {
        final JSwingRipplesEIGEdgeEvent event = new JSwingRipplesEIGEdgeEvent(item, type);
        notifyListeners(new JSwingRipplesEIGEvent(this, new JSwingRipplesEvent[]{event}));
	}
	private  void notifyListeners(final JSwingRipplesEIGEvent event) {
	    //create working copy of listener list.
	    List<JSwingRipplesEIGListener> list;
	    synchronized (eigListeners) {
	        list = new LinkedList<JSwingRipplesEIGListener>(eigListeners);
	    }

	    for (final JSwingRipplesEIGListener l : list) {
	        l.jRipplesEIGChanged(event);
        }
	}
    /**
     * @return the project name.
     */
    public JavaProject getJavaProject() {
        return projectName;
    }
    /**
     * @return the history.
     */
    public History getHistory() {
        return history;
    }
}
