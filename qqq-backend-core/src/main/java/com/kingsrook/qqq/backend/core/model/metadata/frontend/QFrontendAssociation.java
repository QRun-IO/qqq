/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
