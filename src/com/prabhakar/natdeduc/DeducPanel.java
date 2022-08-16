package com.prabhakar.natdeduc;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class DeducPanel extends KeyablePanel implements ActionListener {
	JButton deducButton;
	
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
		
		bruteForce(premProps, concProp, atomicProps);
		
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
		int added = 0;
		Proposition[] body = new Proposition[maxBody];
		
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
		
		for (int line=0; line<maxBody; line++) {
			nextLine(premProps, newAtomics, concProp, 1);
			System.exit(0);
		}
		
		Proposition pholder = new Proposition('p');
		Proposition[] placeholder = {pholder};
		Deduction d = new Deduction(premProps, placeholder, concProp, mainFrame);
		return d;
	}
	
	// Used (recursively) by bruteForce as the engine
	private void nextLine(Proposition[] bank, Proposition[] atomics, Proposition concProp, int depth) {
		//for (int p=0; p<atomics.length; p++) {
		//	System.out.println(atomics[p].name);
		//}
		
		
		for (long tries=0; tries<itsToLevel(10, atomics); tries++) {
			Proposition newProp = seedProp(tries, atomics);
			
			// Check if newProp follows from anything thus far
			int[] results = propFromBank(bank, newProp);
			if (results[0] > 0) {
				newProp.configName();
				System.out.println(newProp.name);
				if (newProp.sameAs(concProp)) {
					// Conclusion reached
					String message = "Deduction found!\n\n" + deducToString(bank, newProp);
					JOptionPane.showMessageDialog(mainFrame, message, "Deduction found", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				
				// Try pursuing this prop; if we get nowhere by a certain limit, let the next prop try
				if (depth<20) {
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
						Thread.sleep(1);
					} catch(Exception e) {}
					nextLine(newBank, newAtomics, concProp, depth+1);
				}
				//System.out.println("Trying new branch");
			}
		}
	}
	
	// Generate one of the every-proposition-possible from a number, such that every proposition
	// is uniquely generated by some seed.
	private Proposition seedProp(long seed, Proposition[] atomics) {
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
		
		for (int level=1; level<5; level++) {
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
								return new Proposition(operations[op], seedProp(basicProp, atomics), "Deduction");
							}
							
							levelIteration++;
						}
					} else {
						for (int basicProp1=0; basicProp1<numAtomics; basicProp1++) {
							for (int basicProp2=0; basicProp2<numAtomics; basicProp2++) {
								if (levelIteration==seed) {
									// Form the proposition
									return new Proposition(seedProp(basicProp1, atomics), operations[op], seedProp(basicProp2, atomics), "Deduction");
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
		
		sum += (sum*onePlaces) + (sum*sum*twoPlaces);
		
		return sum;
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
		if (Deduction.reiteration(inP, outP)) return REITERATION;
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
	
	// Return a non-repeating array of all the immediate components in each given prop
	private ArrayList<Proposition> splitProps(Proposition[] inPs) {
		ArrayList<Proposition> molecules = new ArrayList<Proposition>();
		boolean contains;
		
		for (int p=0; p<inPs.length; p++) {
			if (inPs[p].places >= 1) {
				contains = false;
				for (int m=0; m<molecules.size(); m++) {
					if (inPs[p].place1.sameAs(molecules.get(m))) contains = true;
				}
				if (!contains) {
					molecules.add(inPs[p].place1);
				}
			} 
			if (inPs[p].places == 2) {
				contains = false;
				for (int m=0; m<molecules.size(); m++) {
					if (inPs[p].place2.sameAs(molecules.get(m))) contains = true;
				}
				if (!contains) {
					molecules.add(inPs[p].place2);
				}
			}
		}
		
		return molecules;
	}
	
	// Write a deduction as a plain string, each line on its own line
	private String deducToString(Proposition[] bank, Proposition conc) {
		String deduc = "";
		
		for (int p=0; p<bank.length; p++) {
			if (bank[p].name == "Deduction") bank[p].configName();
			deduc += bank[p].name;
			deduc += "\n";
		}
		
		conc.configName();
		deduc += conc.name;
		
		return deduc;
	}
	
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
			case "Deduce":
				deduce();
				break;
			case "Add premise":
				incrPrems();
				break;
		}
	}
}