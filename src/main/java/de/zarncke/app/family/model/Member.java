package de.zarncke.app.family.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import de.zarncke.lib.coll.L;

public class Member implements Comparable<Member>, Serializable {
	public enum Gender {
		MALE, FEMALE, UNKNOWN
	}

	public class Place implements Serializable {
		public String place;
	}

	private final String name;
	private final String lastName;
	private final Gender gender;
	private final LocalDate birth;
	private LocalDate death;
	private Place place;
	private List<Marriage> marriages;

	public Member(final String name, final String lastName, final Gender gender, final LocalDate birth) {
		this.name = name;
		this.lastName = lastName;
		this.gender = gender;
		this.birth = birth;
	}

	public void add(final Marriage marriage) {
		if (this.marriages == null) {
			this.marriages = new ArrayList<Marriage>(1);
		}
		this.marriages.add(marriage);
	}

	@Override
	public int compareTo(final Member o) {
		int bc = this.birth.compareTo(o.birth);
		if (bc != 0) {
			return bc;
		}
		bc = this.lastName.compareTo(o.lastName);
		if (bc != 0) {
			return bc;
		}
		return this.name.compareTo(o.name);
	}

	public void print(final PrintWriter ps) {
		ps.println(getName());
		ps.println("  born " + this.birth);
		if (this.marriages != null) {
			for (Marriage m : this.marriages) {
				ps.println("  married to -> " + m.other(this).getName());
				for (Member c : m.getChildren()) {
						ps.println("  -> " + c.getName());
					}
			}
		}
	}

	private String getName() {
		return this.name + " " + this.lastName;
	}

	@Override
	public String toString() {
		return getName() + " (" + this.birth + ")";
	}

	public Gender getGender() {
		return this.gender;
	}

	public LocalDate getBirth() {
		return this.birth;
	}

	public String getLastName() {
		return this.lastName;
	}

	public List<Marriage> getMarriages() {
		return this.marriages == null ? L.<Marriage> e() : this.marriages;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.birth == null ? 0 : this.birth.hashCode());
		result = prime * result + (this.lastName == null ? 0 : this.lastName.hashCode());
		result = prime * result + (this.name == null ? 0 : this.name.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Member other = (Member) obj;
		if (this.birth == null) {
			if (other.birth != null) {
				return false;
			}
		} else if (!this.birth.equals(other.birth)) {
			return false;
		}
		if (this.lastName == null) {
			if (other.lastName != null) {
				return false;
			}
		} else if (!this.lastName.equals(other.lastName)) {
			return false;
		}
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}
}