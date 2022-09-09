/* An object for a natural deduction - an attempted one, i.e. having a Deduction
 *  object is no guarantee of validity
 * 
 * Inference rules implemented:
 * 	- Introduction + elimination for conjunction, disjunction, conditional, biconditional, and negation
 * 	- Ex Falso/explosion and double negation elimination
 * 	- Tertium non datur
 * 	- Reiteration
 * 	- Disjunctive syllogism
 * 	- Modus tollens
 * 	- De Morgan rules
 */

package com.prabhakar.natdeduc;

import java.util.Arrays;

import javax.swing.JOptionPane;

public class Deduction {
	Proposition[] prems, body;
	Proposition conc;
	Proposition lastPrem;
	int numPrems, numBody;
	Main mainFrame;
	
	public Deduction(Proposition[] p, Proposition[] b, Proposition c, Main m) {
		prems = p;
		body = b;
		conc = c;
		numPrems = p.length;
		numBody = b.length;
		lastPrem = p[numPrems-1];
		mainFrame = m;
	}
	
	public boolean follows(Proposition p, int established, Deduction[] assumpBlocks) {
		// Form an array of Propositions we can presently use as resources, i.e. prems + established
		int numAssumps = assumpBlocks.length;
		int numRes = numPrems + established;
		Proposition[] resources = new Proposition[numRes];
		for (int pr=0; pr<numPrems; pr++) {
			resources[pr] = prems[pr];
		}
		for (int bd=0; bd<established; bd++) {
			resources[numPrems+bd] = body[bd];
		}
		
		
		// Use brute force with an element of intelligence
		// Rules which might + might not yield an atomic have to be anticipated before the switch
		
		// Conjunction elimination
		for (int i=0; i<numRes; i++) {
			if (conjElim(resources[i], p)) return true;
		}
		// Disjunction elimination
		for (int i=0; i<numRes; i++) {
			for (int j=0; j<numAssumps; j++) {
				for (int k=0; k<numAssumps; k++) {
					if (disjElim(resources[i], assumpBlocks[j], assumpBlocks[k], p)) return true;
				}
			}
		}
		// Conditional elimination
		for (int i=0; i<numRes; i++) {
			for (int j=0; j<numRes; j++) {
				if (condElim(resources[i], resources[j], p)) return true;
			}
		}
		// Biconditional elimination
		for (int i=0; i<numRes; i++) {
			for (int j=0; j<numRes; j++) {
				if (bicondElim(resources[i], resources[j], p)) return true;
			}
		}
		// Negation elimination
		for (int i=0; i<numRes; i++) {
			for (int j=0; j<numRes; j++) {
				if (negElim(resources[i], resources[j], p)) return true;
			}
		}
		// Ex falso
		for (int i=0; i<numRes; i++) {
			if (exFalso(resources[i], p)) return true;
		}
		// Tertium non datur
		for (int i=0; i<numAssumps; i++) {
			for (int j=0; j<numAssumps; j++) {
				if (tertNonDat(assumpBlocks[i], assumpBlocks[j], p)) return true;
			}
		}
		// Double negation elimination
		for (int i=0; i<numRes; i++) {
			if (doubleNegElim(resources[i], p)) return true;
		}
		// Disjunctive syllogism
		for (int i=0; i<numRes; i++) {
			for (int j=0; j<numRes; j++) {
				if (disjSyl(resources[i], resources[j], p)) return true;
			}
		}
		// Modus tollens
		for (int i=0; i<numRes; i++) {
			for (int j=0; j<numRes; j++) {
				if (modusToll(resources[i], resources[j], p)) return true;
			}
		}
		// De Morgan rules
		for (int i=0; i<numRes; i++) {
				if (deMorg(resources[i], p)) return true;
		}
		// Reiteration
		// This comes last to keep it from overriding any more-substantive inference
				for (int i=0; i<numRes; i++) {
					if (reiteration(resources[i], p)) return true;
				}
		
		// Rules which certainly won't yield an atomic proposition
		switch (p.operation) {
			case '∧':
				// Conjunction introduction
				for (int i=0; i<numRes; i++) {
					for (int j=0; j<numRes; j++) {
						if (conjIntro(resources[i], resources[j], p)) return true;
					}
				}
				break;
				
			case '∨':
				// Disjunction introduction
				for (int i=0; i<numRes; i++) {
					if (disjIntro(resources[i], p)) return true;
				}
				break;
				
			case '→':
				// Conditional introduction
				for (int i=0; i<numAssumps; i++) {
					if (condIntro(assumpBlocks[i], p)) return true;
				}
				break;
				
			case '↔':
				// Biconditional introduction
				for (int i=0; i<numRes; i++) {
					for (int j=0; j<numRes; j++) {
						if (bicondIntro(resources[i], resources[j], p)) return true;
					}
				}
				break;
				
			case '¬':
				// Negation introduction
				for (int i=0; i<numAssumps; i++) {
					if (negIntro(assumpBlocks[i], p)) return true;
				}
				break;
		}
		
		
		return false;
	}
	
	// Check the validity of the object
	public boolean check() {
		// Handle assumption blocks iteratively
		// First, count how many we have
		int numAssumps = 0;
		for (int p=0; p<numBody; p++) {
			if (body[p].operation=='@' || body[p].operation=='£') {
				char assumpChar = body[p].operation;
				numAssumps++;
				while (body[p].operation == assumpChar) {
					p++;
					if (p==numBody) break;
				}
			}
		}
		
		// Copy the assumption blocks, now counted, into assumpBlocks
		Deduction[] assumpBlocks = new Deduction[numAssumps];
		int d = 0;
		int p = 0;
		
		while (p<numBody) {
			int blockLen = 0;
			if (body[p].operation=='@' || body[p].operation=='£') {
				char assumpChar = body[p].operation;
				while (body[p].operation == assumpChar) {
					blockLen++;
					p++;
					if (p==numBody) break;
				}
				if (blockLen<2) {
					JOptionPane.showMessageDialog(mainFrame, "An assumption block must be at least two lines.", "Syntax error in body", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				
				Proposition assumpConc = body[p-1].place1;
				Proposition[] assumpBody = new Proposition[blockLen-2];
				for (int q=0; q<blockLen-2; q++) {
					assumpBody[q] = body[p-blockLen+q+1].place1;
				}
				
				// Add the overall premises to those the assumption can use
				Proposition[] assumpPrems = new Proposition[numPrems+1];
				for (int pr=0; pr<numPrems; pr++) {
					assumpPrems[pr] = prems[pr];
				}
				assumpPrems[numPrems] = body[p-blockLen].place1;
				
				assumpBlocks[d] = new Deduction(assumpPrems, assumpBody, assumpConc, mainFrame);
				if (!assumpBlocks[d].check()) return false;
				d++;
			} else {
				p++;
			}
		}
		
		// Now that we get to this point, anything within assumption blocks is valid
		for (p=0; p<numBody; p++) {
			if (!body[p].atomic && (body[p].operation=='@' || body[p].operation=='£')) continue;
			if (!follows(body[p], p, assumpBlocks)) {
				body[p].configName();
				JOptionPane.showMessageDialog(mainFrame, "Proposition " + body[p].name + " does not follow.", "Logical error in body", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		
		if (!follows(conc, numBody, assumpBlocks)) {
			conc.configName();
			JOptionPane.showMessageDialog(mainFrame, "Proposition " + conc.name + " does not follow.", "Logical error in conclusion", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		return true;
	}
	
	// The following methods check whether one/two proposition/s entail a third via a specific rule
	// Where there are multiple input Propositions, they are order-sensitive (this
	// shifts the extra work to the brute forcer)
	public static boolean conjIntro(Proposition inP1, Proposition inP2, Proposition outP) {
		if (outP.places==2) {
			if (outP.place1.sameAs(inP1)) {
				if (outP.place2.sameAs(inP2)) {
					if (outP.operation=='∧') {
						return true;
					}
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean conjElim(Proposition inP, Proposition outP) {
		if (inP.places==2) {
			if (inP.operation=='∧') {
				if (inP.place1.sameAs(outP) || inP.place2.sameAs(outP)) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean disjIntro(Proposition inP, Proposition outP) {
		if (outP.places==2) {
			if (outP.operation=='∨') {
				if (outP.place1.sameAs(inP) || outP.place2.sameAs(inP)) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	// With the rules that use assumption blocks, the block must be given as a deduction,
		// meaning the work to make sure we really do have an assumption block to try must be done 
		// by check() when deciding which rules to brute-force
	public static boolean disjElim(Proposition inP, Deduction inD1, Deduction inD2, Proposition outP) {
		if (inP.places==2 && inP.operation=='∨') {
			if (inD1.lastPrem.sameAs(inP.place1) && inD2.lastPrem.sameAs(inP.place2)) {
				if (inD1.conc.sameAs(inD2.conc)) {
					if (inD2.conc.sameAs(outP)) {
						return true;
					}
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean condIntro(Deduction inD, Proposition outP) {
		if (outP.places==2) {
			if (outP.operation=='→') {
				if (inD.lastPrem.sameAs(outP.place1) && inD.conc.sameAs(outP.place2)) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	
	public static boolean condElim(Proposition inP1, Proposition inP2, Proposition outP) {
		if (inP1.places==2) {
			if (inP1.operation=='→') {
				if (inP1.place1.sameAs(inP2)) {
					if (inP1.place2.sameAs(outP)) {
						return true;
					}
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean bicondIntro(Proposition inP1, Proposition inP2, Proposition outP) {
		if (inP1.places==2 && inP2.places==2) {
			if (inP1.operation=='→' && inP2.operation=='→') {
				if (inP1.place1.sameAs(inP2.place2) && inP1.place2.sameAs(inP2.place1)) {
					if (outP.places==2 && outP.operation=='↔') {
						if (inP1.place1.sameAs(outP.place1) && inP2.place1.sameAs(outP.place2)) {
							return true;
						}
					}
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean bicondElim(Proposition inP1, Proposition inP2, Proposition outP) {
		if (inP1.places==2) {
			if (inP1.operation=='↔') {
				if (inP1.place1.sameAs(inP2) && inP1.place2.sameAs(outP)) {
					return true;
				}
				if (inP1.place2.sameAs(inP2) && inP1.place1.sameAs(outP)) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean negIntro(Deduction inD, Proposition outP) {
		if (outP.places==1) {
			if (outP.operation=='¬') {
				if (inD.lastPrem.sameAs(outP.place1) && inD.conc.name.equals("⊥")) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean negElim(Proposition inP1, Proposition inP2, Proposition outP) {
		if (inP2.places==1) {
			if (inP2.place1.sameAs(inP1) && inP2.operation=='¬') {
				if (outP.name.equals("⊥")) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean exFalso(Proposition inP, Proposition outP) {
		if (inP.name.equals("⊥")) {
			return true;
		}
		
		
		return false;
	}
	
	public static boolean doubleNegElim(Proposition inP, Proposition outP) {
		if (inP.places==1 && inP.operation=='¬') {
			if (inP.place1.places==1 && inP.place1.operation=='¬') {
				if (inP.place1.place1.sameAs(outP)) {
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean reiteration(Proposition inP, Proposition outP) {
		if (inP.sameAs(outP)) {
			return true;
		}
		
		
		return false;
	}
	
	public static boolean disjSyl(Proposition inP1, Proposition inP2, Proposition outP) {
		if (inP1.places==2) {
			if (inP1.operation=='∨') {
				if (inP2.places==1) {
					if (inP2.operation=='¬') {
						if (inP2.place1.sameAs(inP1.place1) && outP.sameAs(inP1.place2)) {
							return true;
						}
						if (inP2.place1.sameAs(inP1.place2) && outP.sameAs(inP1.place1)) {
							return true;
						}
					}
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean modusToll(Proposition inP1, Proposition inP2, Proposition outP) {
		if (inP1.places==2 && inP1.operation=='→') {
			if (inP2.places==1 && inP2.operation=='¬') {
				if (inP2.place1.sameAs(inP1.place2)) {
					if (outP.places==1 && outP.operation=='¬') {
						if (outP.place1.sameAs(inP1.place1)) {
							return true;
						}
					}
				}
			}
		}
		
		
		return false;
	}
	
	public static boolean tertNonDat(Deduction inD1, Deduction inD2, Proposition outP) {
		if (inD1.lastPrem.places==1 && inD1.lastPrem.operation=='¬'){
			if (inD1.lastPrem.place1.sameAs(inD2.lastPrem)) {
				if (inD1.conc.sameAs(inD2.conc)) {
					if (inD2.conc.sameAs(outP)) {
						return true;
					}
				}
			}
		}
		
		if (inD2.lastPrem.places==1 && inD2.lastPrem.operation=='¬'){
			System.out.println("a");
			if (inD2.lastPrem.place1.sameAs(inD1.lastPrem)) {
				System.out.println("b");
				if (inD1.conc.sameAs(inD2.conc)) {
					System.out.println("c");
					if (inD2.conc.sameAs(outP)) {
						return true;
					}
				}
			}
		}
		
		
		return false;
	}

	public static boolean deMorg(Proposition inP, Proposition outP) {
		if (inP.places==1 && inP.operation=='¬') {
			if (inP.place1.places==2) {
				if (inP.place1.operation=='∨') {
					if (outP.places==2 && outP.operation=='∧') {
						if (outP.place1.places==1 && outP.place1.operation=='¬') {
							if (outP.place2.places==1 && outP.place2.operation=='¬') {
								if (outP.place1.place1.sameAs(inP.place1.place1) &&
										outP.place2.place1.sameAs(inP.place1.place2)) {
									return true;
								}
							}
						}
					}
				}
				if (inP.place1.operation=='∧') {
					if (outP.places==2 && outP.operation=='∨') {
						if (outP.place1.places==1 && outP.place1.operation=='¬') {
							if (outP.place2.places==1 && outP.place2.operation=='¬') {
								if (outP.place1.place1.sameAs(inP.place1.place1) &&
										outP.place2.place1.sameAs(inP.place1.place2)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		
		if (inP.places==2 && inP.operation=='∨') {
			if (inP.place1.places==1 && inP.place1.operation=='¬') {
				if (inP.place2.places==1 && inP.place2.operation=='¬') {
					if (outP.places==1 && outP.operation=='¬') {
						if (outP.place1.places==2 && outP.place1.operation=='∧') {
							if (outP.place1.place1.sameAs(inP.place1.place1) &&
									outP.place1.place2.sameAs(inP.place2.place1)) {
								return true;
							}
						}
					}
				}
			}
		}
		
		if (inP.places==2 && inP.operation=='∧') {
			if (inP.place1.places==1 && inP.place1.operation=='¬') {
				if (inP.place2.places==1 && inP.place2.operation=='¬') {
					if (outP.places==1 && outP.operation=='¬') {
						if (outP.place1.places==2 && outP.place1.operation=='∨') {
							if (outP.place1.place1.sameAs(inP.place1.place1) &&
									outP.place1.place2.sameAs(inP.place2.place1)) {
								return true;
							}
						}
					}
				}
			}
		}
		
		
		return false;
	}
}