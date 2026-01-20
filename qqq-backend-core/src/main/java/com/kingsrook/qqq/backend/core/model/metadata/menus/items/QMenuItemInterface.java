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

package com.kingsrook.qqq.backend.core.model.metadata.menus.items;


import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** interface for different types of menu items.
 *******************************************************************************/
public interface QMenuItemInterface extends QMetaDataObject, Cloneable
{
   /***************************************************************************
    * returns user-facing text to display as the label for this menu item.  e.g.,
    * "New", "Copy", or "Process Orders".
    ***************************************************************************/
   String getLabel();

   /***************************************************************************
    * Optional icon to display with this menu item.  Frontends may choose to
    * use a default if no icon is given.
    ***************************************************************************/
   QIcon getIcon();

   /***************************************************************************
    * Specify the "type" of menu item - e.g., what sub-class it is, but more
    * accurately, instructions to the frontend on how to handle the item.
    ***************************************************************************/
   String getItemType();

   /***************************************************************************
    * get the values associated with this specific menu item.
    *
    * <p>For example, for a run-process item, the name of the process.</p>
    ***************************************************************************/
   default Map<String, Serializable> getValues()
   {
      return (Collections.emptyMap());
   }

   /***************************************************************************
    * As part of QInstanceValidation, verify that the meta-data in this object
    * is all fully valid.
    *
    * <p>Subclasses should generally include a call to super.validate</p>
    ***************************************************************************/
   default void validate(QInstanceValidator validator, QInstance qInstance, QMetaDataObject parentObject)
   {
      validator.assertCondition(StringUtils.hasContent(getItemType()), "Missing an itemType for menu item [" + getLabel() + "] in " + parentObject);
   }

   /***************************************************************************
    *
    ***************************************************************************/
   QMenuItemInterface clone();
}
