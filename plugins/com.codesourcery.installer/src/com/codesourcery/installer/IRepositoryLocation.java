/*******************************************************************************
 *  Copyright (c) 2015 Mentor Graphics and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mentor Graphics - initial API and implementation
 *******************************************************************************/
package com.codesourcery.installer;

import java.net.URI;

/**
 * Specifies the locations of a set of P2 repositories to be used in the installation.
 */
public interface IRepositoryLocation {
	/**
	 * @return Returns the location identifier
	 */
	public String getId();
	
	/**
	 * @return Returns the locations of the meta-data repositories.
	 */
	public URI[] getMetadataLocations();
	
	/**
	 * @return Returns the locations of the artifact repositories.
	 */
	public URI[] getArtifactLocations();
}
