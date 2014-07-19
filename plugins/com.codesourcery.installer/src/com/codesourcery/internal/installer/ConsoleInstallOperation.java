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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.ui.pages.ProductsPage;
import com.codesourcery.internal.installer.ui.pages.SummaryPage;

/**
 * An install operation that works with the console.
 */
public class ConsoleInstallOperation extends InstallOperation {
	/** End of line */
	private static final String EOL = System.getProperty("line.separator");
	/** Maximum number of console lines to display before pausing */
	private static final int DEFAULT_CONSOLE_LINES = 25;
	/** Progress monitor */
	private ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
	/** Number of console lines to print at once */
	private int maxLines = DEFAULT_CONSOLE_LINES;
	
	/**
	 * Constructor
	 */
	public ConsoleInstallOperation() {
	}
	
	/**
	 * Sets the maximum number of console lines to print at once.
	 * If more than the maximum is printed, it will be paginated and require
	 * the user to press <code>ENTER</code> to continue.
	 * 
	 * @param maxLines Maximum number of lines
	 */
	public void setMaxLines(int maxLines) {
		this.maxLines = maxLines;
	}
	
	/**
	 * Returns the maximum number of lines to print at once.
	 * 
	 * @return Maximum number of lines
	 */
	public int getMaxLines() {
		return maxLines;
	}
	
	/**
	 * Returns the progress monitor to use for operations.
	 * 
	 * @param installing <code>true</code> if installing, <code>false</code> if
	 * uninstalling
	 * @return Progress monitor
	 */
	private IProgressMonitor getProgressMonitor(boolean installing) {
		progressMonitor.setInstalling(installing);
		return progressMonitor;
	}
	
	/**
	 * Prints to the console. If the number of lines provided is greater
	 * than the maximum allowed, the printing will be paginated.
	 * 
	 * @param text Text to print
	 * @throws IOException on failure
	 */
	private void printConsole(String text) throws IOException {
		BufferedReader consoleReader = null;
		BufferedReader responseReader = null;
		try {
			consoleReader = new BufferedReader(new InputStreamReader(System.in));
			responseReader = new BufferedReader(new StringReader(text));
			StringBuilder output = new StringBuilder();
			String responseLine;
			int count = 0;
			while ((responseLine = responseReader.readLine()) != null) {
				output.append(responseLine);
				output.append(EOL);
				// Limit console text to a maximum and prompt to continue
				if (++ count == getMaxLines()) {
					count = 0;
					System.out.print(output.toString());
					System.out.println(EOL);
					System.out.println(InstallMessages.ConsolePressEnter);
					output = new StringBuilder();
					consoleReader.readLine();
				}
			}
			System.out.print(output.toString());
		}
		finally {
			if (responseReader != null) {
				responseReader.close();
			}
		}
	}
	
	@Override
	public void run() {
		IStatus status = Status.OK_STATUS;
		try {
			// Installation data
			InstallData installData = new InstallData();

			// Loop through the install pages
			IInstallWizardPage[] wizardPages = Installer.getDefault().getInstallManager().getWizardPages();
			
			// Add summary page
			IInstallWizardPage[] pages = new IInstallWizardPage[wizardPages.length + 2];
			System.arraycopy(wizardPages, 0, pages, 0, wizardPages.length);
			pages[pages.length - 2] = new SummaryPage("summaryPage", InstallMessages.SummaryPageTitle);
			
			// Add uninstall products page
			ProductsPage productsPage = new ProductsPage("productsPage", InstallMessages.ProductsPageTitle, InstallMessages.ProductsMessage);
			productsPage.setMessage(InstallMessages.SelectProductsToUninstall);
			pages[pages.length - 1] = productsPage;
			
			// Show wizard pages
			for (IInstallWizardPage page : pages) {
				if (!page.isSupported())
					continue;
				
				// Page supports console presentation
				if (page instanceof IInstallConsoleProvider) {
					// Set active
					page.setActive(installData);
					
					IInstallConsoleProvider consolePage = (IInstallConsoleProvider)page;
					// Get initial page text
					String response = consolePage.getConsoleResponse(null);
					
					while (response != null) {
						printConsole(EOL);
						// Print response
						printConsole(response);

						// Console input reader
						BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
						// Get input
						String input = consoleReader.readLine();
						if (input == null)
							break;
						// Get page next response
						response = consolePage.getConsoleResponse(input);
					}
					// Save page data
					page.saveInstallData(installData);
				}
			}
			
			// Install
			if (Installer.getDefault().getInstallManager().getInstallMode().isInstall()) {
				Installer.getDefault().getInstallManager().install(installData, getProgressMonitor(true));
			}
			// Uninstall
			else {
				IInstallProduct[] products = productsPage.getSelectedProducts();
				if (products.length > 0) {
					Installer.getDefault().getInstallManager().uninstall(products, getProgressMonitor(false));
				}
			}
		}
		// Install aborted
		catch (IllegalArgumentException e) {
			status = new Status(IStatus.CANCEL, Installer.ID, 0, e.getLocalizedMessage(), null);
			System.out.println(e.getMessage());
		}
		catch (Exception e) {
			status = new Status(IStatus.ERROR, Installer.ID, 0, e.getLocalizedMessage(), e);
			Installer.log(e);
			System.err.println(e.getLocalizedMessage());
		}
		
		// Write status
		writeStatus(status);
	}

	/**
	 * Console progress monitor
	 */
	class ConsoleProgressMonitor implements IProgressMonitor {
		private boolean canceled;
		private boolean installing;
		
		/**
		 * Constructor
		 */
		public ConsoleProgressMonitor() {
		}

		/**
		 * Sets installing or uninstalling.
		 * 
		 * @param installing <code>true</code> if installing, <code>false</code>
		 * if uninstalling.
		 */
		public void setInstalling(boolean installing) {
			this.installing = installing;
		}
		
		/**
		 * Returns if installing.
		 * 
		 * @return <code>true</code> if installing
		 */
		private boolean isInstalling() {
			return installing;
		}
		
		@Override
		public void beginTask(String name, int totalWork) {
			System.out.print(isInstalling() ? 
				InstallMessages.ConsoleInstalling : 
				InstallMessages.ConsoleUninstalling);
		}

		@Override
		public void done() {
			System.out.print(EOL);
		}

		@Override
		public void internalWorked(double work) {
		}

		@Override
		public boolean isCanceled() {
			return canceled;
		}

		@Override
		public void setCanceled(boolean value) {
			canceled = value;
		}

		@Override
		public void setTaskName(String name) {
			Installer.log(name);
		}

		@Override
		public void subTask(String name) {
			Installer.log(name);
		}

		@Override
		public void worked(int work) {
			// Append progress tick
			System.out.print('.');
		}
	}
}
