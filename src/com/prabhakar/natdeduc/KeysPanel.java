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
	final int ASSUMPKEY = 9;
	
	Main mainFrame;
	
	public KeysPanel(Main m) {
		super();
		FlowLayout flow = new FlowLayout();
		this.setLayout(flow);
		mainFrame = m;
		
		keyButtons = new JButton[10];
		keyButtons[AXKEY] = new JButton("∀x");
		keyButtons[EXKEY] = new JButton("∃x");
		keyButtons[CONDKEY] = new JButton("→");
		keyButtons[BICONDKEY] = new JButton("↔");
		keyButtons[NOTKEY] = new JButton("¬");
		keyButtons[NOTEQKEY] = new JButton("≠");
		keyButtons[ANDKEY] = new JButton("∧");
		keyButtons[ORKEY] = new JButton("∨");
		keyButtons[CONTRKEY] = new JButton("⊥");
		keyButtons[ASSUMPKEY] = new JButton("Assumption");
		
		// Until we implement FOL
		keyButtons[AXKEY].setEnabled(false);
		keyButtons[EXKEY].setEnabled(false);
		keyButtons[NOTEQKEY].setEnabled(false);
		
		Dimension keySize = new Dimension(45, 30);
		for (int i=0; i<keyButtons.length; i++) {
			if (i!=ASSUMPKEY) keyButtons[i].setPreferredSize(keySize);
			keyButtons[i].setFocusable(false);
			add(keyButtons[i]);
			keyButtons[i].addActionListener(this);
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		mainFrame.keyButton(e.getActionCommand());
	}
}