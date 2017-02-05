package de.zarncke.lib.jna;

import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import de.zarncke.lib.coll.L;

public interface Kernel32 extends Library {
	final Kernel32 K32 = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class, W32APIOptions.ASCII_OPTIONS);

	boolean CreateHardLink(String from, String to, Pointer reserved);

	boolean GetDiskFreeSpace(String path, IntByReference sectorPerCluster, IntByReference bytesPerSector,
			IntByReference freeCluster, IntByReference totalCluster);

	int GetLastError();

	// void GlobalMemoryStatus(MEMORYSTATUS result);
	boolean GlobalMemoryStatusEx(MEMORYSTATUSEX result);

	public static class MEMORYSTATUSEX extends Structure {
		public int dwLength;
		public int dwMemoryLoad;
		public long ullTotalPhys;
		public long ullAvailPhys;
		public long ullTotalPageFile;
		public long ullAvailPageFile;
		public long ullTotalVirtual;
		public long ullAvailVirtual;
		public long ullAvailExtendedVirtual;

		@Override
		protected List getFieldOrder() {
			return L.l("dwLength", "dwMemoryLoad", "ullTotalPhys", "ullAvailPhys", "ullTotalPageFile",
					"ullAvailPageFile", "ullTotalVirtual", "ullAvailVirtual", "ullAvailExtendedVirtual");
		}
	}

	// public class MEMORYSTATUS extends Structure {
	// public int dwLength;
	// public int dwMemoryLoad;
	// public int dwTotalPhys;
	// public int dwAvailPhys;
	// public int dwTotalPageFile;
	// public int dwAvailPageFile;
	// public int dwTotalVirtual;
	// public int dwAvailVirtual;
	// }
}