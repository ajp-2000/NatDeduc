package com.prabhakar.natdeduc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class CheckPanel extends KeyablePanel implements ActionListener {
	JTextArea bodyArea;
	JScrollPane bodyScroll;
	JButton checkButton;
	
	public CheckPanel(Main m) {
		// Init some things
		super(m);
		prems = 2;
		
		// Add JLabels and JTextFields for the premises
		for (int i=0; i<prems; i++) {
			addPrem();
		}
		
		// And for the body of the deduction, then the end matter
		addBodyArea();
		addConc();
		addCheckButton();
		addPremButton();
	}
	
	private String[] parseBody(String body) {
		return body.split(System.lineSeparator());
	}
	
	// Bring things together to check the validity of the deduction
	private boolean check() {
		// Sort out the premises
		int numPrems = 0;
		for (int p=0; p<prems; p++) {
			String curr = premFields[p].getText();
			if (!curr.isEmpty()) numPrems++;
		}
		
		if (numPrems==0) {
			JOptionPane.showMessageDialog(mainFrame, "Please provide at least one premise.", "No premises entered", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		Proposition[] premProps = new Proposition[numPrems];
		for (int p=0; p<numPrems; p++) {
			Parser pars = new Parser();
			try {
				premProps[p] = pars.parseLine(premFields[p].getText());
			} catch(ParseException e) {
				JOptionPane.showMessageDialog(mainFrame, e.getMessage(), "Syntax error in premise " + Integer.toString(p+1), JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		
		// And the body
		String[] body = parseBody(bodyArea.getText());
		int lenBody = 0;
		for (String s:body) {
			if (!s.isEmpty()) lenBody++;
		}
		
		if (lenBody==0) {
			JOptionPane.showMessageDialog(mainFrame, "Please provide at least one line of working.", "No body entered", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		Proposition[] bodyProps = new Proposition[lenBody];
		for (int b=0; b<lenBody; b++) {
			Parser pars = new Parser();
			try {
				bodyProps[b] = pars.parseLine(body[b]);
			} catch(ParseException e) {
				JOptionPane.showMessageDialog(mainFrame, e.getMessage(), "Syntax error in line " + Integer.toString(b+1), JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		
		// And the conclusion
		String conc = concField.getText();
		if (conc.isEmpty()) {
			JOptionPane.showMessageDialog(mainFrame, "Please provide a conclusion.", "No conclusion entered", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		Proposition concProp;
		Parser pars = new Parser();
		try {
			concProp = pars.parseLine(conc);
		} catch(ParseException e) {
			JOptionPane.showMessageDialog(mainFrame, e.getMessage(), "Syntax error in conclusion", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		// Construct the deduction
		for (int pr=0; pr<numPrems; pr++) {
			if (premProps[pr].sameAs(concProp)) {
				JOptionPane.showMessageDialog(mainFrame, "This natural deduction is vacuously valid.", "Deduction valid", JOptionPane.INFORMATION_MESSAGE);
				return true;
			}
		}
		
		Deduction d = new Deduction(premProps, bodyProps, concProp, mainFrame);
		if (d.check()) {
			JOptionPane.showMessageDialog(mainFrame, "This natural deduction is valid.", "Deduction valid", JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		
		
		return false;
	}
	
	private void addBodyArea() {
		bodyArea = new JTextArea(7, 20);
		bodyScroll = new JScrollPane(bodyArea);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = prems + 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.insets = fieldInset;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		
		bag.setConstraints(bodyScroll, c);
		this.add(bodyScroll);
		bodyArea.addFocusListener(this);
		
		gridy = prems + 2;
	}
	
	// Add the button to check the natural deduction's validity
	private void addCheckButton() {
		checkButton = new JButton("Check");
		labelConstraint.gridy = prems + 3;
		bag.setConstraints(checkButton, labelConstraint);
		this.add(checkButton);
		checkButton.addActionListener(this);
	}
	
	private void incrPrems() {
		String conc = concField.getText();
		String body = bodyArea.getText();
		
		// Remove the components below the new premise
		this.remove(bodyScroll);
		this.remove(concLabel);
		this.remove(concField);
		this.remove(checkButton);
		this.remove(premButton);
		
		
		// Add new premise
		prems++;
		gridy -= 2;
		addPrem();
		addBodyArea();
		addConc();
		addCheckButton();
		addPremButton();
		//concField.replaceSelection(conc);
		bodyArea.replaceSelection(body);
		
		repaint();
		revalidate();
		mainFrame.premButton();
	}
	
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
			case "Check":
				check();
				break;
			case "Add premise":
				incrPrems();
				break;
		}
	}
}