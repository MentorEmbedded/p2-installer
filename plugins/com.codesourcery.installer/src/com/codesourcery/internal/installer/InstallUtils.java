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

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import com.codesourcery.installer.Installer;
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
	 * Returns installable units from a profile.
	 * 
	 * @param profile Profile
	 * @param versions Versions for installable units
	 * @return Installable units
	 */
	public static IInstallableUnit[] getUnitsFromProfile(IProfile profile, IVersionedId[] versions) {
		ArrayList<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
		
		for (IVersionedId version : versions) {
			IQueryResult<IInstallableUnit> query = profile.query(QueryUtil.createIUQuery(version), null);
			Iterator<IInstallableUnit> iter = query.iterator();
			while (iter.hasNext()) {
				units.add(iter.next());
			}
		}
		
		return units.toArray(new IInstallableUnit[units.size()]);
	}
	
	/**
	 * Returns the latest version of an installable unit in a profile.
	 * 
	 * @param profile Profile
	 * @param id Installable unit identifier
	 * @return Latest version found or <code>null</code>
	 */
	public static IInstallableUnit getUnitFromProfile(IProfile profile, String id) {
		IInstallableUnit unit = null;
		IQueryResult<IInstallableUnit> query = profile.query(QueryUtil.createIUQuery(id), null);
		Iterator<IInstallableUnit> iter = query.iterator();
		while (iter.hasNext()) {
			IInstallableUnit foundUnit = iter.next();
			if ((unit == null) || (unit.getVersion().compareTo(unit.getVersion()) > 0)) {
				unit = foundUnit;
			}
		}
		
		return unit;
	}

	/**
	 * Finds an installable unit in all repositories.
	 * 
	 * @param manager Repository meta-data manager
	 * @param spec Version specification
	 * @return Installable unit or <code>null</code>.
	 * @throws CoreException on failure
	 */
	public static IInstallableUnit findUnitAll(IMetadataRepositoryManager manager, IVersionedId spec) throws CoreException {
		String id = spec.getId();
		if (id == null) {
			Installer.fail(InstallMessages.Error_NoId);
		}
		Version version = spec.getVersion();
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, range);
		Iterator<IInstallableUnit> matches = manager.query(query, null).iterator();
		// pick the newest match
		IInstallableUnit newest = null;
		while (matches.hasNext()) {
			IInstallableUnit candidate = matches.next();
			if (newest == null || (newest.getVersion().compareTo(candidate.getVersion()) < 0))
				newest = candidate;
		}
		if (newest == null)
		{
			Installer.fail(InstallMessages.Error_IUNotFound + id);
		}
		return newest;
	}

	/**
	 * Finds an installable unit in local repositories.
	 * 
	 * @param manager Repository meta-data manager
	 * @param spec Version specification
	 * @return Installable unit or <code>null</code>.
	 * @throws CoreException on failure
	 */
	public static IInstallableUnit findUnit(IMetadataRepositoryManager manager, IVersionedId spec) throws CoreException {
		String id = spec.getId();
		if (id == null) {
			Installer.fail(InstallMessages.Error_NoId);
		}
		Version version = spec.getVersion();
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		
		URI[] locations = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_LOCAL);
		List<IMetadataRepository> queryables = new ArrayList<IMetadataRepository>(locations.length);
		for (URI location : locations) {
			queryables.add(manager.loadRepository(location, new NullProgressMonitor()));
		}

		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, range);
		IQueryable<IInstallableUnit> compoundQueryable = QueryUtil.compoundQueryable(queryables);
		IQueryResult<IInstallableUnit> queryResult = compoundQueryable.query(query, new NullProgressMonitor());
		
		Iterator<IInstallableUnit> matches = queryResult.iterator();
		// pick the newest match
		IInstallableUnit newest = null;
		while (matches.hasNext()) {
			IInstallableUnit candidate = matches.next();
			if (newest == null || (newest.getVersion().compareTo(candidate.getVersion()) < 0))
				newest = candidate;
		}
		if (newest == null)
		{
			Installer.fail(InstallMessages.Error_IUNotFound + id);
		}
		return newest;
	}
	
	/**
	 * Finds an installable unit in a repository.
	 * 
	 * @param manager Repository meta-data manager
	 * @param spec Version specification
	 * @return Installable unit or <code>null</code>.
	 * @throws CoreException on failure
	 */
	public static IInstallableUnit findUnit(IMetadataRepository repository, IVersionedId spec) throws CoreException {
		String id = spec.getId();
		if (id == null) {
			Installer.fail(InstallMessages.Error_NoId);
		}
		Version version = spec.getVersion();
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, range);
		IQueryResult<IInstallableUnit> queryResult = repository.query(query, new NullProgressMonitor());
		
		Iterator<IInstallableUnit> matches = queryResult.iterator();
		// pick the newest match
		IInstallableUnit newest = null;
		while (matches.hasNext()) {
			IInstallableUnit candidate = matches.next();
			if (newest == null || (newest.getVersion().compareTo(candidate.getVersion()) < 0))
				newest = candidate;
		}
		
		return newest;
	}

	/**
	 * Returns a file path-friendly string based on the specified file name.
	 * @param fileName 
	 * @return The safe filename
	 */
	public static String makeFileNameSafe(String fileName) {
		
		// Spaces in file paths are not very Linuxy.
		if (!Installer.isWindows()) {
			fileName = fileName.replace(" ", "_");
		}
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
}
