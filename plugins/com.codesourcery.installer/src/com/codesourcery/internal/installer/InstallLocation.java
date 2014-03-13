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

import org.eclipse.core.runtime.IPath;

/**
 * Represents an install location.  The location can be referenced .
 */
public class InstallLocation {
	/** Path for install location */
	private IPath path;
	/** References to install location */
	private int referenceCount = 0;
	
	/**
	 * Constructor
	 * 
	 * @param path Path for location
	 */
	public InstallLocation(IPath path) {
		this.path = path;
		addReference();
	}
	
	/**
	 * Returns the path for the location.
	 * 
	 * @return Path
	 */
	public IPath getPath() {
		return path;
	}
	
	/**
	 * Adds a reference to the location.
	 */
	public void addReference() {
		referenceCount ++;
	}
	
	/**
	 * Removes a reference to the location.
	 */
	public void removeReference() {
		if (--referenceCount < 0)
			referenceCount = 0;
	}
	
	/**
	 * Returns if the location has references.
	 * 
	 * @return <code>true</code> if the location is being referenced
	 */
	public boolean hasReferences() {
		return (referenceCount > 0);
	}

	/**
	 * Sets the location reference count.
	 * 
	 * @param referenceCount Reference count
	 */
	public void setReferenceCount(int referenceCount) {
		this.referenceCount = referenceCount;
	}
	
	/**
	 * Returns the location reference count.
	 * 
	 * @return Reference count
	 */
	public int getReferenceCount() {
		return referenceCount;
	}

	@Override
	public String toString() {
		return getPath().toOSString() + ", count = " + 
				Integer.toString(getReferenceCount());
	}
}
