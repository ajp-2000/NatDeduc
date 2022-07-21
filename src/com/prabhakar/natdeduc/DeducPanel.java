package com.prabhakar.natdeduc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DeducPanel extends KeyablePanel implements ActionListener {
	JButton deducButton;
	
	public DeducPanel(Main m) {
		// Initialise various things
		super(m);
		prems = 5;
		
		// Add JLabels and JTextFields for the deduction
		for (int i=0; i<prems; i++) {
			addPrem();
		}
		addConc();
		
		// Add JButtons (find deduction and add premise)
		addDeducButton();
		addPremButton();
	}
	
	// Add the button to calculate the natural deduction
	private void addDeducButton() {
		deducButton = new JButton("Deduce");
		labelConstraint.gridy = prems + 1;
		bag.setConstraints(deducButton, labelConstraint);
		this.add(deducButton);
		deducButton.addActionListener(this);
	}
	
	// Increase the number of premises in the Panel once it has already been drawn
	private void incrPrems() {
		String conc = concField.getText();
		
		// Remove the components below the new premise
		this.remove(concLabel);
		this.remove(concField);
		this.remove(deducButton);
		this.remove(premButton);
		
		// Add new premise
		prems++;
		addPrem();
		addConc();
		addDeducButton();
		addPremButton();
		concField.replaceSelection(conc);
		
		repaint();
		revalidate();
		mainFrame.premButton();
	}
	
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
			case "Deduce":
				System.out.println("Dedoocing");
				break;
			case "Add premise":
				incrPrems();
				break;
		}
	}
}