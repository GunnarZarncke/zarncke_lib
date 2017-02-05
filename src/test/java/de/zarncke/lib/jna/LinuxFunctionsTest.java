package de.zarncke.lib.jna;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.util.Misc;

public class LinuxFunctionsTest extends GuardedTest {
	public void testRamSpace() throws FileNotFoundException, IOException {
		if (Misc.isLinux()) {
			if (failIfAfter(2014, 9, 1)) {
				return;
			}

			Long free = LinuxFunctions.getMeminfoLinux(LinuxFunctions.PROC_MEMINFO_MEM_FREE);
			if (free == null) {
				fail("missing " + LinuxFunctions.PROC_MEMINFO_MEM_FREE + " in " + LinuxFunctions.getMeminfoLinux());
			}
			Long total = LinuxFunctions.getMeminfoLinux(LinuxFunctions.PROC_MEMINFO_MEM_TOTAL);
			if (total == null) {
				fail("missing " + LinuxFunctions.PROC_MEMINFO_MEM_TOTAL + " in " + LinuxFunctions.getMeminfoLinux());
			}
		}
	}

}
