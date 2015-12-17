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

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import com.codesourcery.installer.Installer;

/**
 * Copies files and directories from source to destination
 */
public class FilesCopier implements FileVisitor<Path> {
	/** Path of source directory. **/
	private final Path source;
	/** Path of target directory. **/
	private final Path target;
	/** Copy options. **/
	private final CopyOption[] option;
	
	/**
	 * Constructor
	 * 
	 * @param source Source for copy
	 * @param target Destination for copy
	 * @param copyOption Copy options
	 */
	public FilesCopier(Path source, Path target, CopyOption[] copyOption) {
		this.source = source;
		this.target = target;
		this.option = copyOption;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path arg0, IOException arg1)
			throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir,
			BasicFileAttributes arg1) throws IOException {
		Path targetPath = target.resolve(source.relativize(dir));
		if(!Files.exists(targetPath)){
			Files.createDirectory(targetPath);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path srcFile, BasicFileAttributes arg1)
			throws IOException {
		Files.copy(srcFile, target.resolve(source.relativize(srcFile)),option);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path srcFile, IOException exc)
			throws IOException {
		Installer.log(exc);
		return FileVisitResult.CONTINUE;
	}
}
