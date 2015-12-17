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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.Installer;

/**
 * Common file utility helper methods.
 */
public class FileUtils {
	/** New line separator */
	private static final String NEWLINE = System.getProperty("line.separator");

	/**
	 * Copies source directory to target and preserves attributes.  Any existing files will be replaced.
	 * Links will be followed.
	 * 
	 * @param source Path of source directory
	 * @param target Path of target directory
	 * @param replace <code>true</code> to replace already existing target directory
	 * @throws IOException on failure
	 */
	public static void copyDirectory(Path source, Path target, boolean replace) throws IOException {
		CopyOption[] options = (replace)?new CopyOption[] {StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING}:
			new CopyOption[] {StandardCopyOption.COPY_ATTRIBUTES};

		// Follow links during copy.
		EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
		FilesCopier tc = new FilesCopier(source, target, options);
		Files.walkFileTree(source, opts, Integer.MAX_VALUE, tc);
	}
	
	/**
	 * Copies source file to destination by preserving file attributes
	 * @param srcFile Path of source file.
	 * @param destFile Path of target file.
	 * @param replace <code>true</code> to replace already existing target file.
	 * @throws IOException in case of failure
	 */
	public static void copyFile(Path srcFile, Path destFile, boolean replace) throws IOException {
		CopyOption[] options = (replace)?new CopyOption[] {StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING}:
			new CopyOption[] {StandardCopyOption.COPY_ATTRIBUTES};
		
		Files.copy(srcFile, destFile, options);
	}
	
	/**
	 * Deletes a directory.
	 * 
	 * @param directory Directory to delete
	 * @throws IOException on failure to delete the directory
	 */
	public static void deleteDirectory(Path directory) throws IOException {
		File[] files = deleteFiles(directory, null, new NullProgressMonitor());
		if (files.length > 0) {
			throw new IOException("Failed to delete " + files[0].getAbsolutePath() + ".");
		}
		else {
			Files.delete(directory);
		}
	}

	/**
	 * Deletes files in a directory.  This method does not delete the directory itself.
	 * This method does not throw an exception on failure, but instead returns any files that could not be deleted.
	 * 
	 * @param directory Directory to delete
	 * @param excludedPaths Paths (files or directories) to exclude from deletion or <code>null</code>
	 * @param monitor Progress monitor or <code>null</code>
	 * @return Files that could not be deleted
	 */
	public static File[] deleteFiles(final Path directory, final Path[] excludedPaths, final IProgressMonitor monitor) {
		final ArrayList<File> filesNotRemoved = new ArrayList<File>();
		final IPath directoryPath = new org.eclipse.core.runtime.Path(directory.toFile().getAbsolutePath());
		try {
			final int[] fileCount = new int[] { 0 };
			
			// Count file to be removed
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path path,
						BasicFileAttributes attrs) throws IOException {
					fileCount[0] ++;
					// If file is read-only, make it writable
					File file = path.toFile();
					if (!file.canWrite()) {
						if (!file.setWritable(true)) {
							throw new IOException("Failed to set file writeable: " + file.getAbsolutePath());
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
			
			if (monitor != null) {
				monitor.beginTask(NLS.bind(InstallMessages.Removing0, ""), fileCount[0]);
			}
			
			// Delete files and sub-directories
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				/**
				 * Returns if a path is excluded.
				 * 
				 * @param arg Path
				 * @return <code>true</code> if path is excluded
				 */
				private boolean isExcluded(Path arg) {
					boolean excluded = false;
					if (excludedPaths != null) {
						for (Path excludedPath : excludedPaths) {
							if (excludedPath.equals(arg)) {
								excluded = true;
								break;
							}
						}
					}
					
					return excluded;
				}
				
				@Override
				public FileVisitResult preVisitDirectory(Path arg0,
						BasicFileAttributes arg1) throws IOException {
					// Is directory excluded
					if (isExcluded(arg0)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					else {
						return FileVisitResult.CONTINUE;
					}
				}

				@Override
				public FileVisitResult postVisitDirectory(Path arg0,
						IOException arg1) throws IOException {
					try {
						if (!arg0.equals(directory)) {
							if (monitor != null) {
								IPath filePath = new org.eclipse.core.runtime.Path(arg0.toFile().getAbsolutePath()).removeFirstSegments(directoryPath.segmentCount()).setDevice("");
								monitor.setTaskName(NLS.bind(InstallMessages.Removing0, filePath.toOSString()));
							}
							// Delete directory
							Files.delete(arg0);
						}
					}
					catch (Exception e) {
						// Ignore
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path arg0, BasicFileAttributes arg1)
						throws IOException {
					try {
						if (!isExcluded(arg0)) {
							// Delete file
							Files.delete(arg0);
						}
					}
					catch (Exception e) {
						filesNotRemoved.add(arg0.toFile());
						Installer.log(e);
					}
					if (monitor != null) {
						monitor.worked(1);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// If an exception is thrown from visitor, it is an error
			Installer.log(e);
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
		
		return filesNotRemoved.toArray(new File[filesNotRemoved.size()]);
	}
	
	/**
	 * Reads the entire contents of a file into a string.
	 * 
	 * @param file File to read
	 * @return File contents
	 * @throws IOException on failure to read file
	 */
	public static String readFile(File file) throws IOException {
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(NEWLINE);
			}
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}
		
		return buffer.toString();
	}
	
	/**
	 * Sets all files in a directory to be writable.
	 * 
	 * @param directory Directory
	 * @throws IOException on failure
	 */
	public static void setWritable(Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			/**
			 * Sets a directory or file writable.
			 * 
			 * @param path Path to directory or file
			 * @return File visit result
			 * @throws IOException on failure
			 */
			private FileVisitResult setWritable(Path path) throws IOException {
				File file = path.toFile();
				if (!file.canWrite()) {
					if (!file.setWritable(true)) {
						throw new IOException("Failed to set file writeable: " + file.getAbsolutePath());
					}
				}
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				return setWritable(dir);
			}

			@Override
			public FileVisitResult visitFile(Path arg0, BasicFileAttributes arg1)
					throws IOException {
				return setWritable(arg0);
			}
		});
	}
}
