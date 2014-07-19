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

import org.eclipse.equinox.p2.metadata.VersionRange;

import com.codesourcery.installer.IProductRange;

/**
 * Default implementation of {@link com.codesourcery.installer.IProductRange}.
 */
public class ProductRange implements IProductRange {
	/** Product identifier */
	private String id;
	/** Product version range */
	private VersionRange versionRange;

	/**
	 * Constructor
	 * 
	 * @param id Product identifier
	 * @param versionRange Product version range
	 */
	public ProductRange(String id, VersionRange versionRange) {
		this.id = id;
		this.versionRange = versionRange;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public VersionRange getVersionRange() {
		return versionRange;
	}

	@Override
	public String toString() {
		String desc = getId();
		if (getVersionRange() != null) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(' ');
			buffer.append(InstallMessages.Version);
			buffer.append(' ');
			buffer.append(InstallMessages.GreaterThan);
			buffer.append(' ');
			if (getVersionRange().getIncludeMinimum()) {
				buffer.append(InstallMessages.OrEqualTo);
				buffer.append(' ');
			}
			buffer.append(getVersionRange().getMinimum().toString());
			buffer.append(' ');
			buffer.append(InstallMessages.AndLessThan);
			buffer.append(' ');
			if (getVersionRange().getIncludeMaximum()) {
				buffer.append(InstallMessages.OrEqualTo);
				buffer.append(' ');
			}
			buffer.append(getVersionRange().getMaximum().toString());
			
			desc += " " + buffer.toString();
		}
		return desc;
	}
}
