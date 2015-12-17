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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;

import com.codesourcery.installer.LicenseDescriptor;

/**
 * Installer helpers
 */
public class InstallUtils {
	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 * 
	 * @param list List of tokens
	 * @param separator Separator characters
	 * @return Array of tokens
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List<String> result = new ArrayList<String>();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			result.add(token);
		}
		return result.toArray(new String[result.size()]);
	}
	
	/**
	 * Converts an array of strings to a single separator delimited string.
	 *  
	 * @param list Array of strings
	 * @param separator Separator
	 * @return Delimited string
	 */
	public static String getStringFromArray(String[] list, String separator) {
		StringBuilder buffer = new StringBuilder();
		for (int index = 0; index < list.length; index++) {
			if (index != 0) {
				buffer.append(separator);
			}
			buffer.append(list[index]);
		}
		
		return buffer.toString();
	}

	/**
	 * Returns a file path-friendly string based on the specified file name.
	 * @param fileName 
	 * @return The safe filename
	 */
	public static String makeFileNameSafe(String fileName) {
		String newName = fileName.replace("/", "-");
		return newName;
	}
	
	/**
	 * Find license of given name from list of licenses
	 * @param licenses Array of Licenses
	 * @param licenseName Name of License to find
	 * @return LicenseDescriptor or null 
	 */
	public static LicenseDescriptor find(LicenseDescriptor[] licenses, String licenseName) {
		if(licenses == null || licenses.length == 0 || licenseName == null || licenseName.isEmpty())
			return null;
		
		for(LicenseDescriptor ld : licenses) {
			if(licenseName.equals(ld.getLicenseName()))
				return ld;
		}
			
		return null;
	}
	
	/**
	 * Creates a version range.
	 * 
	 * @param range Range
	 * @return Version range
	 */
	public static VersionRange createVersionRange(String range) {
		return new VersionRange(formatVersion(range));
	}
	
	/**
	 * Creates a version.
	 * 
	 * @param version Version specification
	 * @return Version
	 */
	public static Version createVersion(String version) {
		return Version.create(formatVersion(version));
	}
	
	/**
	 * Formats a compatible OmniVersion string from one that may or may not
	 * be compatible.
	 * 
	 * @param version Version string
	 * @return Version
	 */
	public static String formatVersion(String version) {
		version = version.trim();
		version = version.replace('-', '.');
		version = version.replace('_', '.');
		
		return version;
	}
	
	/**
	 * Resolves a path, replacing ~ with the user's home directory path.
	 * 
	 * @param path Path
	 * @return Resolved path
	 */
	public static IPath resolvePath(String path) {
		if (path == null)
			return null;
		
		path = path.trim().replace('\\', File.separatorChar);
		path = path.replace('/', File.separatorChar);
		// Replace home directory
		path = path.replace("~", System.getProperty("user.home"));
		
		return new Path(path);
	}

	/**
	 * Creates an IU description.
	 * 
	 * @param id IU identifier
	 * @param version IU version
	 * @param singleton <code>true</code> if singleton
	 * @param properties IU properties or <code>null</code>
	 * @return IU description
	 */
	public static InstallableUnitDescription createIuDescription(String id, Version version, boolean singleton, 
			Map<String, String> properties) {
		InstallableUnitDescription iuDesc = new MetadataFactory.InstallableUnitDescription();
		iuDesc.setId(id);
		iuDesc.setVersion(version);
		iuDesc.setSingleton(singleton);
		iuDesc.setUpdateDescriptor(MetadataFactory.createUpdateDescriptor(id, new VersionRange(null), IUpdateDescriptor.NORMAL, ""));

		// Set provided capability
		iuDesc.setCapabilities(new IProvidedCapability[] { MetadataFactory
				.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, id, version) });
		
		// Set properties
		if (properties != null) {
			Iterator<Entry<String, String>> propertiesIter = properties.entrySet().iterator();
			while (propertiesIter.hasNext()) {
				Entry<String, String> property = propertiesIter.next();
				iuDesc.setProperty(property.getKey(), property.getValue());
			}
		}
		
		return iuDesc;
	}
	
	/**
	 * Adds installable unit requirements to an installable unit description.
	 * The group property will be set on the description.
	 * 
	 * @param desc Installable unit description
	 * @param units Installable units to add
	 * @param strictVersion <code>true</code> to set strict requirement on the IU versions.
	 */
	public static void addInstallableUnitRequirements(InstallableUnitDescription desc, List<IInstallableUnit> units, boolean strictVersion) {
		ArrayList<IRequirement> requirements = new ArrayList<IRequirement>();
		for (IInstallableUnit unit : units) {
			IRequirement requirement = MetadataFactory.createRequirement(
					IInstallableUnit.NAMESPACE_IU_ID,
					unit.getId(), 
					strictVersion ? 
							new VersionRange(unit.getVersion(), true, unit.getVersion(), true) : 
							new VersionRange(null), 
					null, 
					false, 
					false);
			requirements.add(requirement);
		}
		// Add requirements
		if (!requirements.isEmpty()) {
			desc.addRequirements(requirements);
			desc.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, Boolean.TRUE.toString());
		}
	}

	/**
	 * Returns if an IU's requirements are satisfied by a 
	 * specified set of IUs.
	 * 
	 * @param unit Unit to check
	 * @param candidates Units for requirements
	 * @return <code>true</code> if units requirements are satisified
	 */
	public boolean isIURequirementsSatisfied(IInstallableUnit unit, List<IInstallableUnit> candidates) {
		boolean satisfied = true;
		// Get unit requirements
		Iterator<IRequirement> requirements = unit.getRequirements().iterator();
		while (requirements.hasNext()) {
			IRequirement requirement = requirements.next();
			boolean requirementSatisfied = false;
			// Check if any of the units satisfy the units requirement
			for (IInstallableUnit candidate : candidates) {
				if (candidate.satisfies(requirement)) {
					requirementSatisfied = true;
					break;
				}
			}
			// At least one requirement is not satisfied
			if (!requirementSatisfied) {
				satisfied = false;
				break;
			}
		}
		
		return satisfied;
	}
}
