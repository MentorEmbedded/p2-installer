/*******************************************************************************
 *  Copyright (c) 2014 Mentor Graphics and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mentor Graphics - initial API and implementation
 *******************************************************************************/
package com.codesourcery.internal.installer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Shell;

import com.codesourcery.installer.IInstallPlatform;
import com.codesourcery.installer.IInstallPlatformActions;
import com.codesourcery.installer.Installer;

/**
 * Install platform
 */
public class InstallPlatform implements IInstallPlatform {
	/** Windows short-cut file extension */
	private static final String WINDOWS_SHORTCUT_EXTENSION = "lnk";
	/** Windows redistributable driver installer binary name */
	private static final String WINDOWS_DPINST = "dpinst";
	
	/** Path to install monitor binary */
	private String path;
	/** Path to the redistributables directory */
	private String redistPath;
	/** Installer process identifier */
	private String installerPid;
	/** Log path */
	private String logPath;
	/** Response file for commands */
	private File responseFile = null;
	/** Platform dependent actions */
	private IInstallPlatformActions osActions;
	
	/**
	 * Constructor
	 * 
	 * @param path Path to install monitor binary
	 * @param redistPath Path to the directory containing redistrubutables or
	 * <code>null</code>
	 * @param installerPid Process identifier for installer
	 * @param logPath Path to log file or <code>null</code> to not log
	 * @param useResponseFile <code>true</code> to use response file
	 */
	public InstallPlatform(String path, String redistPath, String installerPid, String logPath) throws IOException {
		this.path = path;
		this.redistPath = redistPath;
		this.installerPid = installerPid;
		this.logPath = logPath;
		
		// Create temporary response file to avoid command line length
		// limitations
		responseFile = File.createTempFile("instmon", null);
	}
	
	/**
	 * Returns the platform actions.
	 * 
	 * @return Platform actions
	 */
	private IInstallPlatformActions getPlatformActions() {
		if (osActions == null) {
			osActions = ContributorRegistry.getDefault().getPlatformActions();
		}
		
		return osActions;
	}
	
	/**
	 * Returns the path to the install monitor binary.
	 * 
	 * @return Path to binary
	 */
	private String getPath() {
		return path;
	}
	
	/**
	 * Returns the path to the redistributables directory.
	 * 
	 * @return Path to redistributable directory or <code>null</code>
	 */
	private String getRedistPath() {
		return redistPath;
	}
	
	/**w
	 * Returns the installer process identifier.
	 * 
	 * @return Process identifier
	 */
	private String getInstallerPid() {
		return installerPid;
	}
	
	/**
	 * Returns the path to the log file.
	 * 
	 * @return Path to log file or <code>null</code>
	 */
	private String getLogPath() {
		return logPath;
	}
	
	/**
	 * Convenience method to run install monitor with a specified set of
	 * options and wait for result.
	 * 
	 * @param options Options
	 * @return Command result
	 * @throws CoreException
	 */
	private String run(String[] options) throws CoreException {
		return run(options, true);
	}
	
	/**
	 * Runs the install monitor with a specified set of options.
	 * 
	 * @param options Options
	 * @param waitForResult <code>true</code> to wait for monitor and
	 * return result, <code>false</code> to not wait for monitor and return
	 * <code>null</code>.
	 * @return Command result
	 * @throws CoreException on failure
	 */
	private String run(String[] options, boolean waitForResult) throws CoreException {
		BufferedWriter writer = null;
		StreamHandler outputHandler = null;
		StreamHandler errorHandler = null;
		
		if (options.length == 0) {
			return null;
		}
		
		try {
			// Create response file for options to avoid command line
			// length limitations
			writer = new BufferedWriter(new FileWriter(responseFile, false));
			for (String option : options) {
				writer.write(option);
				writer.newLine();
			}
			// Add log option
			if (getLogPath() != null) {
				writer.write("-log \"" + getLogPath() + "\"");
				writer.newLine();
			}
			writer.close();

			ArrayList<String> args = new ArrayList<String>();
			// Monitor executable
			args.add(getPath());
			// Use response file to avoid any command line length limitations
			args.add("-tempfile");
			args.add("\"" + responseFile.getAbsolutePath() + "\"");
			
			// Create monitor process
			ProcessBuilder builder = new ProcessBuilder(args);
			Process process = builder.start();
			// Wait for result
			if (waitForResult) {
				// Get output
				outputHandler = new StreamHandler(process.getInputStream());
				outputHandler.start();
				// Get error
				errorHandler = new StreamHandler(process.getErrorStream());
				errorHandler.start();
				// Wait for process
				process.waitFor();
				// Handle error
				String error = errorHandler.getOutput();
				if (!error.isEmpty()) {
					Installer.fail(error);
				}
			}
		}
		catch (Exception e) {
			StringBuffer opts = new StringBuffer();
			for (String option : options) {
				opts.append(option);
				opts.append(' ');
			}
			Installer.fail(InstallMessages.Error_MonitorCommand + opts.toString(), e);
		}
		finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
		
		return (outputHandler != null) ? outputHandler.getOutput() : null;
	}
	
	@Override
	public void dispose(String[] directories, String[] emptyDirectories) 
			throws CoreException
	{
		try {
			boolean removeDirectories = false;
			
			ArrayList<String> commands = new ArrayList<String>();
			StringBuffer arg;
			
			// Add directories
			if ((directories != null) && (directories.length > 0)) {
				arg = new StringBuffer("-removeDir ");
				arg.append('\"');
				for (int index = 0; index < directories.length; index++) {
					if (index != 0) {
						arg.append(',');
					}
					arg.append(directories[index]);
				}
				arg.append('\"');
				commands.add(arg.toString());
				removeDirectories = true;
			}
			
			// Add empty directories
			if ((emptyDirectories != null) && (emptyDirectories.length > 0)) {
				arg = new StringBuffer("-removeEmptyDir ");
				arg.append('\"');
				for (int index = 0; index < emptyDirectories.length; index++) {
					if (index != 0) {
						arg.append(',');
					}
					arg.append(emptyDirectories[index]);
				}
				arg.append('\"');
				commands.add(arg.toString());
				removeDirectories = true;
			}
			
			if (removeDirectories) {
				// Add PID of installer
				commands.add("-pid " + getInstallerPid());
				// Add time to wait
				commands.add("-wait 10");
			}

			// Delete platform binary
			commands.add("-deleteOnExit");
			
			run(commands.toArray(new String[commands.size()]), false);
		}
		catch (Exception e) {
			StringBuffer dirs = new StringBuffer();
			if (directories != null) {
				for (String dir : directories)
					dirs.append(dir);
			}
			if (emptyDirectories != null) {
				for (String dir : emptyDirectories)
					dirs.append(dir);
			}
			Installer.fail(InstallMessages.Error_FailedToRemoveDirectories + dirs.toString(), e);
		}
	}

	@Override
	public void deleteDirectory(String path, boolean onlyIfEmpty) throws CoreException {
		run(new String[] {
				(onlyIfEmpty ? "-removeEmptyDir" : "-removeDir") +
				MessageFormat.format(" \"{0}\"", new Object[] { 
						path,
					})
			});
	}
	
	/**
	 * Sets a registry value.
	 * 
	 * @param key Fully qualified registry key
	 * @param name Value name
	 * @param value Value
	 * @param type Value type ("string" or "dword")
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException If operation is not supported
	 * by operating system.
	 */
	private void setRegistryValue(String key, String name, String value, String type) 
			throws CoreException, UnsupportedOperationException {
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();
		
		run(new String[] {
				MessageFormat.format("-regSetValue \"{0},{1},{2},{3}\"", new Object[] { 
						key,
						name,
						value,
						type
					})
			});
	}
	
	@Override
	public void setWindowsRegistryValue(String key, String name, String value)
			throws CoreException, UnsupportedOperationException {
		setRegistryValue(key, name, value, "string");
	}

	@Override
	public void setWindowsRegistryValue(String key, String name, int value)
			throws CoreException, UnsupportedOperationException {
		setRegistryValue(key, name, Integer.toString(value), "dword");
	}

	@Override
	public void deleteWindowsRegistryKey(String key) throws CoreException, UnsupportedOperationException {
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();
		
		run(new String[] {
				MessageFormat.format("-regDeleteKey \"{0}\"", new Object[] { 
						key,
					})
			});
	}

	@Override
	public void deleteWindowsRegistryValue(String key, String name)
			throws CoreException, UnsupportedOperationException {
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();

		run(new String[] {
				MessageFormat.format("-regDeleteValue \"{0},{1}\"", new Object[] { 
						key,
						name
					})
			});
	}

	/**
	 * Returns a Windows folder identifier for a
	 * named folder.
	 * 
	 * @param folder Folder name
	 * @return Folder identifier
	 */
	private String getWindowsFolderId(ShortcutFolder folder) {
		if (folder == ShortcutFolder.PROGRAMS) {
			return "CSIDL_PROGRAMS";
		}
		else if (folder == ShortcutFolder.DESKTOP) {
			return "CSIDL_DESKTOP";
		}
		else {
			return null;
		}
	}
	
	/**
	 * Returns the Windows System folder. A Typical path is C:\Windows\System32.
	 * 
	 * Only supported on Windows.
	 * 
	 * @return Folder path
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException if operation is not supported by
	 * the operating system.
	 */
	public IPath getWindowsSystemFolder() throws CoreException, UnsupportedOperationException {
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();
		
		String folderId = "CSIDL_SYSTEM";
		
		String path = run(new String[] {
				MessageFormat.format("-getSpecialFolder \"{0}\"", new Object[] { folderId })
		});
		
		return new Path(path);
	}
	
	/**
	 * Returns the path to a special folder.
	 * Only supported on Windows.
	 * 
	 * @param folder Folder
	 * @return Folder path
	 * @throws CoreException on failure
	 */
	private IPath getWindowsSpecialFolderPath(ShortcutFolder folder) throws CoreException {
		String folderId = getWindowsFolderId(folder);
		
		String path = run(new String[] {
				MessageFormat.format("-getSpecialFolder \"{0}\"", new Object[] { folderId })
		});

		return new Path(path);
	}

	@Override
	public void createShortcut(IPath path, String linkName,
			IPath targetFile, String arguments, IPath workingDirectory, IPath iconPath,
			int iconIndex)
			throws CoreException {
		
		run(new String[] {
				MessageFormat.format("-createShortcut \"{0},{1},{2},{3},{4},{5},{6},{7},{8}\"", new Object[] { 
						path.toOSString(),
						linkName,
						targetFile.toOSString(),
						arguments != null ? arguments : "",
						"",
						"-1",
						workingDirectory != null ? workingDirectory.toOSString() : "",
						(iconPath == null) ? targetFile.toOSString() : iconPath.toOSString(),
						Integer.toString(iconIndex)
					})
			});
	}

	@Override
	public void deleteShortcut(IPath path, String linkName) throws CoreException {
		path = path.append(linkName);
		if (Installer.isWindows())
			path = path.addFileExtension(WINDOWS_SHORTCUT_EXTENSION);
		
		if (!path.toFile().delete())
			Installer.fail("Failed to delete short-cut: " + path.toOSString());
	}
	
	@Override
	public String getWindowsRegistryValue(String key, String name)
			throws CoreException {
		return run(new String[] {
				MessageFormat.format("-regGetValue \"{0},{1}\"", new Object[] { key, name })
		});
	}
	
	/**
	 * Monitor stream handler.
	 */
	private class StreamHandler extends Thread {
		/** Input stream */
		private InputStream stream;
		/** Stream buffer */
		private StringBuffer buffer = new StringBuffer();
		
		/**
		 * Constructor
		 * 
		 * @param stream Stream
		 */
		public StreamHandler(InputStream stream) {
			this.stream = stream;
		}
		
		/**
		 * Returns the current output of the stream.
		 * 
		 * @return Output
		 */
		public String getOutput() {
			return buffer.toString();
		}

		@Override
		public void run() {
			BufferedReader reader = null;
			try {
				InputStreamReader streamReader = new InputStreamReader(stream);
				reader = new BufferedReader(streamReader);
				String line = null;
				while ((line = reader.readLine()) != null) {
					buffer.append(line);
				}
			}
			catch (Exception e) {
			}
		}
		
		
	}

	@Override
	public IPath getShortcutFolder(ShortcutFolder folder) throws CoreException {
		IPath path = null;
		if (Installer.isWindows()) {
			path = getWindowsSpecialFolderPath(folder);
		}
		else {
			if (folder == ShortcutFolder.DESKTOP) {
				path = new Path(System.getProperty("user.home"));
				path = path.append("Desktop");
			}
			else if (folder == ShortcutFolder.PROGRAMS) {
				path = new Path(System.getProperty("user.home"));
			}
		}
		
		return path;
	}
	
	public boolean desktopIsUnity() {
		// Need a more robust check here.
		String session = System.getenv("DESKTOP_SESSION"); //$NON-NLS-1$
		return session != null && session.contains("ubuntu"); //$NON-NLS-1$
	}

	@Override
	public int getWindowsMajorVersion() throws CoreException, UnsupportedOperationException {
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();
		
		String majorVersion = run(new String[] { "-getOsProperty WIN_MAJOR_VERSION" });
		if (!majorVersion.isEmpty()) {
			try {
				return Integer.parseInt(majorVersion);
			}
			catch (NumberFormatException e) {
				Installer.log(e);
				return -1;
			}
		}
		else
			return -1;
	}

	@Override
	public int getWindowsMinorVersion() throws CoreException, UnsupportedOperationException {
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();
		
		String minorVersion = run(new String[] { "-getOsProperty WIN_MINOR_VERSION" });
		if (!minorVersion.isEmpty()) {
			try {
				return Integer.parseInt(minorVersion);
			}
			catch (NumberFormatException e) {
				Installer.log(e);
				return -1;
			}
		}
		else
			return -1;
	}

	@Override
	public String launchProgram(String path, String[] arguments) 
		throws CoreException {
		StreamHandler outputHandler = null;
		StreamHandler errorHandler = null;
		
		try {
			ArrayList<String> args = new ArrayList<String>();
			// Add program to run
			args.add(path);
			// Add program arguments
			for (String argument : arguments) {
				args.add(argument);
			}
			// Launch process
			ProcessBuilder builder = new ProcessBuilder(args);
			Process process = builder.start();
			// Get output
			outputHandler = new StreamHandler(process.getInputStream());
			outputHandler.start();
			// Get error
			errorHandler = new StreamHandler(process.getErrorStream());
			errorHandler.start();
			// Wait for process
			process.waitFor();
		}
		catch (Exception e) {
			Installer.fail(e.getMessage());
		}
		// Handle error
		if (errorHandler != null) {
			String error = errorHandler.getOutput();
			if (!error.isEmpty()) {
				Installer.fail(error);
			}
		}
		// Return output
		if (outputHandler != null) {
			String output = outputHandler.getOutput();
			return output;
		}
		else {
			return "";
		}
	}

	/**
	 * Returns if the installer is running on 64-bit Windows operating system.
	 * 
	 * @return <code>true</code> if running on 64-bit Windows
	 */
	public boolean is64BitWindows() {
		boolean is64Bit = false;

		// Windows
		if (Installer.isWindows()) {
			// x86 Program Files environment variable is only set
			// for 64bit Windows
			is64Bit = (System.getenv("ProgramFiles(x86)") != null);
		}
		
		return is64Bit;
	}

	@Override
	public void installWindowsDriver(String path) throws CoreException, UnsupportedOperationException {
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();
		
		File driverDir = new File(path);
		if (!driverDir.exists())
			Installer.fail(path + " was not found.");
		
		try {
			IPath redistPath = new Path(getRedistPath()).append(is64BitWindows() ? "64" : "32").
					append(WINDOWS_DPINST).addFileExtension(IInstallConstants.EXTENSION_EXE);
			// Run the DPINST utility with administrator rights
			// /SW = Suppress the installation wizard
			// /path = Path to the directory containing drivers to be installed
			run(new String[] { "-runAdmin " + redistPath.toOSString() + ",/SW /path '" + path + "'" });
		}
		catch (Exception e) {
			Installer.fail("Failed to install driver: " + path, e);
		}
	}

	@Override
	public String updateWindowsSystemEnvironment(int timeout)
			throws CoreException, UnsupportedOperationException {
		// Operation is only supported on Windows.
		if (!Installer.isWindows())
			throw new UnsupportedOperationException();

		return run(new String[] { "-updateEnvironment " + Integer.toString(timeout) });
	}

	@Override
	public void bringShellToFront(Shell shell) {
		IInstallPlatformActions actions = getPlatformActions();
		if (actions != null) {
			actions.bringToFront(shell);
		}
	}
}
