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
package com.codesourcery.installer.console;

import java.util.ArrayList;
import java.util.List;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * This class can be used to present a list of check box items
 * that can be selected in the console.
 * 1. [x] Item 1
 * 2. [x] Item 2
 * ...
 *
 * Call {@link #addItem(String, Object, boolean, boolean)} to add items
 * for the list.
 * 
 * Example:
 *   private ConsoeListPrompter<String> prompter;
 *   
 *   public MyClass() {
 *     prompter = new ConsoleListPrompter<String>("Select an item:");
 *     prompter.addItem("Item 1", item1Data, true, true);
 *     prompter.addItem("Item 2", item2Data, true, true);
 *   }
 *   
 *   public String getConsoleResponse(String input) throws IllegalArgumentException {
 *     String response = prompter.getResponse(input);
 *     if (response == null) {
 *       ArrayList<String> data = new ArrayList<String>();
 *       prompter.getSelectedDatas(data);
 *       // Handle data...
 *     }
 *     
 *     return response;
 *   }
 *
 * The list can also be set to select a single option.
 * 1. Item 1
 * 2. Item 2
 * 
 * In this case use {@link #addItem(String, Object)} and get the selected
 * item using ...
 */
public class ConsoleListPrompter<T> implements IInstallConsoleProvider {
	/** Items */
	private ArrayList<Item<T>> items = new ArrayList<Item<T>>();
	/** Console message */
	private String message;
	/** <code>true</code> if only a single option can be selected */
	private boolean single = false;

	/**
	 * Constructor
	 * 
	 * @param message Message to display
	 */
	public ConsoleListPrompter(String message) {
		this.message = message;
	}

	/**
	 * Constructor
	 * 
	 * @param message Message to display
	 * @param single <code>true</code> if only a single option can be selected
	 */
	public ConsoleListPrompter(String message, boolean single) {
		this(message);
		this.single = single;
	}

	/**
	 * Returns if only a single selection can be made.
	 * 
	 * @return <code>true</code> if single selection
	 */
	public boolean isSingleSelection() {
		return single;
	}
	
	/**
	 * Returns the message.
	 * 
	 * @return Message
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Adds an item to the list.
	 * 
	 * @param name Name to display for the item
	 * @param data Data associated with the item
	 * @param selected <code>true</code> if item should be checked initially
	 * @param optional <code>true</code> if the item can be changed
	 * @return Index of item
	 */
	public int addItem(String name, T data, boolean selected, boolean optional) {
		Item<T> item = new Item<T>(name, data, selected, optional);
		items.add(item);
		return items.size() - 1;
	}
	
	/**
	 * Adds an item to the list.
	 * 
	 * @param name Name to display for the item
	 * @param data Data associated with the item
	 * @return Index of item
	 */
	public int addItem(String name, T data) {
		return addItem(name, data, false, true);
	}

	/**
	 * Removes an item from the list.
	 * 
	 * @param index Index of item to remove
	 */
	public void removeItem(int index) {
		items.remove(index);
	}

	@Override
	public String getConsoleResponse(String input) throws IllegalArgumentException {
		String response = null;
		
		if (input == null) {
			response = toString();
		}
		else {
			// Continue
			if (input.isEmpty()) {
				response = null;
			}
			// Toggle item
			else {
				int index = Integer.parseInt(input) - 1;
				// Invalid input
				if ((index < 0) || (index >= items.size())) {
					response = InstallMessages.Error_InvalidSelection;
				}
				else {
					Item<T> item = items.get(index);
					// Single selection
					if (isSingleSelection()) {
						item.setSelected(true);
						response = null;
					}
					// Multiple selection
					else {
						// Item can be changed
						if (item.isOptional()) {
							item.setSelected(!item.isSelected());
							response = toString();
						}
						else {
							response = InstallMessages.Error_ItemCantChange;
						}
					}
				}
			}
		}
		
		return response;
	}

	/**
	 * Returns the number of items in the list.
	 * 
	 * @return Number of items
	 */
	public int getItemCount() {
		return items.size();
	}
	
	/**
	 * Returns an item name.
	 * 
	 * @param index Index of the item
	 * @return Item name
	 */
	public String getItemName(int index) {
		Item<T> item = items.get(index);
		return item.getName();
	}
	
	/**
	 * Returns the item data.
	 * 
	 * @param index Index of the item
	 * @return Item data
	 */
	public T getItemData(int index) {
		Item<T> item = items.get(index);
		return item.getData();
	}
	
	/**
	 * Returns the data for all selected items.
	 * 
	 * @param list List to fill with item data
	 */
	public void getSelectedData(List<T> list) {
		list.clear();
		for (Item<T> item : items) {
			if (item.isSelected()) {
				list.add(item.getData());
			}
		}
	}
	
	/**
	 * Returns if an item is selected.
	 * 
	 * @param index Index of item
	 * @return <code>true</code> if item is selected
	 */
	public boolean isItemSelected(int index) {
		Item<T> item = items.get(index);
		return item.isSelected();
	}
	
	/**
	 * Sets an item selected.
	 * 
	 * @param index Index of item
	 * @param selected <code>true</code> to select item.
	 */
	public void setItemSelected(int index, boolean selected) {
		Item<T> item = items.get(index);
		item.setSelected(selected);
	}
	
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		
		if (getMessage() != null) {
			buffer.append(getMessage());
			buffer.append("\n\n");
		}
		
		int index = 0;
		for (Item<T> item : items) {
			buffer.append(String.format("%4d. %s\n", ++index, item.toString()));
		}
		
		buffer.append("\n");
		buffer.append(InstallMessages.ConsoleItemsTogglePrompt);
		
		return buffer.toString();
	}

	/**
	 * Internal class to store item attributes.
	 */
	private class Item<TT> {
		/** Item name */
		private String name;
		/** <code>true</code> if item is selected */
		private boolean selected;
		/** <code>true</code> if item can change */
		private boolean optional;
		/** Item data */
		private TT data;
		
		/**
		 * Constructor
		 * 
		 * @param name Item name
		 * @param data Data for item
		 * @param selected <code>true</code> to select item by default
		 * @param optional <code>true</code> if item can change
		 */
		public Item(String name, TT data, boolean selected, boolean optional) {
			this.name = name;
			this.data = data;
			this.selected = selected;
			this.optional = optional;
		}
		
		/**
		 * Returns the item name.
		 * 
		 * @return Name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Returns the item data.
		 * 
		 * @return Data
		 */
		public TT getData() {
			return data;
		}
		
		/**
		 * Returns if the item is selected.
		 * 
		 * @return <code>true</code> if selected
		 */
		public boolean isSelected() {
			return selected;
		}
		
		/**
		 * Sets the item selected.
		 * 
		 * @param selected <code>true</code> selected
		 */
		public void setSelected(boolean selected) {
			this.selected = selected;
		}
		
		/**
		 * Returns if the item can change.
		 * 
		 * @return <code>true</code> if item can change
		 */
		public boolean isOptional() {
			return optional;
		}
		
		@Override
		public String toString() {
			// Single selection
			if (isSingleSelection()) {
				return getName();
			}
			// Multiple selection
			else {
				// [<'x' if selected>] name
				return "[" +
						(isSelected() ? "*" : " ") +
						"]" + 
						getName();
			}
		}
	}
}
