// A new instance should be created for every new line being parsed, to avoid clogging up escapeProps

package com.prabhakar.natdeduc;

import java.util.*;

import javax.swing.JOptionPane;

public class Parser {
	HashMap<Character, Proposition> escapeProps = new HashMap<Character, Proposition>();
	final char assumpChar = (char) 12288;
	
	// Check if a line has legitimate bracketing (including none)
	public boolean validBrackets(String line) {
		char[] chs = line.toCharArray();
		Stack<Boolean> brackets = new Stack<Boolean>();
		
		for (int i=0; i<chs.length; i++) {
			switch (chs[i]) {
				case '(':
					brackets.push(true);
					break;
				case ')':
					if (brackets.empty()) return false;
					brackets.pop();
					break;
			}
		}
		
		
		return brackets.empty();
	}
	
	public String negBrackets(String inLine) {
		String outLine = "";
		
		char[] inChars = inLine.toCharArray();
		for (int i=0; i<inChars.length; i++) {
			if (inChars[i]=='¬') {
				if (i<inChars.length-1) {
					if (isAtomicProp(inChars[i+1])) {
						outLine += "(";
						outLine += Character.toString(inChars[i]);
						i++;
						outLine += Character.toString(inChars[i]);
						outLine += ")";
					} else {
						outLine += Character.toString(inChars[i]);
					}
				} else {
					outLine += Character.toString(inChars[i]);
				}
			} else {
				outLine += Character.toString(inChars[i]);
			}
		}
		
		
		return outLine;
	}
	
	// Find the positions of the inmost bracketed section/s of a line (only called by parseLine())
	private int[] findPeaks(int[] depth){
		ArrayList<Integer> peaks = new ArrayList<Integer>();
		for (int i=0; i<depth.length; i++) {
			//if (depth[i]>depth[i+1] && depth[i]>depth[i-1]) peaks.add(i);
			// Compare: the current depth, the previous one (excluding adjacent same values), and the next
			// (In other words, factor out stuff other than brackets)
			int curr = depth[i];
			int prev = curr, next = curr;
			if (i==0) prev = 0;
			for (int j=i; j>=0; j--) {
				if (depth[j]!=curr) {
					prev = depth[j];
					break;
				}
				if (j==0) prev = 0;
			}
			for (int k=i; k<depth.length; k++) {
				if (depth[k]!=curr) {
					next = depth[k];
					break;
				}
			}
			
			if (curr>prev && curr>next) peaks.add(i);
		}
		
		int[] result = new int[peaks.size()];
		for (int i=0; i<peaks.size(); i++) {
			result[i] = peaks.get(i);
		}
		
		
		return result;
	}
	
	public boolean isOnePlaceOperation(char ch) {
		switch (ch) {
			case '¬':
			case '~':
				return true;
		}
		
		
		return false;
	}
	
	public boolean isTwoPlaceOperation(char ch) {
		switch (ch) {
			case '∧':
			case '&':
			case '∨':
			case '→':
			case '↔':
				return true;
		}
		
		
		return false;
	}
	
	public boolean isAtomicProp(char ch) {
		if (escapeProps.containsKey(new Character(ch))) return true;
		if (Character.isUpperCase(ch)) return false;
		
		int c = Character.getNumericValue(ch);
		if (c>=Character.getNumericValue('p') && c<=Character.getNumericValue('t')) {
			return true;
		}
		
		
		return false;
	}
	
	public char nextEsc() {
		int code = 0x0100;
		while(escapeProps.containsKey((char)code)) {
			code++;
		}
		
		
		return (char) code;
	}
	
	// Take a String with escape codes and replace them with the propositions they represent
	public String parseEscCodes(String input) {
		char[] inChars = input.toCharArray();
		String output = "";
		
		for (char ch: inChars) {
			if (escapeProps.containsKey(ch)) {
				output += "(";
				output += parseEscCodes(escapeProps.get(ch).name);
				output += ")";
			} else {
				output += Character.toString(ch);
			}
		}
		
		
		return output;
	}
	
	// Return a Proposition object for the given basic segment - 
	// i.e. an operation on one or two atomic propositions
	// Complain if the bracketing that got us here was either insufficient or redundant
	// Only handles truth-functional operations for now
	public Proposition parseBasicProp(String s) throws ParseException{
		Proposition p;
		char[] chs = s.toCharArray();
		int len = chs.length;
		
		// Add proper error-handling to this
		if (len==0) {
			throw new ParseException("Don't leave blank space within brackets.");
		}
		
		if (len==1) {
			if (!Character.isLetter(chs[0])) {
				throw new ParseException(String.format("Symbol not recognised: '%c'.\n", chs[0]));
			}
			
			if (isAtomicProp(chs[0])) {
				return new Proposition(chs[0], escapeProps);
			} else if (Character.isUpperCase(chs[0])){
				throw new ParseException("Please use lower-case characters for all atomic propositions.");
			} else {
				throw new ParseException("Atomic propositions should be in the range 'p' to 't'");
			}
		}
		
		if (isOnePlaceOperation(chs[0])) {
			if (len>2) {
				throw new ParseException(String.format("Too many arguments for one-place operation '%c'.\n", chs[0]));
			} else if (len<2){
				throw new ParseException(String.format("Argument needed for one-place operation '%c'.\n", chs[0]));
			} else {
				if (!isAtomicProp(chs[1])) {
					throw new ParseException("Atomic propositions should be in the range 'p' to 't'");
				}
				Proposition place1 = new Proposition(chs[1], escapeProps);
				p = new Proposition(chs[0], place1, s);
				return p;
			}
		}
		
		if (len==2) {
			// If we've reached here, the first character of two isn't a one-place operation
			throw new ParseException(String.format("'%s' is not a proposition.\n", parseEscCodes(s)));
		}
		
		if (len==3) {
			if (isTwoPlaceOperation(chs[1])) {
				if (!isAtomicProp(chs[0])) {
					throw new ParseException("Atomic propositions should be in the range 'p' to 't'");
				}
				if (!isAtomicProp(chs[2])) {
					throw new ParseException("Atomic propositions should be in the range 'p' to 't'");
				}
				Proposition place1 = new Proposition(chs[0], escapeProps);
				Proposition place2 = new Proposition(chs[2], escapeProps);
				p = new Proposition(place1, chs[1], place2, s);
				return p;
			} else {
				throw new ParseException(String.format("'%c' is not a valid two-place operation.\n", chs[1]));
			}
		}
		
		// We reach here if len > 3
		throw new ParseException(String.format("Proposition '%s' too long; try breaking up with brackets.\n", parseEscCodes(s)));
	}
	
	// Return a Proposition object for a given line, in terms of atomic propositions and operations
	// Return null if the line is not a valid Proposition (establishing this is bound up with parsing it)
	// parseLine() itself handles the structure of the line, the rest being done by calling other methods
	// If line is not a valid proposition, exception throwing is handled within this method and
	// null is returned. Whatever method calls this one only has to detect the null and 
	// end its own thing. This is so that the exception can be printed differently depending
	// on where parseLine() was called from
	public Proposition parseLine(String line) throws ParseException {
		if (!validBrackets(line)) {
			throw new ParseException("Invalid bracketing.");
		}
		
		if (line.equals("⊥")) return new Proposition('⊥');
		
		// Remove all newspace because the below assumes none
		line = line.replace(" ", "");
		
		
		// Allow negations to be unbracketed by bracketing them here
		char[] chs = negBrackets(line).toCharArray();
		
		// Deal with assumption blocks
		if (chs[0]==assumpChar) {
			String assumed = line.substring(1);
			if (assumed==null) {
				throw new ParseException("Something's gone wrong with the assumption syntax.");
			} else {
				return parseLine(assumed).makeAssump();
			}
		}
		
		// Trace out where the brackets lie to know where to find the inmost propositions
		int[] depth = new int[chs.length];
		Arrays.fill(depth, 0);
		
		for (int i=0; i<chs.length; i++) {
			if (i==0) {
				if (chs[i]=='(') {
					depth[0] = 1;
				}
			} else if (chs[i]=='(') {
				depth[i] = depth[i-1] + 1;
			} else if(chs[i]==')') {
				depth[i] = depth[i-1] -1;
			} else {
				depth[i] = depth[i-1];
			}
		}
		
		// Identify the number of distinct peaks, since peaks[] stores every index including adjacent ones
		int[] peaks = findPeaks(depth);
		int numPeaks = (peaks.length==0) ? 0 : 1;
		for (int i=1; i<peaks.length; i++) {
			if (peaks[i]>peaks[i-1]+1) numPeaks++;
		}
		
		// Use numPeaks to find the inmost segments themselves (numPeaks=0 must be treated as a special case)
		String[] basic = new String[numPeaks];
		Arrays.fill(basic, "");
		int p = 0;
		for (int i=0; i<peaks.length; i++) {
			if (chs[peaks[i]]!='(') basic[p] += Character.toString(chs[peaks[i]]);
			
			// Check if we've reached the end of the current peak
			if (i<peaks.length-1) {
				if (peaks[i+1]>peaks[i]+1) {
					p++;
				}
			}
		}
		//System.out.println(Arrays.toString(basic));
		
		// Or if there are no brackets
		if (numPeaks==0) {
			try {
				return parseBasicProp(line);
			} catch(ParseException e) {
				throw e;
			}
		}
		
		// Construct Proposition objects from these inmost segments
		Proposition[] basicProps = new Proposition[numPeaks];
		for (int i=0; i<numPeaks; i++) {
			basicProps[i] = parseBasicProp(basic[i]);
		}
		
		// Now we just strip out the inmost propositions and call this method again 
		// on the next layer up, until the no-brackets scenario above is reached.
		// To do this we need a way of passing Proposition objects to parseBasicProp()
		// in place of atomic propositions. So we'll use escape codes, one char long.
		char[] escs = new char[numPeaks];
		for (int i=0; i<numPeaks; i++) {
			escs[i] = nextEsc();
			escapeProps.put(escs[i], basicProps[i]);
		}
		
		String lineNew = "";
		
		// The very painful process of copying out those parts of line which aren't in basic
		// j tracks where we are in peaks
		// k tracks which peak we are on
		int j = 0;
		int k = 0;
		boolean justFinished = false;
		for (int i=0; i<chs.length; i++) {
			justFinished = false;
			if (i>0 && j>0 && j<=peaks.length) {
				if (i!=peaks[j-1] && i-1==peaks[j-1]) {
					justFinished = true;
				}
			}
			
			if (j<peaks.length && i==peaks[j]) {
				j++;
			} else {
				if (justFinished) {
					lineNew += Character.toString(escs[k]);
					k++;
				} else {
					lineNew += Character.toString(chs[i]);
				}
			}
		}
		//System.out.println(line);
		//System.out.println(lineNew);
		
		
		try {
			return parseLine(lineNew);
		} catch(ParseException e) {
			throw e;
		}
	}
}