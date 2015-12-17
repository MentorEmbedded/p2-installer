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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.ui.InstallWizardPage;
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
	 * @param installMode Install mode
	 * @return Progress monitor
	 */
	private IProgressMonitor getProgressMonitor(IInstallMode installMode) {
		progressMonitor.setInstallMode(installMode);
		return progressMonitor;
	}
	
	/**
	 * Reads input from the console.
	 * 
	 * @param reader Console reader
	 * @return Console input or <code>null</code>
	 * @throws IllegalArgumentException if installation is cancelled
	 * @throws IOException on failure
	 */
	private String readConsole(BufferedReader reader) throws IllegalArgumentException, IOException {
		String input = reader.readLine();
		if ((input != null) && input.trim().toLowerCase().equals("exit")) {
			throw new IllegalArgumentException(InstallMessages.SetupCancelled);
		}
		
		return input;
	}
	
	/**
	 * Prints to the console. If the number of lines provided is greater
	 * than the maximum allowed, the printing will be paginated.
	 * 
	 * @param text Text to print
	 * @throws IllegalArgumentException if installation is cancelled
	 * @throws IOException on failure
	 */
	private void printConsole(String text) throws IllegalArgumentException, IOException {
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
					readConsole(consoleReader);
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
		boolean firstPage = true;
		try {
			IInstallWizardPage[] wizardPages = Installer.getDefault().getInstallManager().getWizardPages();
			IInstallData installData = Installer.getDefault().getInstallManager().getInstallData();
			
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
					// If this is the first page, print the exit prompt
					if (firstPage) {
						firstPage = false;
						response = InstallMessages.ConsoleExitPrompt + response;
					}
					
					while (response != null) {
						printConsole(EOL);
						// Print response
						printConsole(response);

						// Console input reader
						BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
						// Get input
						String input = readConsole(consoleReader);
						if (input == null)
							break;
						// Get page next response
						response = consolePage.getConsoleResponse(input);
						// Save page data
						if (response == null) {
							try {
								page.saveInstallData(installData);
							}
							catch (CoreException e) {
								printConsole(e.getStatus().getMessage());
								// Start page over
								response = consolePage.getConsoleResponse(null);
							}
						}
					}
				}
			}
			
			IInstallMode installMode = Installer.getDefault().getInstallManager().getInstallMode();
			
			// Install
			if (installMode.isInstall()) {
				Installer.getDefault().getInstallManager().install(getProgressMonitor(installMode));

				try {
					printConsole(EOL);
					// Mirror
					if (Installer.getDefault().getInstallManager().getInstallMode().isMirror()) {
						printConsole(NLS.bind(InstallMessages.ConsoleMirrorComplete0, getInstallManager().getInstallDescription().getProductName()));
					}
					// Install
					else {
						printConsole(NLS.bind(InstallMessages.ConsoleInstallationComplete0, getInstallManager().getInstallDescription().getProductName()));
					}
					String installText = getInstallManager().getInstallDescription().getText(IInstallDescription.TEXT_INSTALL_ADDENDUM, null);
					if (installText != null) {
						printConsole(EOL);
						printConsole(InstallWizardPage.formatConsoleMessage(installText));
					}
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
			// Uninstall
			else {
				IInstallProduct[] products = productsPage.getSelectedProducts();
				if (products.length > 0) {
					Installer.getDefault().getInstallManager().uninstall(products, getProgressMonitor(installMode));
					
					printConsole(EOL);
					printConsole(InstallMessages.ConsoleUninstallationComplete);
					try {
						for (IInstallProduct product : products) {
							String uninstallText = product.getProperty(IInstallProduct.PROPERTY_UNINSTALL_TEXT);
							if (uninstallText != null) {
								printConsole(InstallWizardPage.formatConsoleMessage(uninstallText));
							}
						}
					}
					catch (Exception e) {
						Installer.log(e);;
					}
				}
			}
		}
		// Install aborted
		catch (IllegalArgumentException e) {
			status = new Status(IStatus.CANCEL, Installer.ID, 0, e.getLocalizedMessage(), null);
			showError(e.getMessage());
			cleanupInstallation();
		}
		catch (Exception e) {
			status = new Status(IStatus.ERROR, Installer.ID, 0, e.getLocalizedMessage(), e);
			Installer.log(e);
			showError(e.getLocalizedMessage());
			cleanupInstallation();
		}
		
		// Write status
		writeStatus(status);
	}

	/**
	 * Console progress monitor
	 */
	class ConsoleProgressMonitor implements IProgressMonitor {
		private boolean canceled;
		private IInstallMode installMode;
		
		/**
		 * Constructor
		 */
		public ConsoleProgressMonitor() {
		}

		/**
		 * Sets the install mode.
		 * 
		 * @param installMode Install mode
		 */
		public void setInstallMode(IInstallMode installMode) {
			this.installMode = installMode;
		}
		
		/**
		 * @return The install mode.
		 */
		private IInstallMode getInstallMode() {
			return installMode;
		}
		
		@Override
		public void beginTask(String name, int totalWork) {
			System.out.print(getInstallMode().isInstall() ? 
				(getInstallMode().isMirror() ? InstallMessages.ConsoleSaving : InstallMessages.ConsoleInstalling) : 
				InstallMessages.ConsoleUninstalling);
		}

		@Override
		public void done() {
			System.out.print(EOL);
		}

		@Override
		public void internalWorked(double work) {
			// Append progress tick
			System.out.print('.');
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
		}

		@Override
		public void subTask(String name) {
		}

		@Override
		public void worked(int work) {
		}
	}

	@Override
	public void showError(String message) {
		System.err.println(message);
	}
}
