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
package com.codesourcery.internal.installer;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.metadata.expression.IExpression;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import com.codesourcery.installer.Installer;

/**
 * This class provides utility methods for a repository manager.
 */
public class RepositoryManagerAdapter {
	/** Meta-data repository manager */
	private IMetadataRepositoryManager manager;
	
	/**
	 * Constructor
	 * 
	 * @param manager Meta-data repository manager
	 */
	public RepositoryManagerAdapter(IMetadataRepositoryManager manager) {
		this.manager = manager;
	}
	
	/**
	 * @return The repository manager
	 */
	public IMetadataRepositoryManager getManager() {
		return manager;
	}
	
	/**
	 * Finds the latest version of an installable unit in all repositories.
	 * 
	 * @param spec Version specification
	 * @return Installable unit or <code>null</code>.
	 * @throws CoreException on failure
	 */
	public IInstallableUnit findUnitAll(IVersionedId spec) throws CoreException {
		String id = spec.getId();
		if (id == null) {
			Installer.fail(InstallMessages.Error_NoId);
		}
		Version version = spec.getVersion();
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, range);
		Iterator<IInstallableUnit> matches = getManager().query(query, null).iterator();
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
	 * Finds the latest version of an installable unit in local repositories.
	 * 
	 * @param spec Version specification
	 * @return Installable unit or <code>null</code>.
	 * @throws CoreException on failure
	 */
	public IInstallableUnit findUnit(IVersionedId spec) throws CoreException {
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
			queryables.add(getManager().loadRepository(location, new NullProgressMonitor()));
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

		return newest;
	}

	/**
	 * Returns all required members for an installable unit.
	 * 
	 * @param unit Unit
	 * @param members Required members
	 */
	public void findMemberUnits(IInstallableUnit unit, List<IInstallableUnit> members) {
		members.clear();
		// Expression to get required IU's.  This expression matches the expression used in
		// QueryUtil.matchesRequirementsExpression to get category IU members.
		IExpression matchesRequirementsExpression = ExpressionUtil.parse("$0.exists(r | this ~= r)");
		IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(matchesRequirementsExpression, 
				unit.getRequirements());
		IQueryResult<IInstallableUnit> result = getManager().query(query, null);
		Iterator<IInstallableUnit> iter = result.iterator();
		while (iter.hasNext()) {
			members.add(iter.next());
		}
	}
}
