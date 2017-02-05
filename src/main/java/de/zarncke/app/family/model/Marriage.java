package de.zarncke.app.family.model;

import java.io.Serializable;
import java.util.SortedSet;
import java.util.TreeSet;

import org.joda.time.LocalDate;

import de.zarncke.app.family.model.Member.Gender;
import de.zarncke.lib.coll.L;

public class Marriage implements Serializable {
	private final LocalDate marriage;
	private LocalDate divorce;
	private Member husband;
	private Member wife;
	private SortedSet<Member> children;

	public Marriage(final Member one, final Member other, final LocalDate marriage) {
		if (one.getGender() == Gender.FEMALE) {
			this.wife = one;
			this.husband = other;
		} else {
			this.husband = one;
			this.wife = other;
		}
		this.marriage = marriage;
	}

	public Member other(final Member member) {
		return member == this.husband ? this.wife : this.husband;
	}

	public void add(final Member child) {
		if (this.children == null) {
			this.children = new TreeSet<Member>();
		}
		this.children.add(child);
	}

	@Override
	public String toString() {
		return this.husband + " oo " + this.wife + " (" + this.marriage + ")";
	}

	public SortedSet<Member> getChildren() {
		return this.children == null ? L.<Member> eset() : this.children;
	}

	public LocalDate getMarriage() {
		return this.marriage;
	}

	public LocalDate getDivorce() {
		return this.divorce;
	}

	public void setDivorce(final LocalDate divorce) {
		this.divorce = divorce;
	}

	public Member getHusband() {
		return this.husband;
	}

	public Member getWife() {
		return this.wife;
	}
}