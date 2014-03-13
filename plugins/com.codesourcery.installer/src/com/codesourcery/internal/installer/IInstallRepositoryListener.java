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

import com.codesourcery.installer.ui.IInstallComponent;

/**
 * A listener that is notified of changes to the install repositories. 
 */
public interface IInstallRepositoryListener {
	/** Repository load status */
	public enum RepositoryStatus {
		/** Repository loading started */
		loadingStarted,
		/** Repository loading completed */
		loadingCompleted
	}

	/**
	 * Called with the status of repository loading.
	 * <ul>
	 * <li>loadingStarted - Loading of repositories has started</li>
	 * <li>oadingCompleted - Loading of repositories has completed</li>
	 * </ul>
	 * 
	 * @param status Loading status
	 */
	public void repositoryStatus(RepositoryStatus status);
	
	/**
	 * Called when a repository is loaded.
	 * 
	 * @param location Repository location
	 * @param components Components loaded from repository
	 */
	public void repositoryLoaded(URI location, IInstallComponent[] components);
	
	/**
	 * Called when a repository fails to load due to an error.
	 * 
	 * @param location Location of repository
	 * @param errorMessage Error message
	 */
	public void repositoryError(URI location, String errorMessage);
}
