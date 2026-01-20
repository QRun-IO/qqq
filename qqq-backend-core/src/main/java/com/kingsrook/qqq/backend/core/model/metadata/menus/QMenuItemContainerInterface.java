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

package com.kingsrook.qqq.backend.core.model.metadata.menus;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemInterface;


/*******************************************************************************
 ** Interface for objects that contain a collection of menu items.
 **
 ** <p>This interface is implemented by both menus and menu items that can
 ** contain other items (such as sub-menus and sub-lists). It provides a
 ** common way to access the items within a container.</p>
 **
 ** @see QMenu
 ** @see com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemSubMenu
 ** @see com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemSubList
 *******************************************************************************/
public interface QMenuItemContainerInterface
{
   /***************************************************************************
    * Returns the list of menu items contained within this container.
    *
    * @return the list of menu items, which may be empty but should not be null
    ***************************************************************************/
   List<QMenuItemInterface> getItems();
}
