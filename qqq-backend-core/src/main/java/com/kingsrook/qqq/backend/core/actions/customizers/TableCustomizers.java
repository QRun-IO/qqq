/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.customizers;


/*******************************************************************************
 ** Enum definition of possible table customizers - "roles" for custom code that
 ** can be applied to tables.
 **
 *******************************************************************************/
public enum TableCustomizers
{
   POST_QUERY_RECORD("postQueryRecord", TableCustomizerInterface.class),
   PRE_INSERT_RECORD("preInsertRecord", TableCustomizerInterface.class),
   POST_INSERT_RECORD("postInsertRecord", TableCustomizerInterface.class),
   PRE_UPDATE_RECORD("preUpdateRecord", TableCustomizerInterface.class),
   POST_UPDATE_RECORD("postUpdateRecord", TableCustomizerInterface.class),
   PRE_DELETE_RECORD("preDeleteRecord", TableCustomizerInterface.class),
   POST_DELETE_RECORD("postDeleteRecord", TableCustomizerInterface.class),
   POST_META_DATA_ACTION("postMetaDataAction", TableCustomizerInterface.class);


   private final String   role;
   private final Class<?> expectedType;



   /*******************************************************************************
    **
    *******************************************************************************/
   TableCustomizers(String role, Class<?> expectedType)
   {
      this.role = role;
      this.expectedType = expectedType;
   }



   /*******************************************************************************
    ** Get the TableCustomer for a given role (e.g., the role used in meta-data, not
    ** the enum-constant name).
    *******************************************************************************/
   public static TableCustomizers forRole(String name)
   {
      for(TableCustomizers value : values())
      {
         if(value.role.equals(name))
         {
            return (value);
         }
      }

      return (null);
   }



   /*******************************************************************************
    ** get the role from the tableCustomizer
    **
    *******************************************************************************/
   public String getRole()
   {
      return (role);
   }



   /*******************************************************************************
    ** Getter for expectedType
    **
    *******************************************************************************/
   public Class<?> getExpectedType()
   {
      return expectedType;
   }
}
