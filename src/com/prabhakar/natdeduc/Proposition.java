/* A class to be bulit up incrementally, such that any proposition comes down to
 * relations of atomic propositions, which have nothing 'to' them, just a name for reference.
 * The ways in which instances of Proposition relate captures all of 
 * what would have been articulated by brackets in a string.
 * Once a Proposition object has been produced, everything is grammatical.
 */

package com.prabhakar.natdeduc;

import java.util.*;

public class Proposition {
	boolean atomic;
	Proposition place1, place2;
	int places;
	char operation;
	String name;
	
	public Proposition(char n) {
		atomic = true;
		places = 0;
		name = Character.toString(n);
	}
	
	public Proposition(char n, Map<Character, Proposition> escapeProps) {
		if (escapeProps.containsKey(n)) {
			Proposition p = escapeProps.get(n);
			this.atomic = p.atomic;
			this.place1 = p.place1;
			this.place2 = p.place2;
			this.places = p.places;
			this.operation = p.operation;
			this.name = p.name;
		} else {
			atomic = true;
			places = 0;
			name = Character.toString(n);
		}
	}
	
	public Proposition(char o, Proposition p1, String n) {
		atomic = false;
		operation = o;
		place1 = p1;
		places = 1;
		name = n;
		
		if (operation=='~') operation = '¬';
	}
	
	public Proposition(Proposition p1, char o, Proposition p2, String n) {
		atomic = false;
		operation = o;
		place1 = p1;
		place2 = p2;
		places = 2;
		name = n;
		
		if (operation=='&') operation = '∧';
	}
	
	// Return an object which is the input, but within an assumption block
	public Proposition makeAssump() {
		return new Proposition('@', this, this.name);
	}
	
	public boolean sameAs(Proposition target) {
		if (this.atomic || target.atomic) {
			if (!this.atomic || !target.atomic) {
				return false;
			}
			
			if (this.name.equals(target.name)) {
				return true;
			}
			
			return false;
		}
		
		if (this.places==1) {
			if (target.places!=1) {
				return false;
			}
			
			if (this.operation==target.operation && this.place1.sameAs(target.place1)) {
				return true;
			}
			
			return false;
		}
		
		if (this.places==2) {
			if (target.places!=2) {
				return false;
			}
			
			if (this.operation==target.operation && 
					this.place1.sameAs(target.place1) && 
					this.place2.sameAs(target.place2)) {
				return true;
			}
		}
		
		
		return false;
	}
	
	public void print() {
		if (atomic) {
			System.out.println("Atomic proposition");
			System.out.printf("Name %s\n\n", name);
		}
		
		if (place1!=null && place2==null) {
			System.out.println("One-place proposition");
			System.out.printf("Operation %c\n", operation);
			System.out.printf("Place %s\n", place1.name);
			System.out.printf("Name %s\n\n", name);
		}
		
		if (place1!=null && place2!=null) {
			System.out.println("Two-place proposition");
			System.out.printf("Operation %c\n", operation);
			System.out.printf("Place1 %s\n", place1.name);
			System.out.printf("Place2 %s\n", place2.name);
			System.out.printf("Name %s\n\n", name);
		}
	}
}