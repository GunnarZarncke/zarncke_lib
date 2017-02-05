package de.zarncke.lib.cache;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import de.zarncke.app.family.model.Family;
import de.zarncke.app.family.model.Marriage;
import de.zarncke.app.family.model.Member;
import de.zarncke.app.family.model.Member.Gender;
import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.coll.L;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.region.Region;
import de.zarncke.lib.util.Misc;
import de.zarncke.lib.util.ObjectTool;

/**
 * This class contains methods to benchmark typical java operations.
 * Use a benchmarking framework to determine accurate running times of
 * these methods as follows:
 * 
 * <pre>
 * <code>
 * 	{
 * 		NoIoBenchmark bench = new NoIoBenchmark();
 * 		bench.setupStructure();
 * 		double averageObjectGraphAccessMs = time(performObjectGraphBenchmark());
 * 		double averageObjectGraphRestructureMs = time(performObjectGraphRestructureBenchmark());
 * 		double averageSerializationMs = time(performSerializationBenchmark());
 * 		double averageStringProcessingMs = time(performStringProcessingBenchmark());
 * 
 * 		double opsPerMs = getOperationsPerMs(averageObjectGraphAccessMs, averageObjectGraphRestructureMs,
 * 				averageSerializationMs, averageStringProcessingMs);
 * 
 * 		double averageYourCodeMs = time(yourCode());
 * 		double nominalOpsOfYOurCode = opsPerMs/averageYourCodeMs;
 * 	}
 * </code>
 * </pre>
 * 
 * This will give a measure of the complexity of your code which can the be plugged into optimization code. <br/>
 * The benchmark has the following properties:
 * <ul>
 * <li>Use data structure access (r/w)</li>
 * <li>Use ArrayList, HashMap, TreeMap</li>
 * <li>Use String processing</li>
 * <li>Use (de)serialization</li>
 * <li>Use deep recursion, nested loops, deep conditions</li>
 * <li>Use an object graph with many cross-links</li>
 * <li>Deterministic (the datastructure is random, but with fixed seed).</li>
 * <li>Intentionally no IO (neither disk nor net). This should be estimated separately.</li>
 * <li>Not yet: sychronize deep/shallow</li>
 * <li>Not yet: with/without lots of object creation</li>
 * </ul>
 * <br/>
 * If you do not want to benchmark your code you can use the following guidelines: <br/>
 * 1 BOP amounts to about
 * <ul>
 * <li>1 logical object with an object structure with 6 java objects (including arrays but not Strings) and about in toal 20
 * fields on average.</li>
 * <li>100 traversals of the structure e.g. for searching.</li>
 * <li>10 modifications of the structure.</li>
 * <li>3 string operation on this structure (for rendering it)</li>
 * <li>1 serialization and deserialization (as part of storage)</li>
 * </ul>
 * You may assume that these 4 types take comparable times.
 * So if you only traverse your object once you can savely assume <= 0.01 BOP.
 * If you store it somewhere you can guess 0.1 BOP (half of (de)serialization).
 * And if you restructure it and make 10 passes, then 0.5 BOP is probably right.
 * 
 * @author Gunnar Zarncke
 */
public class NoIoBenchmark {


	private static final int NUMBER_OF_TRACKED_MEMBERS = 20;

	private static final int NUMBER_OF_FAMILIES = 200;

	private static final int START_YEAR = 1500;

	private final Random random = new Random(3141592);

	private static final String[] NAMES_M = new String[] {//
	"Andrew", "Bob", "Charly", "Dan", "Erol", "Frank", //
			"Ian", "Mark", "Ned", "Oliver", "Peter", "Richard", "Sam" };
	private static final String[] NAMES_W = new String[] {//
	"Alice", "Carol", "Dorothy", "Emily", "Gina", //
			"Hannah", "Irene", "Kim", "Lisa", "Nora", "Petra" };

	private static final String[] NAMES_L1 = new String[] {//
	"Kin", "Ful", "Den", "Mey", "Spe", //
			"Stall", "Mill", "Guy", "Kin", "Ned", //
			"Pet", "Sulli", "Zar" };
	private static final String[] NAMES_L2 = new String[] {//
	"der", "er", "nik", "ord", //
			"nard", "one", "ver", "son", "van", "edy", "ncke" };

	String makeLastName() {
		return NAMES_L1[this.random.nextInt(NAMES_L1.length)] //
				+ NAMES_L2[this.random.nextInt(NAMES_L2.length)];
	}

	String makeName(final Gender gender) {
		List<String> names;
		switch (gender) {
		case FEMALE:
			names = L.l(NAMES_W);
			break;
		case MALE:
			names = L.l(NAMES_M);
			break;
		case UNKNOWN:
			names = L.l(Elements.concat(NAMES_M, NAMES_W));
			break;
		default:
			names = L.l("unknown", "unknown", "unknown");
		}

		StringBuilder first = new StringBuilder();
		for (int i = this.random.nextInt(3); i >= 0; i--) {
			first.append(names.remove(this.random.nextInt(names.size()))).append(" ");
		}
		first.setLength(first.length() - 1);
		return first.toString();
	}

	private List<Family> families;
	private int members = 0;
	private int marriages = 0;
	private Map<String, Member> personByName;

	private int untracked;

	public void setNumberOfThreads(final int threads) {

	}

	public void setSynchronize(final boolean synchronize) {

	}

	public static void main(final String[] args) {
		NoIoBenchmark bm = new NoIoBenchmark();
		bm.setupStructure();

		bm.performObjectGraphBenchmark();
		bm.performObjectGraphRestructureBenchmark();
		bm.performSerializationBenchmark();
		bm.performStringProcessingBenchmark();
	}

	public void setupStructure() {
		this.members = 0;
		this.marriages = 0;
		this.families = new LinkedList<Family>();
		for (int i = 0; i < NUMBER_OF_FAMILIES; i++) {
			Family f = createFamily("Family " + i);
			// f.print(new PrintWriter(System.out));
			this.families.add(f);
		}

		Log.LOG.get().report("members: " + this.members);
		Log.LOG.get().report("marriages: " + this.marriages);
		Log.LOG.get().report("untracked: " + this.untracked);
	}

	private Family createFamily(final String name) {
		Family f = new Family(name);
		Member anchestor = makeRandomMember(START_YEAR + this.random.nextInt(30));
		f.add(anchestor);

		List<Member> followed = new ArrayList<Member>();
		followed.add(anchestor);
		while (followed.size() > 0) {
			Member current = followed.remove(0);
			int year = current.getBirth().getYear();
			if (year > 2010) {
				break;
			}

			for (int i = nextIntLeft(6); i >= 0; i--) {
				// choose spoose
				if (this.random.nextInt(20) == 0) {
					// choose from other families
				}
				Marriage m = makeSpoose(current);
				f.add(m.other(current));

				int n = year < 1900 ? this.random.nextInt(8) : year < 1970 ? this.random.nextInt(6) : nextIntLeft(7);
				for (int j = 0; j < n; j++) {
					Member child = makeChild(m);
					if (child == null) {
						break;
					}
					f.add(child);
					if (child.getLastName().equals(current.getLastName()) || this.random.nextInt(3) == 0) {
						followed.add(child);
					}
				}

			}
			while (followed.size() > NUMBER_OF_TRACKED_MEMBERS) {
				followed.remove(this.random.nextInt(followed.size()));
				this.untracked++;
			}
		}
		return f;
	}

	private Member makeRandomMember(final int year) {
		Gender g;
		if (year > 1960 && this.random.nextInt(20) == 0) {
			g = Gender.UNKNOWN;
		} else {
			g = this.random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
		}

		String lastName = makeLastName();
		String name = makeName(g);
		LocalDate birth = new LocalDate(year, this.random.nextInt(12) + 1, this.random.nextInt(27) + 1);
		this.members++;
		return new Member(name, lastName, g, birth);
	}

	private Marriage makeSpoose(final Member member) {
		LocalDate md;
		if (member.getMarriages().isEmpty()) {
			md = member.getBirth().plusYears(16 + this.random.nextInt(20));
		} else {
			Marriage prevMarriage = member.getMarriages().get(member.getMarriages().size() - 1);
			md = prevMarriage.getMarriage().plusYears(1 + this.random.nextInt(10));
			prevMarriage.setDivorce(md.minusDays(1 + this.random.nextInt(300)));
		}
		md = rndDay(md);

		LocalDate birth = rndDay(md.minusYears(16 + this.random.nextInt(20)));

		Gender g = member.getGender() == Gender.FEMALE ? Gender.MALE : Gender.FEMALE;
		String name = makeName(g);

		this.members++;
		Member spoose = new Member(name, makeLastName(), g, birth);

		this.marriages++;
		Marriage marriage = new Marriage(member, spoose, md);
		spoose.add(marriage);
		member.add(marriage);
		return marriage;
	}

	private Member makeChild(final Marriage m) {
		LocalDate early = m.getHusband().getBirth().plusYears(16);
		if (early.isAfter(m.getWife().getBirth().plusYears(14))) {
			early = m.getWife().getBirth().plusYears(14);
		}
		if (m.getChildren().size() > 0) {
			Member last = m.getChildren().last();

			if (early.isBefore(last.getBirth().plusYears(1))) {
				early = last.getBirth().plusYears(1);
			}
		}

		LocalDate late = m.getHusband().getBirth().plusYears(80);
		if (late.isBefore(m.getWife().getBirth().plusYears(45))) {
			late = m.getWife().getBirth().plusYears(45);
		}

		if (!late.isAfter(early)) {
			return null;
		}

		int days = Days.daysBetween(early, late).getDays();
		if (days <= 0) {
			return null;
		}
		LocalDate birth = early.plusDays(nextIntLeft(days));
		int year = birth.getYear();
		if (year > 2010) {
			return null;
		}

		Gender g = this.random.nextBoolean() ? Gender.MALE : Gender.FEMALE;
		String name = makeName(g);

		String lastName = this.random.nextInt(20) == 0 ? m.getWife().getLastName() : m.getHusband().getLastName();
		this.members++;
		Member child = new Member(name, lastName, g, birth);

		m.add(child);
		return child;
	}

	private int nextIntLeft(final int range) {
		return this.random.nextInt(range) * this.random.nextInt(range) / range;
	}

	private LocalDate rndDay(final LocalDate md) {
		return md.plusDays(this.random.nextInt(300));
	}

	public void discardStructure() {
		this.families = null;
	}

	public void performObjectGraphBenchmark() {
		Map<Member, Integer> seen = L.map();
		int max = 0;
		for (Family f : this.families) {
			for (Member m : f.getMembers()) {
				if (seen.containsKey(m)) {
					continue;
				}

				int d = maxDepthOf(m, seen);

				if (d > max) {
					max = d;
				}
			}
		}
		// Log.LOG.get().report("max depth " + max);
	}

	public int maxDepthOf(final Member member, final Map<Member, Integer> seen) {
		int max = 0;
		for (Marriage mm : member.getMarriages()) {
			for (Member c : mm.getChildren()) {
				Integer dep = seen.get(c);
				int d;
				if (dep != null) {
					d = dep.intValue() + 1;
				} else {
					d = maxDepthOf(c, seen) + 1;
				}
				if (d > max) {
					max = d;
				}
			}
		}
		seen.put(member, Integer.valueOf(max));
		return max;
	}

	public void performSerializationBenchmark() {
		Region bytes = ObjectTool.serialize((Serializable) this.families);
		List<Family> result = (List<Family>) ObjectTool.deserialize(bytes);
		assert result.size() == this.families.size() : "implausible deserialization";
		Log.LOG.get().report("size " + bytes.length());
	}

	public void performStringProcessingBenchmark() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(baos, 8192);
		OutputStreamWriter osw = new OutputStreamWriter(bos, Misc.UTF_8);
		PrintWriter pw = new PrintWriter(osw);

		for (Family f : this.families) {
			f.print(pw);
		}
		pw.flush();
		Log.LOG.get().report("size " + baos.size());
	}


	public void performObjectGraphRestructureBenchmark() {
		// TODO
	}

	public int getLogicalObjectCount() {
		return this.members;
	}


	public double getOperationsPerMs(final double averageObjectGraphAccessMs, final double averageObjectGraphRestructureMs,
			final double averageSerializationMs, final double averageStringProcessingMs) {
		double totalWeightedMs = averageObjectGraphAccessMs * 100//
				+ averageObjectGraphRestructureMs * 10 + averageStringProcessingMs * 3 + averageSerializationMs;
		double averageMsPerObject = totalWeightedMs / getLogicalObjectCount();
		return 1 / averageMsPerObject;
	}
}
