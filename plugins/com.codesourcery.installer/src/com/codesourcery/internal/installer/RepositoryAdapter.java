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
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * This class provides utility methods for a meta-data repository. 
 */
public class RepositoryAdapter {
	/** Meta-data repository */
	private IMetadataRepository repository;
	
	/**
	 * Constructor
	 * 
	 * @param repository meta-data repository
	 */
	public RepositoryAdapter(IMetadataRepository repository) {
		this.repository = repository;
	}
	
	/**
	 * @return The meta-data repository
	 */
	public IMetadataRepository getRepository() {
		return repository;
	}

	/**
	 * Finds the latest version of an installable unit in a repository.
	 * 
	 * @param spec Version specification
	 * @return Installable unit or <code>null</code>.
	 * @throws CoreException on failure
	 */
	public IInstallableUnit findUnit(IVersionedId spec) {
		String id = spec.getId();
		Version version = spec.getVersion();
		VersionRange range = VersionRange.emptyRange;
		if (version != null && !version.equals(Version.emptyVersion))
			range = new VersionRange(version, true, version, true);
		
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id, range);
		IQueryResult<IInstallableUnit> queryResult = getRepository().query(query, new NullProgressMonitor());
		
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
	 * Finds all required members for an installable unit.
	 * 
	 * @param parent Parent installable unit
	 * @param members Filled with the member installable units
	 */
	public void findMemberUnits(IInstallableUnit parent, List<IInstallableUnit> members) {
		members.clear();
		// Expression to get required IU's.  This expression matches the expression used in
		// QueryUtil.matchesRequirementsExpression to get category IU members.
		IExpression matchesRequirementsExpression = ExpressionUtil.parse("$0.exists(r | this ~= r)");
		IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery(matchesRequirementsExpression, 
				parent.getRequirements());
		IQueryResult<IInstallableUnit> result = getRepository().query(query, null);
		Iterator<IInstallableUnit> iter = result.iterator();
		while (iter.hasNext()) {
			members.add(iter.next());
		}
	}
}
