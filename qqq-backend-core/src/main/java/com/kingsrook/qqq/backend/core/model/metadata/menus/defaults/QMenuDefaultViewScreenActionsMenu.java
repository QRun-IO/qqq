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

package com.kingsrook.qqq.backend.core.model.metadata.menus.defaults;


import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.menus.QMenu;
import com.kingsrook.qqq.backend.core.model.metadata.menus.QMenuSlot;
import com.kingsrook.qqq.backend.core.model.metadata.menus.adjusters.QMenuAdjuster;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemDivider;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemSubList;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.ALL_TABLES_PROCESS_LIST;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.AUDIT;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.COPY;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.DELETE;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.DEVELOPER_MODE;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.EDIT;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.NEW;
import static com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn.DefaultOptions.THIS_TABLE_PROCESS_LIST;


/*******************************************************************************
 ** Default menu definition for the view screen actions slot.
 **
 ** <p>This class provides a standard menu configuration for record view screens
 ** that includes common actions like New, Copy, Edit, * Delete, and process
 * lists. The menu is structured with header items, dividers, and footer items
 * for better organization.</p>
 **
 ** <p>This default menu can be customized or replaced by applications as needed
 ** using menu adjusters or by providing custom menu definitions.</p>
 **
 ** @see QMenuSlot#VIEW_SCREEN_ACTIONS
 ** @see QMenuAdjuster
 *******************************************************************************/
public class QMenuDefaultViewScreenActionsMenu extends QMenu
{

   /*******************************************************************************
    ** Constructor that initializes the default view screen actions menu.
    *******************************************************************************/
   public QMenuDefaultViewScreenActionsMenu()
   {
      init();
   }



   /***************************************************************************
    * Initializes the menu with its label, icon, and slot assignment.
    *
    * <p>Sets up the basic menu properties and then initializes the menu items.</p>
    ***************************************************************************/
   public void init()
   {
      withLabel("Actions");
      withIcon(new QIcon("game"));
      withSlot(QMenuSlot.VIEW_SCREEN_ACTIONS);

      initItems();
   }



   /***************************************************************************
    * Initializes the menu items structure.
    *
    * <p>Creates a menu structure with:</p>
    * <ul>
    *   <li>Header sub-list (New, Copy, Edit, Delete)</li>
    *   <li>Divider</li>
    *   <li>This table's process list</li>
    *   <li>Divider</li>
    *   <li>Footer sub-list (All tables process list, Developer Mode, Audit)</li>
    * </ul>
    ***************************************************************************/
   public void initItems()
   {
      withItem(new HeaderSubList());
      withItem(new QMenuItemDivider());
      withItem(new QMenuItemBuiltIn(THIS_TABLE_PROCESS_LIST));
      withItem(new QMenuItemDivider());
      withItem(new FooterSubList());
   }



   /***************************************************************************
    * Sub-list containing the header menu items (primary CRUD operations).
    *
    * <p>This nested class defines the standard header items that appear at
    * the top of the default view screen actions menu: New, Copy, Edit, and Delete.</p>
    ***************************************************************************/
   public static class HeaderSubList extends QMenuItemSubList
   {
      /*******************************************************************************
       ** Constructor that initializes the header sub-list.
       *******************************************************************************/
      public HeaderSubList()
      {
         init();
      }



      /***************************************************************************
       * Initializes the sub-list structure.
       ***************************************************************************/
      public void init()
      {
         initItems();
      }



      /***************************************************************************
       * Initializes the header menu items with standard CRUD operations.
       ***************************************************************************/
      public void initItems()
      {
         withItem(new QMenuItemBuiltIn(NEW).withLabel("New"));
         withItem(new QMenuItemBuiltIn(COPY).withLabel("Copy"));
         withItem(new QMenuItemBuiltIn(EDIT).withLabel("Edit"));
         withItem(new QMenuItemBuiltIn(DELETE).withLabel("Delete"));
      }
   }



   /***************************************************************************
    * Sub-list containing the footer menu items (system-wide operations).
    *
    * <p>This nested class defines the standard footer items that appear at
    * the bottom of the default view screen actions menu: process lists for all
    * tables, Developer Mode, and Audit functionality.</p>
    ***************************************************************************/
   public static class FooterSubList extends QMenuItemSubList
   {
      /*******************************************************************************
       ** Constructor that initializes the footer sub-list.
       *******************************************************************************/
      public FooterSubList()
      {
         init();
      }



      /***************************************************************************
       * Initializes the sub-list structure.
       ***************************************************************************/
      public void init()
      {
         initItems();
      }



      /***************************************************************************
       * Initializes the footer menu items with system-wide operations.
       ***************************************************************************/
      public void initItems()
      {
         withItem(new QMenuItemBuiltIn(ALL_TABLES_PROCESS_LIST));
         withItem(new QMenuItemBuiltIn(DEVELOPER_MODE).withLabel("Developer Mode"));
         withItem(new QMenuItemBuiltIn(AUDIT).withLabel("Audit"));
      }
   }

}
