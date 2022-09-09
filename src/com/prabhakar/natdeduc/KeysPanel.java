package com.prabhakar.natdeduc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class KeysPanel extends JPanel implements ActionListener {
	JButton[] keyButtons;
	final int AXKEY = 0;
	final int EXKEY = 1;
	final int CONDKEY = 2;
	final int BICONDKEY = 3;
	final int NOTKEY = 4;
	final int NOTEQKEY = 5;
	final int ANDKEY = 6;
	final int ORKEY = 7;
	final int CONTRKEY = 8;
	JButton premButton;
	
	Main mainFrame;
	
	public KeysPanel(Main m) {
		super();
		FlowLayout flow = new FlowLayout();
		this.setLayout(flow);
		mainFrame = m;
		
		keyButtons = new JButton[9];
		keyButtons[AXKEY] = new JButton("∀x");
		keyButtons[EXKEY] = new JButton("∃x");
		keyButtons[CONDKEY] = new JButton("→");
		keyButtons[BICONDKEY] = new JButton("↔");
		keyButtons[NOTKEY] = new JButton("¬");
		keyButtons[NOTEQKEY] = new JButton("≠");
		keyButtons[ANDKEY] = new JButton("∧");
		keyButtons[ORKEY] = new JButton("∨");
		keyButtons[CONTRKEY] = new JButton("⊥");
		premButton = new JButton("Add premise");
		
		// Until we implement FOL
		keyButtons[AXKEY].setEnabled(false);
		keyButtons[EXKEY].setEnabled(false);
		keyButtons[NOTEQKEY].setEnabled(false);
		
		Dimension keySize = new Dimension(45, 30);
		for (int i=0; i<keyButtons.length; i++) {
			keyButtons[i].setPreferredSize(keySize);
			keyButtons[i].setFocusable(false);
			add(keyButtons[i]);
			keyButtons[i].addActionListener(this);
		}
		
		premButton.setEnabled(true);
		add(premButton);
		premButton.addActionListener(this);
	}
	
	public void actionPerformed(ActionEvent e) {
		// Go to Main, so that the same method can add the character to either of the two panels
		mainFrame.keyButton(e.getActionCommand());
	}
}