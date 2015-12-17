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
import java.util.Arrays;
import java.util.List;

import org.eclipse.osgi.util.NLS;

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
	/** Checked item prefix */
	private String checkedPrefix = "[*]";
	/** Unchecked item prefix */
	private String uncheckedPrefix = "[ ]";
	/** Items */
	private ArrayList<Item<T>> items = new ArrayList<Item<T>>();
	/** Console message */
	private String message;
	/** <code>true</code> if only a single option can be selected */
	private boolean single = false;
	/** Default item */
	private Item<T> defaultItem = null;

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
	 * Sets a default item.  This is only used if a single
	 * selection is allowed.
	 * 
	 * @param index Index of the default item or <code>-1</code>
	 */
	public void setDefaultItem(int index) {
		if (index == -1) {
			this.defaultItem = null;
		}
		else {
			this.defaultItem = items.get(index);
		}
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
	 * Sets the string used for checked and un-checked items.
	 * The default is "[*]" and "[ ]".
	 * 
	 * @param checked Checked string
	 * @param unchecked Un-checked string
	 */
	public void setCheckedString(String checked, String unchecked) {
		checkedPrefix = checked;
		uncheckedPrefix = unchecked;
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
	 * @param index Index of parent item
	 * @param name Name to display for the item
	 * @param data Data associated with the item
	 * @param selected <code>true</code> if item should be checked initially
	 * @param optional <code>true</code> if the item can be changed
	 * @return Index of item
	 */
	public int addItem(int index, String name, T data, boolean selected, boolean optional) {
		Item<T> item = new Item<T>(items.get(index), name, data, selected, optional);
		items.add(item);
		return items.size() - 1;
	}

	/**
	 * Returns the index of the first item with data.
	 * 
	 * @param data Data
	 * @return Index of item or <code>-1</code>
	 */
	public int getItemIndex(T data) {
		int index = -1;
		for (int i = 0; i < items.size(); i ++) {
			if (data.equals(items.get(i).getData())) {
				index = i;
				break;
			}
		}
		
		return index;
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
				// If single selection
				if (isSingleSelection()) {
					// If there is a default then select it
					if (defaultItem != null) {
						defaultItem.setSelected(true);
					}
					// Else prompt user to select an item
					else {
						response = NLS.bind(InstallMessages.Error_InvalidSelection0, Integer.toString(items.size()));
					}
				}
			}
			// Toggle item
			else {
				try {
					int index = Integer.parseInt(input) - 1;
					// Invalid input
					if ((index < 0) || (index >= items.size())) {
						response = NLS.bind(InstallMessages.Error_InvalidSelection0, Integer.toString(items.size()));
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
				catch (NumberFormatException e) {
					response = NLS.bind(InstallMessages.Error_InvalidSelection0, Integer.toString(items.size()));
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
		return toString(true, true, true);
	}

	/**
	 * Returns the list as a string.
	 * 
	 * @param message <code>true</code> to include message
	 * @param numbers <code>true</code> to include item numbers
	 * @param prompt <code>true</code> to include prompt
	 * @return List as string
	 */
	public String toString(boolean message, boolean numbers, boolean prompt) {
		StringBuilder buffer = new StringBuilder();
		
		// Message
		if (message && (getMessage() != null)) {
			buffer.append(getMessage());
			buffer.append("\n\n");
		}
		// Items
		int index = 0;
		int defaultIndex = -1;
		for (Item<T> item : items) {
			if (item == defaultItem) {
				defaultIndex = index;
			}
			if (numbers) {
				buffer.append(String.format("%4d. %s\n", ++index, item.toString()));
			}
			else {
				buffer.append(String.format("%s\n", item.toString()));
			}
		}
		// Prompt
		if (prompt) {
			buffer.append("\n");
			if (isSingleSelection()) {
				// Select option
				if (defaultIndex == -1) {
					buffer.append(NLS.bind(InstallMessages.ConsoleItemsSelectPrompt1, "1", Integer.toString(items.size())));
				}
				// Select option or use default
				else {
					buffer.append(NLS.bind(InstallMessages.ConsoleItemsSelectDefaultPrompt2, new Object[] {
							Integer.toString(defaultIndex + 1), 
							"1", 
							Integer.toString(items.size())
					}));
				}
			}
			// Select option to toggle
			else {
				buffer.append(InstallMessages.ConsoleItemsTogglePrompt);
			}
		}
		
		return buffer.toString();
	}

	/**
	 * Internal class to store item attributes.
	 */
	@SuppressWarnings("hiding")
	private class Item<T> {
		/** Item name */
		private String name;
		/** <code>true</code> if item is selected */
		private boolean selected;
		/** <code>true</code> if item can change */
		private boolean optional;
		/** Item data */
		private T data;
		/** Parent item */
		private Item<T> parent;
		/** Child items */
		private ArrayList<Item<T>> children = new ArrayList<Item<T>>();
		
		/**
		 * Constructor
		 * 
		 * @param name Item name
		 * @param data Data for item
		 * @param selected <code>true</code> to select item by default
		 * @param optional <code>true</code> if item can change
		 */
		public Item(String name, T data, boolean selected, boolean optional) {
			this.name = name;
			this.data = data;
			this.selected = selected;
			this.optional = optional;
		}
		
		/**
		 * Constructor
		 * 
		 * @param parent Parent item
		 * @param name Item name
		 * @param data Data for item
		 * @param selected <code>true</code> to select item by default
		 * @param optional <code>true</code> if item can change
		 */
		public Item(Item<T> parent, String name, T data, boolean selected, boolean optional) {
			this.parent = parent;
			this.data = data;
			this.selected = selected;
			this.optional = optional;
			StringBuffer prefix = new StringBuffer();
			Item<T> p = parent;
			while (p != null) {
				prefix.append("  ");
				p = p.parent;
			}
			prefix.append(name);
			this.name = prefix.toString();
			this.parent.children.add(this);
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
		public T getData() {
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
			
			// Set children state
			if (children.size() > 0) {
				for (Item<T> child : children) {
					child.setSelected(selected);
				}
			}
			// If de-selected, set parents de-selected
			if (!selected) {
				Item<T> parent = this.parent;
				while (parent != null) {
					parent.selected = false;
					parent = parent.parent;
				}
			}
			// If selected, see if parent can be selected
			else {
				Item<T> parent = this.parent;
				while (parent != null) {
					boolean childrenSelected = true;
					for (Item<T> child : parent.children) {
						if (!child.selected) {
							childrenSelected = false;
							break;
						}
					}
					parent.selected = childrenSelected;
					parent = parent.parent;
				}
			}
			
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
				if (isOptional()) {
					return (isSelected() ? checkedPrefix : uncheckedPrefix) + " " + getName();
				}
				else {
					char[] padding = new char[checkedPrefix.length()];
					Arrays.fill(padding, ' ');
					return new String(padding) + " " + getName();
				}
			}
		}
	}
}
