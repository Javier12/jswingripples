package org.incha.ui.search;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

public class NewSearchMenu {

	private JPanel searchPanel;
	
	/**
     * Default constructor. Se crea un panel de búsqueda para colocarlo en algún menú.
     */
	public NewSearchMenu() {
		searchPanel = new JPanel();
		JTextField searchedWords = new JTextField(15);
        searchPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        searchPanel.setLayout(new FlowLayout());
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchedWords);
        JButton searchButton = new JButton("Go");
        searchButton.addActionListener(new StartSearchAction(searchedWords));
        searchPanel.add(searchButton);
	}
	
	public JPanel getSearchPanel() {
		return searchPanel;
	}
}
