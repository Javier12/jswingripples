package org.incha.ui.search;

import org.apache.lucene.queryParser.ParseException;
import org.graphstream.graph.Graph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.incha.core.jswingripples.NodeSearchBuilder;
import org.incha.core.search.Searcher;
import org.incha.ui.JSwingRipplesApplication;

import javax.swing.*;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages search upon user search action.
 */
class StartSearchAction implements ActionListener {
    private List<String> words;
    private JTextField searchedWords;
    private Searcher searcher;

    StartSearchAction(JTextField searchedWords) {
        this.searchedWords = searchedWords;
        searcher = Searcher.getInstance();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    	
        words = new ArrayList<>();
        NodeSearchBuilder.getInstance().clearGraph();
        final String text = searchedWords.getText();
        final String cleanedText = text.trim();
        if (text != null && cleanedText.length() > 0) {
            words = getWordList(cleanedText);
            // perform search
            for (String word : words) {
                try {
                    searcher.search(word);
                    System.out.println("ADD TO GRAPH THE WORD " + word);
                    addToGraph(word);
                } catch (IOException | ParseException e1) {
                    e1.printStackTrace();
                }
            }
            showGraph();
        }
    }

    private List<String> getWordList(String text) {
        words.clear();
        final StringBuffer word = new StringBuffer("");
        for (int i = 0; i < text.length(); i ++) {
            if (text.charAt(i) == ';') {  // Finds a word separator
                words.add(word.toString().trim());  // New word is added to the list
                word.delete(0, word.length());  // StringBuffer is cleared to store another word
            }
            else {
                word.append(text.charAt(i));  // Character concatenated to current word
            }
        }
        words.add(word.toString().trim());
        return words;
    }

    public void addToGraph(String s){
    	NodeSearchBuilder.getInstance().setSearch(s);
    }
    public void showGraph()
    {
    	System.out.println("SHOWING GRAPH");
        //NodeSearchBuilder NS = NodeSearchBuilder.getInstance();
        //NS.setSearch(search);
        //NS.setSearch("Node");
        Graph graph = NodeSearchBuilder.getInstance().getGraph();
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
    
    // Prints received words. For testing purposes only.
    private void print(List<String> words) {
        for(String word : words) {
            System.out.println(word);
        }
    }
}
