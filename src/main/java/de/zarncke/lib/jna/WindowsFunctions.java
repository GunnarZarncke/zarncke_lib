package de.zarncke.lib.jna;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.ptr.IntByReference;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.jna.Kernel32.MEMORYSTATUSEX;
import de.zarncke.lib.util.Q;

/**
 * This helper contains functions using various functions only available on Windows systems.
 * It uses e.g. Kernel32 over JNA or drive C:\.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public class WindowsFunctions {
	private WindowsFunctions() {
		// hidden constructor of helper class
	}

	public static void init() {
		Kernel32.K32.getClass();
	}

	public static Pair<Long, Long> getAvailableSystemDiskBytesWindows(final File fileOnFileSystem) {
		String path = fileOnFileSystem == null ? "C:\\" : fileOnFileSystem.getAbsolutePath();
		if (path.startsWith("\\\\")) {
			// is UNC
			int p = path.indexOf("\\", 2);
			path = path.substring(0, p + 1);
		} else {
			int p = path.indexOf(":\\");
			if (p == 1) {
				path = path.substring(0, 3);
			}
		}
		IntByReference spc = new IntByReference();
		IntByReference bps = new IntByReference();
		IntByReference fc = new IntByReference();
		IntByReference tc = new IntByReference();
		if (Kernel32.K32.GetDiskFreeSpace(path, spc, bps, fc, tc)) {
			return Pair.pair(Q.l((long) bps.getValue() * spc.getValue() * fc.getValue()),
					Q.l((long) bps.getValue() * spc.getValue() * tc.getValue()));
		}
		return null;
	}

	public static class FILE_ZERO_DATA_INFORMATION extends Structure {
		public long FileOffset;
		public long BeyondFinalZero;

		@Override
		protected List getFieldOrder() {
			return L.l("FileOffset", "BeyondFinalZero");
		}
	}

	/**
	 * from http://pinvoke.net/default.aspx/Constants/WINBASE.html
	 */
	public final static int FSCTL_SET_SPARSE = 0x000900c4;
	public final static int FSCTL_SET_ZERO_DATA = 0x000980c8;

	/**
	 * Tries to zero a section of a file.
	 * This is also called 'punching a hole'.
	 * Converts the file into a sparse if if it is not already.
	 * Only supported on some file systems, e.g. NTFS.
	 *
	 * @param file to punch hole into
	 * @param position offset
	 * @param length length of hole
	 * @return true: successfully punched hole; false: operation not supported by file system
	 * @throws IOException operation supported but failed
	 */
	public static boolean setSparseZeros(final File file, final long position, final long length) throws IOException {
		com.sun.jna.platform.win32.Kernel32 k32 = com.sun.jna.platform.win32.Kernel32.INSTANCE;

		HANDLE hFile = null;
		hFile = k32.CreateFile(file.getCanonicalPath(), WinNT.GENERIC_ALL, WinNT.FILE_SHARE_WRITE
				| WinNT.FILE_SHARE_READ,
				new WinBase.SECURITY_ATTRIBUTES(), WinNT.OPEN_EXISTING, WinNT.FILE_ATTRIBUTE_NORMAL,
				new HANDLEByReference().getValue());

		if (WinBase.INVALID_HANDLE_VALUE.equals(hFile)) {
			throw Warden.spot(new IOException("failed to open " + file, new Win32Exception(
					com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError())));
		}
		Throwable excep = null;
		try {
			// Buffer buffer = ByteBuffer.wrap(new byte[100]);
			// IntByReference bytesRead = new IntByReference();
			// boolean r = k32.ReadFile(hFile, buffer, 10, bytesRead, null);
			// System.out.println(r + ": " + Elements.toString(buffer.array()));

			IntByReference dwTemp = new IntByReference();
			// ::DeviceIoControl(hFile, FSCTL_SET_SPARSE, NULL, 0, NULL, 0, &dwTemp, NULL);
			boolean res = k32.DeviceIoControl(hFile, FSCTL_SET_SPARSE, null, 0, null, 0, dwTemp, null);
			if (!res) {
				throw new Win32Exception(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError());
			}

			FILE_ZERO_DATA_INFORMATION fzdi = new FILE_ZERO_DATA_INFORMATION();
			fzdi.FileOffset = position;
			fzdi.BeyondFinalZero = position + length;
			fzdi.write();
			// ::DeviceIoControl(hFile, FSCTL_SET_ZERO_DATA, &fzdi, sizeof(fzdi), NULL, 0, &dwTemp, NULL);
			res = k32
					.DeviceIoControl(hFile, FSCTL_SET_ZERO_DATA, fzdi.getPointer(), fzdi.size(), null, 0, dwTemp, null);
			if (!res) {
				throw new Win32Exception(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError());
			}

			// buffer.clear();
			// r = k32.ReadFile(hFile, buffer, 10, bytesRead, null);
			// System.out.println(r + ": " + Elements.toString(buffer.array()));

			return true;
		} catch (Win32Exception e) {
			excep = e;
			throw e;
		} finally {
			if (hFile != null) {
				if (!com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(hFile)) {
					if (excep != null) {
						throw (Win32Exception) new Win32Exception(
								com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError()).initCause(excep);
					}
					throw new Win32Exception(com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError());
				}
			}
		}
	}

	public static void hardlink(final File src, final File dest) throws IOException {
		if (!Kernel32.K32.CreateHardLink(dest.getAbsolutePath(), src.getAbsolutePath(), null)) {
			int error = Kernel32.K32.GetLastError();
			switch (error) {
			case 2:
			case 3:
			case 15:
				throw new IOException("src " + src + " does not exist ");
			case 5:
				throw new IOException("permission denies to access " + dest);
			case 14:
				throw new IOException("disk full " + dest);
			default:
				throw new IOException("hard link " + src + " to " + dest + " failed with code " + error);
			}
		}
	}

	public static Pair<Long, Long> getMeminfoWindows() {
		MEMORYSTATUSEX mem = new MEMORYSTATUSEX();
		mem.dwLength = mem.size();
		if (!Kernel32.K32.GlobalMemoryStatusEx(mem)) {
			int error = Kernel32.K32.GetLastError();
			throw new RuntimeException("Windows returned error " + error + " on call to GlobalMemoryStatusEx");
		}

		return Pair.pair(Q.l(mem.ullAvailPhys), Q.l(mem.ullTotalPhys));
	}

}
