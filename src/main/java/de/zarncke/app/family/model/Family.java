package de.zarncke.app.family.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

public class Family implements Serializable {
	private final String name;
	private final SortedSet<Member> members = new TreeSet<Member>();

	public Family(final String name) {
		this.name = name;
	}

	public void print(final PrintWriter ps) {
		ps.println(this.name);
		ps.println(this.members.size() + " members");
		for (Member m : this.members) {
			m.print(ps);
		}
	}

	@Override
	public String toString() {
		return this.name + " " + this.members.size();
	}

	public void add(final Member anchestor) {
		this.members.add(anchestor);
	}

	public Collection<Member> getMembers() {
		return this.members;
	}
}