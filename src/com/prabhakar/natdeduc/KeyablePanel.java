/* Handles keeping track of which JTExtField is focused. Instantiated by
 * DeducPanel and CheckPanel.
 * This class also features some code that DeducPanel and CheckPanel deploy
 * in setting up their respective GUIs, albeit slightly differently.
 */

package com.prabhakar.natdeduc;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

public class KeyablePanel extends JPanel implements ActionListener, FocusListener {
	Main mainFrame;
	JTextComponent focused;
	
	// Things for shared elements of the two tabs that are used slightly differently
	JLabel[] premLabels;
	JTextField[] premFields;
	JLabel concLabel;
	JTextField concField;
	int prems, currPrem;
	final int MAX_PREMS = 100;
	JButton premButton;
	
	GridBagLayout bag;
	GridBagConstraints labelConstraint, fieldConstraint;
	int gridy = 0;
	Insets labelInset, fieldInset;
	
	public KeyablePanel(Main m) {
		super();
		mainFrame = m;
		currPrem = 0;
		
		// GUI things
		bag = new GridBagLayout();
		this.setLayout(bag);
		
		premLabels = new JLabel[MAX_PREMS];
		premFields = new JTextField[MAX_PREMS];
		
		labelInset = new Insets(1, 3, 1, 0);
		fieldInset = new Insets(1, 0, 1, 3);
		labelConstraint = initLabelConstraint();
		fieldConstraint = initFieldConstraint();
	}
	
	// Two methods to set up the Constraints object used by the premises and conclusion
	GridBagConstraints initLabelConstraint() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.insets = labelInset;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		
		
		return c;
	}
	
	GridBagConstraints initFieldConstraint() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.weightx = 100;
		c.insets = fieldInset;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		
		
		return c;
	}
	
	// Add a premise JLabel at the present y-value in the bag, then a JTextField, then go down one
	void addPrem() {
		// The JLabel
		currPrem++;
		labelConstraint.gridy = gridy;
		premLabels[gridy] = new JLabel("Prem. " + Integer.toString(currPrem) + " ");
		bag.setConstraints(premLabels[gridy], labelConstraint);
		this.add(premLabels[gridy]);
		
		// The JTextField
		fieldConstraint.gridy = gridy;
		premFields[gridy] = new JTextField();
		bag.setConstraints(premFields[gridy], fieldConstraint);
		this.add(premFields[gridy]);
		premFields[gridy].addFocusListener(this);
		
		gridy++;
	}
	
	// Similarly for the conclusion
	void addConc() {
		// The JLabel
		labelConstraint.gridy = gridy;
		concLabel = new JLabel("Conc. ");
		bag.setConstraints(concLabel, labelConstraint);
		this.add(concLabel);
		
		// The JTextField
		fieldConstraint.gridy = gridy;
		concField = new JTextField();
		bag.setConstraints(concField, fieldConstraint);
		this.add(concField);
		concField.addFocusListener(this);
	}
	
	// And the button to add a new premise
	void addPremButton() {
		premButton = new JButton("Add premise");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = gridy + 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.EAST;
		
		bag.setConstraints(premButton, c);
		this.add(premButton);
		premButton.addActionListener(this);
	}
	
	// Both panels need to do this when checking/deducing
	int checkPrems() {
		int numPrems = 0;
		for (int p=0; p<prems; p++) {
			String curr = premFields[p].getText();
			if (!curr.isEmpty()) numPrems++;
		}
		
		if (numPrems==0) {
			JOptionPane.showMessageDialog(mainFrame, "Please provide at least one premise.", "No premises entered", JOptionPane.ERROR_MESSAGE);
			return -1;
		}
		
		return numPrems;
	}
	
	public void actionPerformed(ActionEvent e) {}
	
	public void focusGained(FocusEvent e) {
		try {
			focused = (JTextComponent) e.getSource();
		} catch(ClassCastException ex) {}
	}
	
	public void focusLost(FocusEvent e) {
	}
}