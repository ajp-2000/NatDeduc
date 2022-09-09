package com.prabhakar.natdeduc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.util.*;

public class CheckPanel extends KeyablePanel implements ActionListener, CaretListener, FocusListener {
	JTextPane bodyPane;
	JScrollPane bodyScroll;
	JButton checkButton, clearButton;
	JToggleButton assumpButton;
	
	final float indentSize = 10F;
	
	public CheckPanel(Main m) {
		// Init some things
		super(m);
		prems = 2;
		
		// Add JLabels and JTextFields for the premises
		for (int i=0; i<prems; i++) {
			addPrem();
		}
		
		// And for the body of the deduction, then the end matter
		addBodyPane();
		addConc();
		addCheckButton();
		addAssumpButton();
		addClearButton();
	}
	
	// The bulk of the work done here is in interpreting indents as assumption blocks
	// Now, assumption blocks are preceded by '@' and '£', alternating between blocks, so that one can begin
	// immediately after the previous block ends
	private String[] parseBody() {
		String[] raw = bodyPane.getText().split(System.lineSeparator());
		String[] escapes = new String[raw.length];
		
		// First, go back through the JTextPane looking for left indents and underlines
		int oldPos = bodyPane.getCaretPosition();
		int pos = 0;
		int line = 0;
		String escape = "£";									// So that the first underline will switch it to '@'
		
		// Use the exception reached at the end of JTextPane to break out of the loop
		// getText() throws BadLocationException, and setCaretPosition(), IllegalArgumentException
		boolean newLine = true;
		while (true) {
			try {
				bodyPane.setCaretPosition(pos);
				char ch = bodyPane.getText(pos, 1).toCharArray()[0];
				if (newLine) {
					// This point is reached for each character which begins a new line.
					// It's here that we check for indents and underlines
					AttributeSet indentSet = bodyPane.getParagraphAttributes();
					AttributeSet underlineSet = bodyPane.getCharacterAttributes();
					
					// The first line of an assump block will have both, but we only need to act on one
					if (underlineSet.containsAttribute(StyleConstants.Underline, true)) {
						escape = (escape=="@") ? "£" : "@";
						escapes[line] = escape;
					} else if (indentSet.containsAttribute(StyleConstants.LeftIndent, indentSize)) {
						escapes[line] = escape;
					} else {
						escapes[line] = "";
					}
					
					
					newLine = false;
					line++;
				}
				
				if (ch == '\n') newLine = true;

				pos++;
			} catch(Exception e) {
				break;
			}
		}
		
		// Restore the caret to its position before we started manipulating it
		bodyPane.setCaretPosition(oldPos);
		
		// Put the escape characters at the beginning of each line
		String[] escaped = new String[raw.length];
		for (int i=0; i<raw.length; i++) {
			escaped[i] = escapes[i] + raw[i];
		}
		
		
		return escaped;
	}
	
	// Bring things together to check the validity of the deduction
	private boolean check() {
		// Sort out the premises
		int numPrems = checkPrems();
		if (numPrems==-1) return false;
		
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
		String[] body = parseBody();
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
	
	/* Handling assumption blocks
	 * A boolean tracks whether we are in one, and an ArrayList tracks which cursor positions are at the end
	 * of the underlined segment that the first line of any assumption block is, for use in caretUpdate() below
	 */
	boolean inAssump = false;
	boolean organic = true;
	ArrayList<Integer> underlineEnds = new ArrayList<Integer>();
	
	// Only called when inAssump is false
	boolean enterAssump() {
		// The boolean organic tracks whether this method was called by the user pressing assumpButton, or by
		// the cursor moving into an assumption block
		if (!organic) {
			organic = true;
			return true;
		}
		organic = true;
		
		if (focused==bodyPane) {
			// Something has to be selected
			String line = bodyPane.getSelectedText();
			if (line==null) {
				JOptionPane.showMessageDialog(mainFrame, "Please select a proposition to make it an assumption.", "Can't insert assumption", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			inAssump = true;
			
			// Underline the selected and indent it, stopping the underline for successive lines
			// Set indent as a paragraph attribute, and underline as a character attribute
			bodyPane.replaceSelection(line);
			int pos = bodyPane.getCaretPosition() - line.length();
			
			MutableAttributeSet indentSet = new SimpleAttributeSet();
			MutableAttributeSet underlineSet = new SimpleAttributeSet();
			StyleConstants.setUnderline(underlineSet, true);
			StyleConstants.setLeftIndent(indentSet, indentSize);
			
			bodyPane.getStyledDocument().setParagraphAttributes(pos, line.length(), indentSet, false);
			bodyPane.getStyledDocument().setCharacterAttributes(pos, line.length(), underlineSet, true);
			
			StyleConstants.setUnderline(underlineSet,  false);
			bodyPane.getStyledDocument().setCharacterAttributes(pos + line.length(), 0, underlineSet, true);
			try {
				int newPos = bodyPane.getCaretPosition();
				underlineEnds.add(new Integer(newPos));
				bodyPane.getDocument().insertString(newPos, "\n", indentSet);
			} catch(BadLocationException e) {}
			
			return true;
		} else {
			JOptionPane.showMessageDialog(mainFrame, "Assumption blocks can only be inserted in the body of a deduction.", "Can't insert assumption", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	// Only called when inAssump is true
	boolean leavingIndent = false;
	
	void exitAssump() {
		// As above
		if (!organic) {
			organic = true;
			return;
		}
		organic = true;
		
		// Remove indent
		MutableAttributeSet attrSet = new SimpleAttributeSet();
		StyleConstants.setUnderline(attrSet,  false);
		StyleConstants.setLeftIndent(attrSet, 0);
		bodyPane.getStyledDocument().setParagraphAttributes(bodyPane.getCaretPosition(), 0, attrSet, true);
		bodyPane.getStyledDocument().setCharacterAttributes(bodyPane.getCaretPosition(), 0, attrSet, true);
		inAssump = false;
		leavingIndent = false;
	}
	
	private void addBodyPane() {
		bodyPane = new JTextPane();
		bodyPane.setPreferredSize(new Dimension(7, 20));
		bodyScroll = new JScrollPane(bodyPane);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = prems + 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.weighty = 5;
		c.insets = fieldInset;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		
		bag.setConstraints(bodyScroll, c);
		this.add(bodyScroll);
		bodyPane.addFocusListener(this);
		bodyPane.addCaretListener(this);
		
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
	
	void incrPrems() {
		String conc = concField.getText();
		String body = bodyPane.getText();
		
		// Remove the components below the new premise
		this.remove(bodyScroll);
		this.remove(concLabel);
		this.remove(concField);
		this.remove(checkButton);
		this.remove(assumpButton);
		this.remove(clearButton);
		
		
		// Add new premise
		prems++;
		gridy -= 2;
		addPrem();
		addBodyPane();
		addConc();
		addCheckButton();
		addAssumpButton();
		addClearButton();
		concField.replaceSelection(conc);
		bodyPane.replaceSelection(body);
		
		repaint();
		revalidate();
		mainFrame.premButton();
	}
	
	// Add the button to toggle assumption mode
	private void addAssumpButton() {
		assumpButton = new JToggleButton("Assumption");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = gridy + 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.EAST;
		
		bag.setConstraints(assumpButton, c);
		this.add(assumpButton);
		assumpButton.addActionListener(this);
	}
	
	// Add the button to reset the body JTextPane
	private void addClearButton() {
		clearButton = new JButton("Clear body");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = gridy + 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.EAST;
		
		bag.setConstraints(clearButton, c);
		this.add(clearButton);
		clearButton.addActionListener(this);
	}
	
	// Remove all text and any indents/underlines
	private void clearBodyPane() {
		//bodyPane = new JTextPane();
		//bodyScroll = new JScrollPane(bodyPane);
		bodyPane.setText("");
		
		AttributeSet clearSet = new SimpleAttributeSet();
		bodyPane.setParagraphAttributes(clearSet, true);
		bodyPane.setCharacterAttributes(clearSet,  true);
	}
	
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
			case "Check":
				check();
				break;
			case "Assumption":
				// A bit of wrapper work to toggle assumption mode
				if (inAssump) {
					leavingIndent = true;
					exitAssump();
				} else {
					if (!enterAssump()) {
						assumpButton.setSelected(false);
					}
				}
				focused.grabFocus();
				break;
			case "Clear body":
				clearBodyPane();
				break;
		}
	}
	
	boolean underlineRemoved = false;										// Prevent infinite recursion
	public void caretUpdate(CaretEvent e) {
		Runnable processCaret = new Runnable() {
			@Override
			public void run() {
				// Refresh the infinite-recursion-preventer whenever the cursor moves
				if (underlineRemoved) {
					underlineRemoved = false;
					return;
				}
				
				AttributeSet attrSet = bodyPane.getParagraphAttributes();
				if (attrSet.containsAttribute(StyleConstants.LeftIndent, indentSize)) {
					// If caretUpdate has only been triggered because we're leaving an indent and it hasn't
					// fully been processed yet, ignore it
					//System.out.println("Caret updated to inside an assumption block. leavingIndent = " + Boolean.toString(leavingIndent));
					if (leavingIndent) {
						System.out.println("In assump block, but in the process of leaving");
						leavingIndent = false;
						return;
					}
					//System.out.println("Caret newly inside assump block: removing underline and clicking button");
					
					// Work out whether we're at the end of the underline segment
					int pos = bodyPane.getCaretPosition();
					if (underlineEnds.contains(new Integer(pos))) {
						// If the user clicked to the end of the underlined line, remove that underline again
						// But only if the user pressed enter at the end of the underline
						MutableAttributeSet newSet = new SimpleAttributeSet();
						StyleConstants.setUnderline(newSet,  false);
						bodyPane.getStyledDocument().setParagraphAttributes(pos, 0, newSet, false);
						
						// With the new not-underlined style in place, rewrite the newline if it's there at all
						try {
							String last = bodyPane.getText(pos, 1);
							if ((int)last.toCharArray()[0] == 10) {
								bodyPane.getStyledDocument().remove(pos, 1);
								bodyPane.getDocument().insertString(pos, "\n", newSet);
								bodyPane.setCaretPosition(pos+1);
							}
						} catch(BadLocationException e) {}
						
						underlineRemoved = true;
					}
					
					// And set the assumption button to clicked
					inAssump = true;
					assumpButton.setSelected(true);
				} else {
					//System.out.println("Caret updated, but not inside an assumption block.");
					inAssump = false;
					assumpButton.setSelected(false);
				}
			}
		};
		SwingUtilities.invokeLater(processCaret);
	}
	
	// If a new JTextField is clicked into, un-click assumpButton
	public void focusGained(FocusEvent e) {
		if (e.getSource() != bodyPane) {
			assumpButton.setSelected(false);
		}
		
		// Inherited from KeyablePanel
		try {
			focused = (JTextComponent) e.getSource();
		} catch(ClassCastException ex) {}
	}
}