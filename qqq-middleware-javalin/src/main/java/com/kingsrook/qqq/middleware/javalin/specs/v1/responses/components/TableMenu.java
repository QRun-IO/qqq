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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.menus.QMenu;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/***************************************************************************
 ** A menu that a table defines for a slot on a screen, for example the record
 ** view's actions menu (VIEW_SCREEN_ACTIONS) or its additional menus
 ** (VIEW_SCREEN_ADDITIONAL).
 ***************************************************************************/
public class TableMenu implements ToSchema
{
   @OpenAPIExclude()
   private QMenu wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableMenu(QMenu wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableMenu()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing label of the menu's button.")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Icon of the menu's button.")
   public Icon getIcon()
   {
      return (this.wrapped.getIcon() == null ? null : new Icon(this.wrapped.getIcon()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Where the menu goes: VIEW_SCREEN_ACTIONS, VIEW_SCREEN_ADDITIONAL, QUERY_SCREEN_ACTIONS or QUERY_SCREEN_ADDITIONAL (or a slot the application defines).")
   public String getSlot()
   {
      return (TableMenuItem.nameOf(this.wrapped.getSlot()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The menu's items, in order.")
   @OpenAPIListItems(value = TableMenuItem.class, useRef = true)
   public List<TableMenuItem> getItems()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getItems()).stream().map(TableMenuItem::new).toList());
   }
}
