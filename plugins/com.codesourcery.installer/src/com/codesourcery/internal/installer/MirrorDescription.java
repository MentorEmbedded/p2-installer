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

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.codesourcery.installer.Installer;

/**
 * Stores information about a mirror.
 */
public class MirrorDescription {
	/** Root element */
	private static final String ELEMENT_ROOT = "mirrorInfo";
	/** Product ID attribute */
	private static final String ATTRIBUTE_PRODUCT_ID = "productId";
	/** Product version attribute */
	private static final String ATTRIBUTE_PRODUCT_VERSION = "productVersion";
	/** Product ID */
	private String productId;
	/** Product version */
	private String productVersion;
	
	/**
	 * Constructs a mirror description.
	 */
	public MirrorDescription() {
	}
	
	/**
	 * Sets the product identifier.
	 * 
	 * @param productId Product identifier
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}
	
	/**
	 * @return Returns the product identifier
	 */
	public String getProductId() {
		return productId;
	}

	/**
	 * Sets the product version.
	 * 
	 * @param productVersion Product version
	 */
	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}

	/**
	 * @return Returns the product version.
	 */
	public String getProductVersion() {
		return productVersion;
	}
	
	/**
	 * Loads the mirror description from a file.
	 * 
	 * @param file Description file
	 * @throws CoreException on failure
	 */
	public void load(File file) throws CoreException {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder.parse(file);
			
			NodeList nodes = document.getElementsByTagName(ELEMENT_ROOT);
			if (nodes.getLength() == 1) {
				Element root = (Element)nodes.item(0);
				setProductId(root.getAttribute(ATTRIBUTE_PRODUCT_ID));
				setProductVersion(root.getAttribute(ATTRIBUTE_PRODUCT_VERSION));
			}
		}
		catch (Exception e) {
			Installer.fail(e);
		}
		
	}

	/**
	 * Saves the mirror description to a file.
	 * 
	 * @param file Description file
	 * @throws CoreException on failure
	 */
	public void save(File file) throws CoreException {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder.newDocument();
			
			Element root = document.createElement(ELEMENT_ROOT);
			document.appendChild(root);
			
			root.setAttribute(ATTRIBUTE_PRODUCT_ID, getProductId());
			root.setAttribute(ATTRIBUTE_PRODUCT_VERSION, getProductVersion());
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			
			StreamResult result = new StreamResult(file);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
		}
		catch (Exception e) {
			Installer.fail(e);
		}
	}
	
	
}
