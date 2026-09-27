/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
