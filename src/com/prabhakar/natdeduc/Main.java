/* NatDeduc: a Java application with a Swing GUI to verify or 
 * find valid natural deductions in truth-functional first-order
 * logic.
 * 
 * Main: sets up the GUI
 * DeducPanel: handles the GUI for finding a deduction from given premises to a given conclusion
 * CheckPanel: handles the GUI for checking the validity of an inputted deduction
 * Parser: interprets a String as a proposition of FOL, whether from user input or from brute-force
 * Deducer: finds a deduction from one proposition/s to another by brute force
 * Exporter: writes the completed deduction to disk
 */

package com.prabhakar.natdeduc;

import java.awt.*;
import javax.swing.*;

public class Main extends JFrame {
	JTabbedPane tabs;
	DeducPanel deducPanel;
	CheckPanel checkPanel;
	
	final char assumpChar = (char) 12288;
	
	public Main() {
		super("NatDeduc");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initTabs();
		pack();
		setResizable(false);
		
		// Position the window pleasantly
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenw = screenSize.width;
		int framex = (screenw/2) - (this.getSize().width/2);
		this.setLocation(framex, 200);
		
		setVisible(true);
	}
	
	
	// At the top level, the GUI consists in a JPanel called pane, and one below called keysPane
	// which instantiates KeysPanel. Within pane is a TabbedPane
	// The tabs are each set up by their own methods
	// deducPanel: takes premise/s and conclusion and finds a deduction
	// checkPanel: takes a proposed deduction and checks for validity
	
	void initTabs() {
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		
		tabs = new JTabbedPane();
		deducPanel = new DeducPanel(this);
		checkPanel = new CheckPanel(this);
		tabs.addTab("Find Natural Deduction", deducPanel);
		tabs.addTab("Check Validity", checkPanel);
		pane.add(tabs);
		
		KeysPanel keysPanel = new KeysPanel(this);
		pane.add(keysPanel);
		
		add(pane);
		pack();
	}
	
	// Called by deducPanel and checkPanel to repack when a premise has been added
	void premButton() {
		pack();
	}
	
	// Called by keysPanel to add a symbol to whichever TextField is in focus
	void keyButton(String key) {
		// Check which tab has focus, then which JTextField within it has focus
		// We use getSelectedIndex() because the TabbedPane as a whole won't be within focus
		if (key=="∀x") key = "∀";
		if (key=="∃x") key = "∃";
		if (key=="Assumption") key = Character.toString(assumpChar);
		
		if (tabs.getSelectedIndex()==0) {
			if (deducPanel.focused!=null) {
				deducPanel.focused.replaceSelection(key);
			}
		} else {
			if (checkPanel.focused!=null) {
				checkPanel.focused.replaceSelection(key);
			}
		}
	}
	
	public static void main(String[] args) {
		Main NatDeduc = new Main();
		
		//Parser p = new Parser();
		//p.parseLine("(sv(p&q)=(j^(~d)^j)vj)");
		//p.parseLine("(j(j))");
		//p.parseLine("p");
		//p.parseLine("(p&(q&r)&(q&(r&s))");
		//p.parseLine("(p&(q&r))&(q&(r&s))");
	}
}