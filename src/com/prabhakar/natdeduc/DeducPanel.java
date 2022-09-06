package com.prabhakar.natdeduc;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class DeducPanel extends KeyablePanel implements ActionListener {
	final int MAXDEPTH = 4;
	SwingWorker<Boolean, Void> sw;
	
	JButton deducButton;
	JFrame deducProgress;
	JPanel dPane;
	JLabel[] dLabels;
	JButton cancelButton;
	JProgressBar progBar;
	
	final char[] operations = {'¬', '∧', '∨', '→', '↔'};
	final int[] opPlaces = {1, 2, 2, 2, 2};									// ¬, ∧, ∨, →, ↔
	int onePlaces = 0;
	int twoPlaces = 0;
	
	final int CONJINTRO = 1;
	final int CONJELIM = 2;
	final int DISJINTRO = 3;
	final int DISJELIM = 4;
	final int CONDINTRO = 5;
	final int CONDELIM = 6;
	final int BICONDINTRO = 7;
	final int BICONDELIM = 8;
	final int NEGINTRO = 9;
	final int NEGELIM = 10;
	final int EXFALSO = 11;
	final int DOUBLENEGELIM = 12;
	final int REITERATION = 13;
	final int DISJSYL = 14;
	final int MODUSTOLL = 15;
	final int TERTNONDAT = 16;
	final int DEMORGAN = 17;
	String[] ruleNames = {"doesn't follow", "conjunction introduction", "conjunction elimination", "disjunction introduction", "disjunction elimination", "conditional introduction", "conditional elimination", "biconditional introduction", "biconditional elimination", "negation introduction", "negation elinination", "ex falso", "double negation elimination", "reiteration", "disjunctive syllogism", "modus tollens", "tertium non datur", "De Morgan rules"};
	
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
		
		// Configure operations
		for (int o=0; o<operations.length; o++) {
			if (opPlaces[o] == 1) {
				this.onePlaces++;
			} else {
				this.twoPlaces++;
			}
		}
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
	
	private boolean deduce() {
		/* Check the syntax of the task we've been given */
		// Beginning with the premises
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
		
		// And then the conclusion
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
		
		
		/* At this point we certainly have syntactically valid premise/s and conclusion 
		   Now we brute force the deduction */
		// Get a list of atomic propositions involved
		boolean[] atomics = new boolean[26];
		
		Proposition traceProp;
		for (int p=0; p<numPrems; p++) {
			atomics = premProps[p].mergeBoolArrays(atomics, premProps[p].countAtomic());
		}
		
		// Make sure the conclusion doesn't use any new atomic props
		boolean[] concAtomics = concProp.countAtomic();
		int numAtomics = 0;
		for (int a=0; a<26; a++) {
			if (concAtomics[a] && !atomics[a]) {
				JOptionPane.showMessageDialog(mainFrame, "Conclusion uses atomic proposition " + Character.toString((char)('a' + a)) + " not found in premises.", "Deduction error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			
			if (atomics[a]) numAtomics++;
		}
		
		// Convert to an array of Propositions, so that we can pretend complex props are atomics
		Proposition[] atomicProps = new Proposition[numAtomics];
		int c = 0;
		
		for (int a=0; a<26; a++) {
			if (atomics[a]) {
				atomicProps[c] = new Proposition((char) ('a' + a));
				c++;
			}
		}
		
		// Finally, set up a window to show ongoing progress
		this.deducProgress = new JFrame("Searching...");
		this.deducProgress.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		this.dPane = new JPanel();
		GridLayout grid = new GridLayout(MAXDEPTH+2, 1);
		grid.setHgap(10);
		this.dPane.setLayout(grid);
		this.deducProgress.add(this.dPane);
		this.deducProgress.setVisible(true);
		
		this.dLabels = new JLabel[MAXDEPTH];
		for (int d=0; d<MAXDEPTH; d++) {
			this.dLabels[d] = new JLabel("");
			this.dLabels[d].setPreferredSize(new Dimension(300, 1));
			this.dPane.add(this.dLabels[d]);
		}
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.addActionListener(this);
		this.dPane.add(cancelButton);
		
		this.deducProgress.pack();
		this.deducProgress.setLocationRelativeTo(mainFrame);
		
		// And show the result
		Deduction finalDeduc = bruteForce(premProps, concProp, atomicProps);
		this.dPane.removeAll();
		this.deducProgress.dispose();
		
		if (finalDeduc != null) {
			// Show deduction in dialogue box
			String message = "Deduction found!\n\n" + deducToString(finalDeduc);
			JOptionPane.showMessageDialog(mainFrame, message, "Deduction found", JOptionPane.INFORMATION_MESSAGE);
		} else {
			// Exit silently if the search was cancelled, else declare nothing found
			if (sw.isCancelled()) return false;
			String message = "No deduction found up to depth of " + Integer.toString(MAXDEPTH) + ".\n";
			JOptionPane.showMessageDialog(mainFrame, message, "Deduction not found", JOptionPane.INFORMATION_MESSAGE);
		}
		
		return true;
	}
	
	/* The basic idea is to generate every possible Proposition (i.e. the object, not just
	 * strings of symbols) from the ground up, using every possible component - that is, all
	 * the atomic propositions, contradiction, and every operation, in the patterns that make
	 * sense - and hope to eventually stumble upon a deduction. After a point, when our propositions
	 * get longer than a certain limit or more numerous than another limit, we give up. */
	Deduction bruteForce(Proposition[] premProps, Proposition concProp, Proposition[] atomics) {
		// Check if the conclusion follows directly from the premises, just in case
		int rule;
		for (int p=0; p<premProps.length; p++) {
			rule = propFromProp(premProps[p], concProp);
			if (rule>0) {
				String message = "Premise " + Integer.toString(p+1) + " already entails the conclusion by " + ruleNames[rule] + ".";
				JOptionPane.showMessageDialog(mainFrame, message, "No working required", JOptionPane.INFORMATION_MESSAGE);
				return null;
			}
			
			for (int q=0; q<premProps.length; q++) {
				if (q!=p) {
					rule = propFromProp(premProps[p], premProps[q], concProp);
					if (rule>0) {
						String message = "Premises " + Integer.toString(p+1) + " and " + Integer.toString(q+1) + " already entail the conclusion by " + ruleNames[rule] + ".";
						JOptionPane.showMessageDialog(mainFrame, message, "No working required", JOptionPane.INFORMATION_MESSAGE);
						return null;
					}
				}
			}
		}
		
		/* A first attempt: pure brute force */
		int maxBody = 20;
		
		// From here we will treat our atomic props as the premises, plus their immediate components,
		// so that seedProp() gives anything which can be derived from the prems in one step
		ArrayList<Proposition> extraAtomics = splitProps(premProps);
		Proposition[] newAtomics = new Proposition[premProps.length + extraAtomics.size()];
		
		int p;
		for (p=0; p<premProps.length; p++) {
			newAtomics[p] = premProps[p];
		}
		for (int q=0; q<extraAtomics.size(); q++) {
			newAtomics[p] = extraAtomics.get(q);
			p++;
		}
		
		// Create a Deduction object
		Proposition[] newerAtomics = removeDuplicates(newAtomics);
		this.progBar = new JProgressBar(0, 100);
		this.dPane.add(progBar);
		
		ArrayList<Proposition> newLines = this.nextLine(premProps, newerAtomics, concProp, 1, new ArrayList<Proposition>());
		
		if (newLines != null) {
			return new Deduction(premProps, (Proposition[])newLines.toArray(new Proposition[0]), concProp, mainFrame);
		} else {
			return null;
		}
	}
	
	// Configure the text being updated in the "searching..." window in a method of its own
	String[] progressStr = new String[MAXDEPTH];
	
	private void progressText(String propName, int depth) {
		int s;
		progressStr[depth-1] = "Trying: " + propName;
		
		// Clear the rest of the array (if we're in a new branch)
		for (s=depth; s<MAXDEPTH; s++) {
			progressStr[s] = null;
		}
		
		for (s=0; s<MAXDEPTH; s++) {
			this.dLabels[s].setText((progressStr[s]==null) ? "" : progressStr[s]);
		}
		
		this.deducProgress.revalidate();
		this.deducProgress.repaint();
	}
	
	// Used (recursively) by bruteForce as the engine; returns an ArrayList of all the added lines
	// bank holds all propositions which are currently established, including prems, while nLines
	// keeps track of those which were added by some iteration of this method since its initial call
	// bank is used for testing prospective new lines, nLines for returning
	int progress = 0;
	float step1;
	float step2;
	
	private ArrayList<Proposition> nextLine(Proposition[] bank, Proposition[] atomics, Proposition concProp, int depth, ArrayList<Proposition> nLines) {
		ArrayList<Proposition> newLines;
		
		for (long tries=0; tries<itsToLevel(1, atomics); tries++) {
			// Check the whole deduction-search hasn't been cancelled
			if (sw.isCancelled()) return null;
			
			// Progress is calculated from the progress through the first two layers of depth. We
			// can't calculate how many props will be tried before trying them, because we don't
			// know which attempts will follow from bank, and therefore how many attempts will be
			// pursued to a deeper layer. This is an okay heuristic.
			// itsToLevel(1, atomics) returns different results in the two lines below
			this.progBar.setValue(progress);
			if (depth == 1) {
				step1 = 100 / itsToLevel(1, atomics);
				progress = (int)(tries * step1);
			}
			if (depth == 2) {
				step2 = step1 / itsToLevel(1, atomics);
				progress += (int)(tries * step2);
			}
			
			Proposition newProp = seedProp(tries, atomics);
			newLines = nLines;
			
			// Check if newProp follows from anything thus far
			int[] results = propFromBank(bank, newProp);
			if (results[0] > 0 && !newProp.sameAs(bank[bank.length-1])) {
				// At this point we have a proposition of depth depth which follows from bank
				newLines.add(newProp);
				progressText(newProp.name, depth);
				
				if (newProp.sameAs(concProp)) {
					// Conclusion reached!
					//Replace any escape characters in complex props with the props they represent
					for (int p=0; p<bank.length; p++) {
						bank[p].configName();
					}
					
					return newLines;
				}
				
				// Try pursuing this prop; if we get nowhere by a certain limit, let the next prop try
				if (depth<MAXDEPTH) {
					// Update the two parameters
					Proposition[] newBank = new Proposition[bank.length+1];
					for (int p=0; p<bank.length; p++) {
						newBank[p] = bank[p];
					}
					newBank[bank.length] = newProp;
					
					Proposition[] newPropArr = {newProp};
					ArrayList<Proposition> extraAtomics = splitProps(newPropArr);
					Proposition[] newAtomics = new Proposition[atomics.length + extraAtomics.size()];
					
					int p;
					for (p=0; p<atomics.length; p++) {
						newAtomics[p] = atomics[p];
					}
					for (int q=0; q<extraAtomics.size(); q++) {
						newAtomics[p] = extraAtomics.get(q);
						p++;
					}
					
					try {
						Thread.sleep(10);
					} catch(Exception e) {}
					
					
					ArrayList<Proposition> result = this.nextLine(newBank, removeDuplicates(newAtomics), concProp, depth+1, newLines);
					if (result != null) return result;
				}
			}
		}
		
		return null;
	}
	
	// Generate one of the every-proposition-possible from a number, such that every proposition
	// is uniquely generated by some seed.
	private Proposition seedProp(long seed, Proposition[] atomics) {
		Proposition result;
		long numAtomics = atomics.length;
		int numOperations = opPlaces.length;
		
		// The first n Propositions are just the atomics, in alphabetical order
		if (seed<numAtomics) {
			return atomics[(int)seed];
		}
		seed -= numAtomics;
		
		// After the atomics, we build props which make use of one layer of atomic/s and one
		// operation, and then to props which bear the same relation to these, and so on.
		// The number of ways that n atomic propositions can be arranged into props of the next
		// level up is: n*(number of 1-place operations) + n^2*(number of 2-place operations)
		// This is the same all the way up, the only difference being that the number of basic
		// props *accumulates*, since an operation may have one place as an atomic prop, and its
		// other place as a 10th-level prop. 
		// In addition we need a 
		// count of the number of propositions just at the current level, for use in interpreting
		// the seed.
		
		// At each level, numAtomics is the number of props at the level just previous, and 
		// levelProps, the number at the current level
		
		for (int level=1; level<MAXDEPTH; level++) {
			long levelProps = (numAtomics * onePlaces) + (numAtomics*numAtomics * twoPlaces);
			if (seed<levelProps) {
				// Then the prop is somewhere on this level, so pinpoint it
				// The ordering is:
				// op1 prop1
				// op1 prop2
				// ...
				// op2 prop1 prop1
				// op2 prop1 prop2
				// ...
				int levelIteration = 0;
				
				for (int op=0; op<numOperations; op++) {
					// How we iterate here depends on whether the operation is 1- or 2-place
					if (opPlaces[op]==1) {
						for (int basicProp=0; basicProp<numAtomics; basicProp++) {
							if (levelIteration==seed) {
								// Form the proposition
								result = new Proposition(operations[op], seedProp(basicProp, atomics), "Deduction");
								result.configName();
								return result;
							}
							
							levelIteration++;
						}
					} else {
						for (int basicProp1=0; basicProp1<numAtomics; basicProp1++) {
							for (int basicProp2=0; basicProp2<numAtomics; basicProp2++) {
								if (levelIteration==seed) {
									// Form the proposition
									result = new Proposition(seedProp(basicProp1, atomics), operations[op], seedProp(basicProp2, atomics), "Deduction");
									result.configName();
									return result;
								}
								
								levelIteration++;
							}
						}
					}
				}
			}
			// Else go on to the next level
			
			numAtomics = levelProps;
		}
		
		
		return null;
	}
	
	// Return the number of iterations taken to reach the end of level n of propositions
	private long itsToLevel(int level, Proposition[] atomics) {
		int sum = 0;
		if (level>0) {
			sum += itsToLevel(level-1, atomics);
		}
		
		if (level==0) {
			return atomics.length;
		}
		
		return sum + (sum*onePlaces) + (sum*sum*twoPlaces);
	}
	
	// A function sufficiently different from Deduction.follows()
	// This one returns an int representing the inference rule by which outP follows from inP,
	// or 0 if it doesn't
	// The next function does the same for two input props
	private int propFromProp(Proposition inP, Proposition outP) {
		if (Deduction.conjElim(inP, outP)) return CONJELIM;
		if (Deduction.disjIntro(inP, outP)) return DISJINTRO;
		if (Deduction.exFalso(inP, outP)) return EXFALSO;
		if (Deduction.doubleNegElim(inP, outP)) return DOUBLENEGELIM;
		//if (Deduction.reiteration(inP, outP)) return REITERATION;						// We won't use reiteration here because no deduction strictly requires it
		if (Deduction.deMorg(inP, outP)) return DEMORGAN;
		
		return 0;
	}
	
	private int propFromProp(Proposition inP1, Proposition inP2, Proposition outP) {
		if (Deduction.conjIntro(inP1, inP2, outP)) return CONJINTRO;
		if (Deduction.condElim(inP1, inP2, outP)) return CONDELIM;
		if (Deduction.bicondIntro(inP1, inP2, outP)) return BICONDINTRO;
		if (Deduction.bicondElim(inP1, inP2, outP)) return BICONDELIM;
		if (Deduction.negElim(inP1, inP2, outP)) return NEGELIM;
		if (Deduction.disjSyl(inP1, inP2, outP)) return DISJSYL;
		if (Deduction.modusToll(inP1, inP2, outP)) return MODUSTOLL;
		
		return 0;
	}
	
	// Call the previous two methods to work out if outP follows from anything in bank
	// Return an array of [inference rule, index1, index2], all defaulting to zero
	private int[] propFromBank(Proposition[] bank, Proposition outP) {
		int[] results = {0, 0, 0};
		int rule;
		
		for (int p=0; p<bank.length; p++) {
			rule = propFromProp(bank[p], outP);
			if (rule>0) {
				results[0] = rule;
				results[1] = p;
				return results;
			}
			
			for (int q=0; q<bank.length; q++) {
				if (p!=q) {
					rule = propFromProp(bank[p], bank[q], outP);
					if (rule>0) {
						results[0] = rule;
						results[1] = p;
						results[2] = q;
						return results;
					}
				}
			}
		}
		
		return results;
	}
	
	// Return an ArrayList of all the immediate components in each given prop
	// Ensuring no repeats is done separately, after the return value is added to existing atomics
	private ArrayList<Proposition> splitProps(Proposition[] inPs) {
		ArrayList<Proposition> molecules = new ArrayList<Proposition>();
		
		for (int p=0; p<inPs.length; p++) {
			if (inPs[p].places >= 1) molecules.add(inPs[p].place1);
			
			if (inPs[p].places == 2) molecules.add(inPs[p].place2);
		}
		
		return molecules;
	}
	
	// Return a sized-down array of props with no duplicates
	private Proposition[] removeDuplicates(Proposition[] inPs) {
		ArrayList<Proposition> unique = new ArrayList<Proposition>();
		boolean contains;
		
		for (int p=0; p<inPs.length; p++) {
			contains = false;
			
			for (int q=0; q<unique.size(); q++) {
				if (inPs[p].sameAs(unique.get(q))) contains = true;
			}
			
			if (!contains) unique.add(inPs[p]);
		}
		
		return (Proposition[]) unique.toArray(new Proposition[0]);
	}
	
	// Write a deduction as a plain string, each line on its own line
	private String deducToString(Deduction deduc) {
		String deducStr = "";
		int i;
		
		// Premises
		for (i=0; i<deduc.numPrems; i++) {
			if (deduc.prems[i].name == "Deduction") deduc.prems[i].configName();
			deducStr += String.format("%1$6s", i+1) + ". ";
			deducStr += deduc.prems[i].name;
			deducStr += "\n";
		}
		
		// Body
		for (int j=0; j<deduc.numBody; i++, j++) {
			deducStr += String.format("%1$6s", i+1) + ". ";
			deducStr += deduc.body[j].name;
			deducStr += "\n";
		}
		
		// Conclusion
		deduc.conc.configName();
		deducStr += "Conc. ";
		deducStr += deduc.conc.name;
		
		return deducStr;
	}
	
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
			case "Deduce":
				// This may take time, so execute it outside the Event Dispatch Thread
				this.sw = new SwingWorker<Boolean, Void>(){
					@Override
					public Boolean doInBackground() {
						deduce();
						return true;
					}
				};
				sw.execute();
				break;
			case "Add premise":
				incrPrems();
				break;
			case "Cancel":
				sw.cancel(true);
		}
	}
}