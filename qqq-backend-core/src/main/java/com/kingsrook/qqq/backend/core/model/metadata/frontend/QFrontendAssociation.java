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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;


/*******************************************************************************
 ** Shallow frontend description of a named association and its declared join.
 *******************************************************************************/
public class QFrontendAssociation
{
   private final String        name;
   private final String        associatedTableName;
   private final QJoinMetaData join;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QFrontendAssociation(Association association, QJoinMetaData join)
   {
      this.name = association.getName();
      this.associatedTableName = association.getAssociatedTableName();
      this.join = join.clone();
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Getter for associatedTableName
    *******************************************************************************/
   public String getAssociatedTableName()
   {
      return (this.associatedTableName);
   }



   /*******************************************************************************
    ** Getter for join
    *******************************************************************************/
   public QJoinMetaData getJoin()
   {
      return (this.join);
   }
}
