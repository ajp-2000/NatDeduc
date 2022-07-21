package com.prabhakar.natdeduc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class KeysPanel extends JPanel implements ActionListener {
	JButton[] keyButtons;
	final int AxKey = 0;
	final int ExKey = 1;
	final int condKey = 2;
	final int biCondKey = 3;
	final int notKey = 4;
	final int notEqKey = 5;
	final int andKey = 6;
	final int orKey = 7;
	final int contrKey = 8;
	final int assumpKey = 9;
	
	Main mainFrame;
	
	public KeysPanel(Main m) {
		super();
		FlowLayout flow = new FlowLayout();
		this.setLayout(flow);
		mainFrame = m;
		
		keyButtons = new JButton[10];
		keyButtons[AxKey] = new JButton("∀x");
		keyButtons[ExKey] = new JButton("∃x");
		keyButtons[condKey] = new JButton("→");
		keyButtons[biCondKey] = new JButton("↔");
		keyButtons[notKey] = new JButton("¬");
		keyButtons[notEqKey] = new JButton("≠");
		keyButtons[andKey] = new JButton("∧");
		keyButtons[orKey] = new JButton("∨");
		keyButtons[contrKey] = new JButton("⊥");
		keyButtons[assumpKey] = new JButton("Assumption");
		
		Dimension keySize = new Dimension(45, 30);
		for (int i=0; i<keyButtons.length; i++) {
			if (i!=assumpKey) keyButtons[i].setPreferredSize(keySize);
			keyButtons[i].setFocusable(false);
			add(keyButtons[i]);
			keyButtons[i].addActionListener(this);
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		mainFrame.keyButton(e.getActionCommand());
	}
}