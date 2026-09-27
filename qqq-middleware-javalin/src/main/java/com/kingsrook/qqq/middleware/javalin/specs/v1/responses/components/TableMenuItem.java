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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemBuiltIn;
import com.kingsrook.qqq.backend.core.model.metadata.menus.items.QMenuItemInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIHasAdditionalProperties;


/***************************************************************************
 ** One item of a table menu.
 ***************************************************************************/
public class TableMenuItem implements ToSchema
{
   @OpenAPIExclude()
   private QMenuItemInterface wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableMenuItem(QMenuItemInterface wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TableMenuItem()
   {
   }



   /***************************************************************************
    ** The name of an enum constant, else the value's text (null stays null).
    ***************************************************************************/
   static String nameOf(Object value)
   {
      if(value == null)
      {
         return (null);
      }
      return (value instanceof Enum<?> constant ? constant.name() : String.valueOf(value));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("What the item is and how a frontend handles it: BUILT_IN (a standard action named by values.option, such as NEW, COPY, EDIT, DELETE, DEVELOPER_MODE, AUDIT, THIS_TABLE_PROCESS_LIST or ALL_TABLES_PROCESS_LIST), RUN_PROCESS (values.processName), DOWNLOAD_FILE (values.fieldName), SUB_MENU and SUB_LIST (values.items), or DIVIDER.")
   public String getItemType()
   {
      return (this.wrapped.getItemType());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing label of the item, when it overrides the frontend's default.")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Icon of the item, when it overrides the frontend's default.")
   public Icon getIcon()
   {
      return (this.wrapped.getIcon() == null ? null : new Icon(this.wrapped.getIcon()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Item-type specific values: option for BUILT_IN, processName for RUN_PROCESS, fieldName for DOWNLOAD_FILE, and items (a list of menu items of this same shape) for SUB_MENU and SUB_LIST.")
   @OpenAPIHasAdditionalProperties
   public Map<String, Object> getValues()
   {
      Map<String, Object> values = new LinkedHashMap<>();
      for(Map.Entry<String, ?> entry : CollectionUtils.nonNullMap(this.wrapped.getValues()).entrySet())
      {
         Object value = entry.getValue();
         if(value instanceof Collection<?> collection && collection.stream().allMatch(QMenuItemInterface.class::isInstance))
         {
            values.put(entry.getKey(), collection.stream().map(element -> new TableMenuItem((QMenuItemInterface) element)).toList());
         }
         else if(value instanceof QMenuItemBuiltIn.BuiltInOptionInterface)
         {
            values.put(entry.getKey(), nameOf(value));
         }
         else
         {
            values.put(entry.getKey(), value);
         }
      }
      return (values);
   }
}
