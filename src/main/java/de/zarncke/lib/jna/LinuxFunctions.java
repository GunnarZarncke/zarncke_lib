package de.zarncke.lib.jna;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.DiskInfo;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.StoreConsumer;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.util.Misc;

/**
 * This helper contains functions using various functions only available on (some) Linux/*ix/Posix systems.
 * It uses e.g. LibX over JNA, /proc or /usr/bin commands.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class LinuxFunctions {
	public static final String PROC_MEMINFO = "/proc/meminfo";
	public static final String PROC_MEMINFO_MEM_TOTAL = "MemTotal:";
	public static final String PROC_MEMINFO_MEM_FREE = "MemFree:";
	// from http://www.kneuro.net/cgi-bin/lxr/http/source/include/asm-arm/errno.h
	private static final int EOPNOTSUPP = 95;
	// from http://www.delorie.com/djgpp/doc/incs/fcntl.h
	private static final int O_RDONLY = 0x0000;
	private static final int O_WRONLY = 0x0001;
	private static final int O_RDWR = 0x0002;
	private static final int O_CREAT = 0x0100;
	private static final int O_EXCL = 0x0200;
	private static final int O_NOCTTY = 0x0400;
	private static final int O_TRUNC = 0x0800;
	private static final int O_APPEND = 0x1000;
	private static final int O_NONBLOCK = 0x2000;
	// from http://fossies.org/dox/fio-2.0.14/falloc_8c.html
	private static final int FALLOC_FL_KEEP_SIZE = 0x01;
	private static final int FALLOC_FL_PUNCH_HOLE = 0x02;

	/**
	 * Functions from the libc.
	 *
	 * @author Gunnar Zarncke <gunnar@zarncke.de>
	 */
	private interface LibC extends Library {
		final LibC LIBC = (LibC) Native.loadLibrary("c", LibC.class);

		int link(String from, String to);

		String strerror(int errno);

		int open(String filename, int flags);

		int close(int filedes);
	}

	/**
	 * Functions from libc not available in all kernels.
	 */
	private interface LibCExt extends Library {
		final LibCExt LIBC = (LibCExt) Native.loadLibrary("c", LibCExt.class);

		/**
		 * since 2.6.38
		 * see http://man7.org/linux/man-pages/man2/fallocate.2.html
		 *
		 * @param fd
		 * @param mode
		 * @param offset
		 * @param len
		 * @return status
		 */
		int fallocate(int fd, int mode, long offset, long len);
	}

	private LinuxFunctions() {
		// hidden constructor of helper class
	}

	public static void init() {
		LibC.LIBC.getClass();
	}

	/**
	 * Tries to zero a section of a sparse file.
	 * This is also called 'punching a hole'.
	 * Only supported on some file systems, e.g. btrfs, tmpfs. Fuse can support it. XFS can depending on version.
	 * Vzfs (used by virtualizers) seems to have troubls with this (returns EINVAL instead of EOPNOTSUPP).
	 * 
	 * @param file to punch hole into
	 * @param position offset
	 * @param length length of hole
	 * @return true: successfully punched hole; false: operation not supported by file system
	 * @throws IOException operation supported but failed
	 */
	public static boolean setSparseZeros(final File file, final long position, final long length) throws IOException {
		int fd = LibC.LIBC.open(file.getCanonicalPath(), O_RDWR);
		if (fd == -1) {
			throw new IOException(file + ":" + LibC.LIBC.strerror(Native.getLastError()));
		}
		try {
			int res = LibCExt.LIBC.fallocate(fd, FALLOC_FL_PUNCH_HOLE | FALLOC_FL_KEEP_SIZE, position, length);
			if (res == EOPNOTSUPP) {
				return false;
			}
			if (res != 0) {
				int lastError = Native.getLastError();
				if (lastError == EOPNOTSUPP) {
					return false;
				}
				throw new IOException(lastError + ":" + LibC.LIBC.strerror(lastError) + " for " + file + " " + position + "+"
						+ length);
			}
			return true;
		} finally {
			if (fd > 0) {
				if (LibC.LIBC.close(fd) != 0) {
					throw new IOException("unexpected:" + LibC.LIBC.strerror(Native.getLastError()));
				}
			}
		}

	}

	public static Long getMeminfoLinux(final String memInfoKey) {
		try {
			String proc = getMeminfoLinux();
			List<String> meminfo = L.l(proc.split("\n"));
			for (String info : meminfo) {
				if (info.startsWith(memInfoKey)) {
					String[] parts = info.split(" ");
					return Long.valueOf(Long.parseLong(parts[parts.length - 2]) * Misc.BYTES_PER_KB);
				}
			}
			return null;
		} catch (Exception e) {
			Warden.disregard(e);
			return null;
		}
	}

	public static String getMeminfoLinux() throws IOException, FileNotFoundException {
		File meminfo = new File(PROC_MEMINFO);
		if (!meminfo.exists()) {
			return "<no meminfo>";
		}
		if (!meminfo.canRead()) {
			return "<may not read meminfo>";
		}
		String res = new String(IOTools.getAllBytes(new FileInputStream(PROC_MEMINFO)), Misc.ASCII);
		if (res.isEmpty()) {
			return "<empty, expected " + meminfo.length() + ">";
		}
		return res;
	}

	/**
	 * Uses /usr/bin/df to query disk sizes. Use on you own risk.
	 *
	 * @return List of {@link DiskInfo}
	 */
	public static List<DiskInfo> getDiskInfos() {
		List<String> df;
		try {
			df = Misc.processCommand("/bin/df -P", new StoreConsumer<List<String>>() {
				@Override
				public List<String> consume(final Store storeToProcess) throws IOException {
					return IOTools.getAllLines(storeToProcess.getInputStream());
				}
			}).getFirst();
		} catch (IOException e) {
			Warden.disregard(e);
			return L.e();
		}

		List<DiskInfo> diskInfos = L.l();
		for (String line : df) {
			String[] parts = line.split(" +");
			try {
				long free = Long.parseLong(parts[3]) * Misc.BYTES_PER_KB;
				long total = Long.parseLong(parts[1]) * Misc.BYTES_PER_KB;
				diskInfos.add(new DiskInfo(parts[parts.length - 1], free, total));
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		return diskInfos;
	}

	public static Pair<Long, Long> getAvailableSystemDiskBytes(final File fileOnFileSystem) {
		List<DiskInfo> diskInfos = getDiskInfos();
		if (diskInfos.isEmpty()) {
			return null;
		}
		if (fileOnFileSystem == null) {
			return diskInfos.get(0).toFreeAndTotal();
		}

		// sort by path length longest first
		Collections.sort(diskInfos, new Comparator<DiskInfo>() {
			@Override
			public int compare(final DiskInfo o1, final DiskInfo o2) {
				return -Misc.compare(o1.getName().length(), o2.getName().length());
			}
		});

		for (DiskInfo di : diskInfos) {
			if (fileOnFileSystem.getAbsolutePath().startsWith(di.getPath())) {
				return di.toFreeAndTotal();
			}
		}
		return null;
	}

	public static void hardlink(final File src, final File dest) throws IOException {
		if (LibC.LIBC.link(src.toString(), dest.toString()) != 0) {
			throw new IOException(LibC.LIBC.strerror(Native.getLastError()));
		}
	}
}
