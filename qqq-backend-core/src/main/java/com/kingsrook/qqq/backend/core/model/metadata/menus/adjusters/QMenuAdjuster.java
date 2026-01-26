/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.metadata.menus.adjusters;


import java.util.Iterator;
import java.util.ListIterator;
import com.kingsrook.qqq.backend.core.model.metadata.menus.QMenuItemContainerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Utility class for programmatically modifying menu structures.
 **
 ** <p>This class provides static methods to adjust menus and menu items
 ** after they have been created. Common operations include:</p>
 ** <ul>
 **   <li>Removing items (first match or all matches)</li>
 **   <li>Adding items (before, after, first, or last position)</li>
 **   <li>Replacing items</li>
 ** </ul>
 **
 ** <p>All operations use {@link QMenuItemMatcher} to locate target items
 ** within the menu structure. Operations recursively search through nested
 ** sub-menus and sub-lists.</p>
 **
 ** @see QMenuItemMatcher
 *******************************************************************************/
public class QMenuAdjuster
{

   /***************************************************************************
    * Removes the first menu item that matches the given matcher.
    *
    * <p>Searches recursively through the menu structure and removes only
    * the first matching item found. Stops searching after the first removal.</p>
    *
    * @param menuItemContainer the menu item container to search
    * @param matcher the matcher used to identify the item to remove
    * @return true if an item was removed, false otherwise
    ***************************************************************************/
   public static boolean removeFirst(QMenuItemContainerInterface menuItemContainer, QMenuItemMatcher matcher)
   {
      int removeCount = remove(menuItemContainer, matcher, false);
      return (removeCount == 1);
   }



   /***************************************************************************
    * Removes all menu items that match the given matcher.
    *
    * <p>Searches recursively through the entire menu structure and removes
    * all items that match the matcher's criteria.</p>
    *
    * @param menuItemContainer the menu item container to search
    * @param matcher the matcher used to identify items to remove
    * @return the number of items that were removed
    ***************************************************************************/
   public static int removeAll(QMenuItemContainerInterface menuItemContainer, QMenuItemMatcher matcher)
   {
      return (remove(menuItemContainer, matcher, true));
   }



   /***************************************************************************
    * Internal method that performs the actual removal operation.
    *
    * @param menuItemContainer the menu item container to search
    * @param matcher the matcher used to identify items
    * @param removeAll if true, remove all matches; if false, remove only the first
    * @return the number of items removed
    ***************************************************************************/
   private static int remove(QMenuItemContainerInterface menuItemContainer, QMenuItemMatcher matcher, boolean removeAll)
   {
      int                          noRemoved = 0;
      Iterator<QMenuItemInterface> iterator  = CollectionUtils.nonNullList(menuItemContainer.getItems()).iterator();
      while(iterator.hasNext())
      {
         QMenuItemInterface item = iterator.next();

         if(matcher.doesItemMatch(item))
         {
            iterator.remove();
            noRemoved++;

            if(!removeAll)
            {
               break;
            }
         }

         if(item instanceof QMenuItemContainerInterface subContainer)
         {
            noRemoved += remove(subContainer, matcher, removeAll);

            if(!removeAll && noRemoved > 0)
            {
               break;
            }
         }
      }
      return noRemoved;
   }



   /***************************************************************************
    * Adds a new menu item immediately after the first item that matches the matcher.
    *
    * <p>Searches recursively through the menu structure. If a matching item
    * is found, the new item is inserted immediately after it. If no match is
    * found, no changes are made.</p>
    *
    * @param menuItemContainer the menu item container to modify
    * @param newItem the menu item to add
    * @param matcher the matcher used to locate the insertion point
    * @return true if the item was successfully added, false if no match was found
    ***************************************************************************/
   public static boolean addAfter(QMenuItemContainerInterface menuItemContainer, QMenuItemInterface newItem, QMenuItemMatcher matcher)
   {
      return addBeforeOrAfter(menuItemContainer, newItem, matcher, false);
   }



   /***************************************************************************
    * Adds a new menu item immediately before the first item that matches the matcher.
    *
    * <p>Searches recursively through the menu structure. If a matching item
    * is found, the new item is inserted immediately before it. If no match is
    * found, no changes are made.</p>
    *
    * @param menuItemContainer the menu item container to modify
    * @param newItem the menu item to add
    * @param matcher the matcher used to locate the insertion point
    * @return true if the item was successfully added, false if no match was found
    ***************************************************************************/
   public static boolean addBefore(QMenuItemContainerInterface menuItemContainer, QMenuItemInterface newItem, QMenuItemMatcher matcher)
   {
      return addBeforeOrAfter(menuItemContainer, newItem, matcher, true);
   }



   /***************************************************************************
    * Internal method that performs the before/after insertion operation.
    *
    * @param menuItemContainer the container to modify
    * @param newItem the item to add
    * @param matcher the matcher used to locate the insertion point
    * @param isBefore if true, insert before the match; if false, insert after
    * @return true if the item was successfully added, false otherwise
    ***************************************************************************/
   private static boolean addBeforeOrAfter(QMenuItemContainerInterface menuItemContainer, QMenuItemInterface newItem, QMenuItemMatcher matcher, boolean isBefore)
   {
      ListIterator<QMenuItemInterface> iterator = CollectionUtils.nonNullList(menuItemContainer.getItems()).listIterator();
      while(iterator.hasNext())
      {
         QMenuItemInterface item = iterator.next();

         if(matcher.doesItemMatch(item))
         {
            if(isBefore)
            {
               iterator.previous();
            }
            iterator.add(newItem);
            return (true);
         }

         if(item instanceof QMenuItemContainerInterface subContainer)
         {
            boolean found = addBeforeOrAfter(subContainer, newItem, matcher, isBefore);
            if(found)
            {
               return (true);
            }
         }
      }

      return (false);
   }



   /***************************************************************************
    * Adds a new menu item at the end of a menu item container.
    *
    * @param menuItemContainer the menu item container to modify
    * @param newItem the menu item to add
    ***************************************************************************/
   public static void addLast(QMenuItemContainerInterface menuItemContainer, QMenuItemInterface newItem)
   {
      menuItemContainer.getItems().addLast(newItem);
   }



   /***************************************************************************
    * Adds a new menu item at the start of a menu item container.
    *
    * @param menuItemContainer the menu item container to modify
    * @param newItem the menu item to add
    ***************************************************************************/
   public static void addFirst(QMenuItemContainerInterface menuItemContainer, QMenuItemInterface newItem)
   {
      menuItemContainer.getItems().addFirst(newItem);
   }



   /***************************************************************************
    * Adds a new menu item at the specified index of a menu item container.
    *
    * <p>If the index is out of bounds - this will throw IndexOutOfBoundsException</p>
    *
    * @param menuItemContainer the menu item container to modify
    * @param index (0-based) of the insertion point (see list.add)
    * @param newItem the menu item to add
    ***************************************************************************/
   public static void addAtIndex(QMenuItemContainerInterface menuItemContainer, int index, QMenuItemInterface newItem)
   {
      menuItemContainer.getItems().add(index, newItem);
   }



   /***************************************************************************
    * Replace the first menu that matches the given matcher with a replacement
    * item.
    *
    * <p>Searches recursively through the menu structure. If a matching item
    * is found, the new item replace it. If no match is found, no changes are
    * made.</p>
    *
    * @param menuItemContainer the menu item container to modify
    * @param replacementItem the menu item to add to the container
    * @param matcher the matcher used to locate the item to be replaced by the
    *                replacementItem
    * @return true if the item was successfully added, false if no match was found
    ***************************************************************************/
   public static boolean replaceItem(QMenuItemContainerInterface menuItemContainer, QMenuItemInterface replacementItem, QMenuItemMatcher matcher)
   {
      ListIterator<QMenuItemInterface> iterator = CollectionUtils.nonNullList(menuItemContainer.getItems()).listIterator();
      while(iterator.hasNext())
      {
         QMenuItemInterface item = iterator.next();

         if(matcher.doesItemMatch(item))
         {
            iterator.set(replacementItem);
            return (true);
         }

         if(item instanceof QMenuItemContainerInterface subContainer)
         {
            boolean found = replaceItem(subContainer, replacementItem, matcher);
            if(found)
            {
               return (true);
            }
         }
      }

      return (false);
   }
}
