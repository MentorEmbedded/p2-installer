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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.engine.Operand;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * Extension of org.eclipse.equinox.internal.p2.director.SimplePlanner that orders
 * plan operands to install and update IUs according dependencies between them.
 * Does not perform removal ordering.
 * 
 * See reason for this planner here:
 * http://dev.eclipse.org/mhonarc/lists/p2-dev/msg05399.html
 */
@SuppressWarnings("restriction")
public class OrderedPlanner extends SimplePlanner {
	/**
	 * Constructor
	 * 
	 * @param agent Provisioning agent
	 */
	public OrderedPlanner(IProvisioningAgent agent) {
		super(agent);
	}

	@Override
	public IProvisioningPlan getProvisioningPlan(IProfileChangeRequest request,
			ProvisioningContext context, IProgressMonitor monitor) {
		IProvisioningPlan plan = super.getProvisioningPlan(request, context, monitor);
		if (plan.getStatus().isOK()) {
			return orderPlan(plan);
		}
		return plan;
	}
	
	/**
	 * Order operands in correct IProvisioningPlan to install IUs according dependencies between them. 
	 * @param plan right (status=ok) provisioning plan created by solver
	 * @return new IProvisioningPlan that is the same as original plan, but operands are ordered according to dependecnies between IUs  
	 */
	public IProvisioningPlan orderPlan(IProvisioningPlan plan) {
		Operand[] ops = ((ProvisioningPlan) plan).getOperands();
		ArrayList<Operand> newOps = new ArrayList<Operand>(ops.length);
		ArrayList<InstallableUnitOperand> operandsToSort = new ArrayList<InstallableUnitOperand>(ops.length);
		ArrayList<Operand> misc = new ArrayList<Operand>(ops.length);
		// First of all we are interested only in additions, not removals, so sort out removals to keep them unchanged
		for (Operand op : ops) {
			if (!(op instanceof InstallableUnitOperand)) {
				// Unexpected, but still
				misc.add(op);
				continue;
			}
			InstallableUnitOperand iuo = ((InstallableUnitOperand) op);
			if (iuo.second() == null) {
				// This is removal, put it unchanged
				newOps.add(iuo);
			} else if (iuo.first() == null) {
				// This is addition
				operandsToSort.add(iuo);
			} else {
				// This is update, sort it too
				operandsToSort.add(iuo);
			}
		}

		// Create operands -> IUs mappings
		Map<IInstallableUnit, InstallableUnitOperand> iuToOperand = new HashMap<IInstallableUnit, InstallableUnitOperand>();
		for (InstallableUnitOperand o : operandsToSort) {
			iuToOperand.put(o.second(), o);
		}

		// Build dependencies, we are only interested in dependencies between additions
		IInstallableUnit[] iusToSort = iuToOperand.keySet().toArray(new IInstallableUnit[0]);
		IQueryable<IInstallableUnit> allIUs = new QueryableArray(iusToSort);

		// Build index IU ID -> IU for additions
		Map<String, IInstallableUnit> idToIU = new HashMap<String, IInstallableUnit>();
		for (IInstallableUnit iu : iuToOperand.keySet()) {
			idToIU.put(iu.getId(), iu);
		}

		// Build graph representation in and out edges per IU,
		// edge from B to A means that B is required to install A
		Map<IInstallableUnit, ArrayList<IInstallableUnit>> outNodesPerIU = new HashMap<IInstallableUnit, ArrayList<IInstallableUnit>>();
		Map<IInstallableUnit, ArrayList<IInstallableUnit>> inNodesPerIU = new HashMap<IInstallableUnit, ArrayList<IInstallableUnit>>();

		for (IInstallableUnit iu : iuToOperand.keySet()) {
			ArrayList<IInstallableUnit> inNodes = new ArrayList<IInstallableUnit>();
			inNodesPerIU.put(iu, inNodes);
			ArrayList<IInstallableUnit> outNodes = new ArrayList<IInstallableUnit>();
			outNodesPerIU.put(iu, outNodes);

			Collection<IRequirement> req = iu.getRequirements();
			for (IRequirement r : req) {
				// This is workaround, because simple allIUs.query(QueryUtil.createMatchQuery(r.getMatches()), null) 
				// doesn't work properly. See comment right below.
				IQueryResult<IInstallableUnit> matches = allIUs.query(QueryUtil.createMatchQuery(r.getMatches()), null);

				// The situation where there are more that one candidates is an error, ignore it,
				// because for IUs which are bundles (and therefore contains p2 metadata fragment),
				// there is dependency from org.eclipse.equinox.p2.eclipse.type
				// for which query above (QueryUtil.createMatchQuery(r.getMatches())), returns tons of candidates.
				int matchesSize = 0;
				for (Iterator<IInstallableUnit> iterator = matches.iterator(); iterator.hasNext(); matchesSize++, iterator.next());
				if (matchesSize == 1) {
					IInstallableUnit requires = matches.iterator().next();
					// Do not allow self-references to avoid confusing topolgical sort
					if (!requires.equals(iu)) {
						inNodes.add(requires);
					}
				}
			}
		}

		// Fill in out nodes, they are used in topological sort:
		for (IInstallableUnit to : inNodesPerIU.keySet()) {
			ArrayList<IInstallableUnit> inNodes = inNodesPerIU.get(to);
			for (IInstallableUnit from : inNodes) {
				ArrayList<IInstallableUnit> outs = outNodesPerIU.get(from);
				outs.add(to);
			}
		}

		// Implement topological sort of graph represented by inNodesPerIU and outNodesPerIU
		ArrayList<IInstallableUnit> sortedIUs = new ArrayList<IInstallableUnit>();

		// s <- Set of all nodes with no incoming edges
		HashSet<IInstallableUnit> s = new HashSet<IInstallableUnit>();
		for (IInstallableUnit iu : inNodesPerIU.keySet()) {
			ArrayList<IInstallableUnit> ins = inNodesPerIU.get(iu);
			if (ins.size() == 0) {
				s.add(iu);
			}
		}

		//while s is non-empty do
		while (!s.isEmpty()) {
			//remove a node n from S
			IInstallableUnit n = s.iterator().next();
			s.remove(n);
			//insert n into sortedIUs
			sortedIUs.add(n);

			ArrayList<IInstallableUnit> outNodes = outNodesPerIU.get(n);

			//for each node m with an edge from n to m do
			for (IInstallableUnit m : outNodes) {
				//remove edge from the graph
				ArrayList<IInstallableUnit> mInNodes = inNodesPerIU.get(m);
				mInNodes.remove(n);
				//if m has no other incoming edges then insert m into S
				if (mInNodes.isEmpty()) {
					s.add(m);
				}
			}
		}
		// Consistency check if all edges are removed, 
		// should never find a cycle since the plan status is OK
		boolean cycle = false;
		for (IInstallableUnit iu : inNodesPerIU.keySet()) {
			ArrayList<IInstallableUnit> inNodes = inNodesPerIU.get(iu);
			if (!inNodes.isEmpty()) {
				cycle = true;
				break;
			}
		}
		// Merge them back to the end of the list and create new plan
		ArrayList<InstallableUnitOperand> sortedOperands = new ArrayList<InstallableUnitOperand>();
		for (IInstallableUnit iu : sortedIUs) {
			sortedOperands.add(iuToOperand.get(iu));
		}
		newOps.addAll(sortedOperands);
		newOps.addAll(misc);
		
		// If there are cycles, just add any remaining ops to the end of list
		if (cycle) {
			for (Operand op : ops) {
				if (!newOps.contains(op)) {
					newOps.add(op);
				}
			}
		}

		// Return new sorted plan
		ProvisioningPlan np = new ProvisioningPlan(plan.getStatus(), plan.getProfile(), newOps.toArray(new Operand[newOps.size()]), plan.getContext(), plan.getInstallerPlan());
		return np;
	}
}

