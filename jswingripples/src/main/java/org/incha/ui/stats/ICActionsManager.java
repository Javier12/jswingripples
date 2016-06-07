package org.incha.ui.stats;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.incha.core.ModuleConfiguration;
import org.incha.core.jswingripples.JRipplesICModuleInterface;
import org.incha.core.jswingripples.eig.JSwingRipplesEIG;
import org.incha.core.jswingripples.eig.JSwingRipplesEIGNode;
import org.incha.ui.dependency.ShowDependencyAction;
import org.incha.ui.jripples.JRipplesViewsConstants;

public final class ICActionsManager {
    private static final ICActionsManager instance = new ICActionsManager();
	private ICActionsManager() {}


	public void showMenuForNode(final JSwingRipplesEIGNode node, final int x, final int y, final JComponent context) {
	    final JPopupMenu popup = new JPopupMenu();
	    final ModuleConfiguration cfg = node.getEig().getJavaProject().getModuleConfiguration();

        final String oldMark=node.getMark();
        final JRipplesICModuleInterface incremantealChangeModule
            = ModuleConfiguration.createIncrementalChangeModule(cfg.getIncrementalChange(), node.getEig());
        final Set<String> newPossibleMarks=incremantealChangeModule.GetAvailableRulesForMark(oldMark);

        if (newPossibleMarks !=null) {

            //2. Create out menu items for changing marks at the current granularity
            for (final String newMark:newPossibleMarks) {
                final JMenuItem item = new JMenuItem(new ICAction(node, newMark, 0, true, context));
                popup.add(item);
            }

            //3. Create menu for other granularities
            popup.add(new JSeparator(JSeparator.HORIZONTAL));

            final int nodeNesting=checkNestingLevel(node.getEig().findTopNodeForIMember(node.getNodeIMember()), node);
            final int deepestMember = findDeepestMember(node,0);

            if (nodeNesting==0 && deepestMember==0) {
                return;
            }

            final JMenu granulrity = new JMenu(JRipplesViewsConstants.GRANULARITY);

            for (int i=nodeNesting;i>0;i--) {
                final JMenu granularitiesManagerTmp = new JMenu("Parent "+" (granularity-"+i+")");
                for (final String newMark:newPossibleMarks)
                    granularitiesManagerTmp.add(new ICAction(node, newMark,0-i,true, context));
                granulrity.add(granularitiesManagerTmp);
            }
            for (int i=0;i<deepestMember;i++) {
                final JMenu granularitiesManagerTmp = new JMenu("Members"+" (granularity+"+(i+1)+")");
                for (final String newMark:newPossibleMarks)
                    granularitiesManagerTmp.add(new ICAction(node, newMark,i+1,true, context));
                granulrity.add(granularitiesManagerTmp);
            }

            popup.add(granulrity);

            //4. Add edge level menu
            fillICActionsMenuEdgeLevel(popup, node,newPossibleMarks, context);
        }

        fillDefaultActions(popup, node, context);

        //show popup.
        popup.show(context, x, y);
	}

    /**
     * @param popup
     * @param eig
     * @param context
     */
    public void fillDefaultActions(final JPopupMenu popup,
            final JSwingRipplesEIGNode node, final Component context) {
        final JSwingRipplesEIG eig = node.getEig();
        popup.add(new JSeparator(JSeparator.HORIZONTAL));
        popup.add(new ShowDependencyAction(node));
        popup.add(new JSeparator(JSeparator.HORIZONTAL));
        popup.add(new SpecificGraphAction("Graph dependencies"));
        popup.add(new JSeparator(JSeparator.HORIZONTAL));
        popup.add(new UndoAction(eig.getHistory(), "undo", context));
        popup.add(new RedoAction(eig.getHistory(), "redo", context));
        popup.add(new AbstractAction("copy") {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(final ActionEvent e) {
                final StringSelection text = new StringSelection(node.getShortName()
                        + "\t" + node.getMark());
                final Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard ();
                clpbrd.setContents(text, null);
            }
        });
    }


    private int findDeepestMember(final JSwingRipplesEIGNode n,final int currentLevel) {
		int res=currentLevel;
		for (final JSwingRipplesEIGNode member: n.getEig().getNodeMembers(n)) {
			final int k=findDeepestMember(member, currentLevel+1);
			if (k>res) res=k;
		}
		return res;
	}

	private int checkNestingLevel(final JSwingRipplesEIGNode currentNode, final JSwingRipplesEIGNode neededNode) {
		if (currentNode.equals(neededNode)) return 0;
		for (final JSwingRipplesEIGNode member: currentNode.getEig().getNodeMembers(currentNode)) {
			final int k=checkNestingLevel(member, neededNode);
			if (k!=-1) return k+1;
		}
		return -1;
	}

	public void fillICActionsMenuEdgeLevel(final JPopupMenu manager,final JSwingRipplesEIGNode node,
	        final Set <String> newPossibleMarks, final Container context) {
        final JSwingRipplesEIGNode[] relatedNodes = node.getEig()
                .getAllAnyNodeNeighbors(node);
        if (relatedNodes != null && relatedNodes.length > 0) {
            manager.add(new JSeparator(JSeparator.HORIZONTAL));

            for (final String rule : newPossibleMarks) {
                final JMenu edgesManager = new JMenu(JRipplesViewsConstants.GRANULARITY_EDGE_PREFIX + rule);
                for (int i = 0; i < relatedNodes.length; i++) {
                    final ICAction action = new ICAction(node, relatedNodes[i], rule, false, context);
                    edgesManager.add(action);
                }
                manager.add(edgesManager);
            }
        }
	}

	/**
     * @return the instance
     */
    public static ICActionsManager getInstance() {
        return instance;
    }
}
