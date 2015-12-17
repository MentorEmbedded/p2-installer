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
package com.codesourcery.installer.os;

import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.Shell;

import com.codesourcery.installer.AbstractInstallPlatformActions;

@SuppressWarnings("restriction")
public class Win32ActionsProvider extends AbstractInstallPlatformActions {
	@Override
	public void bringToFront(Shell shell) {
		/**
		 * SWT doesn't provide a direct means to bring the shell to the front on 
		 * all Windows versions.  <code>Shell.forceActive</code> will not on 
		 * more modern versions of Windows.  This method uses the workaround 
		 * given in:
		 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=192036
		 */
		long hFrom = OS.GetForegroundWindow();
			
		if (hFrom <= 0) {
			OS.SetForegroundWindow(shell.handle);
			return;
		}
		if (shell.handle == hFrom) {
			return;
		}
		int pid = OS.GetWindowThreadProcessId(hFrom, null);
	    int _threadid = OS.GetWindowThreadProcessId(shell.handle, null);
	
	    if (_threadid == pid) {
	      OS.SetForegroundWindow(shell.handle);
	      return;
	    }
	
	    if (pid > 0) {
	      if ( !OS.AttachThreadInput(_threadid, pid, true)) {
	        return;
	      }
	      OS.SetForegroundWindow(shell.handle);
	      OS.AttachThreadInput(_threadid, pid, false);
	    }
	
	    OS.BringWindowToTop(shell.handle);
	    OS.UpdateWindow(shell.handle);
	    OS.SetActiveWindow(shell.handle);
	}
}
