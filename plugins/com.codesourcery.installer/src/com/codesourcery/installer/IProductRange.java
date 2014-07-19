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
package com.codesourcery.installer;

import org.eclipse.equinox.p2.metadata.VersionRange;

/**
 * Specifies a product and optional version range.
 */
public interface IProductRange {
	/**
	 * Returns the product identifier.
	 * 
	 * @return Identifier
	 */
	public String getId();
	
	/**
	 * Returns the product version range.
	 * 
	 * @return Version range or <code>null</code> for any version
	 */
	public VersionRange getVersionRange();
}
