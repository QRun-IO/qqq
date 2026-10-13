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

package com.kingsrook.qqq.backend.core.model.metadata.collaboration;


import java.io.Serializable;


/*******************************************************************************
 **
 *******************************************************************************/
public class RecordOwnership implements Serializable
{
   private String ownerFieldName;
   private String assigneeFieldName;



   /*******************************************************************************
    ** Getter for ownerFieldName
    *******************************************************************************/
   public String getOwnerFieldName()
   {
      return (this.ownerFieldName);
   }



   /*******************************************************************************
    ** Setter for ownerFieldName
    *******************************************************************************/
   public void setOwnerFieldName(String ownerFieldName)
   {
      this.ownerFieldName = ownerFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for ownerFieldName
    *******************************************************************************/
   public RecordOwnership withOwnerFieldName(String ownerFieldName)
   {
      setOwnerFieldName(ownerFieldName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for assigneeFieldName
    *******************************************************************************/
   public String getAssigneeFieldName()
   {
      return (this.assigneeFieldName);
   }



   /*******************************************************************************
    ** Setter for assigneeFieldName
    *******************************************************************************/
   public void setAssigneeFieldName(String assigneeFieldName)
   {
      this.assigneeFieldName = assigneeFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for assigneeFieldName
    *******************************************************************************/
   public RecordOwnership withAssigneeFieldName(String assigneeFieldName)
   {
      setAssigneeFieldName(assigneeFieldName);
      return (this);
   }

}
