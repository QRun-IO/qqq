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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;


/*******************************************************************************
 * Frontend-facing representation of a {@link com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData},
 * extending {@link QFrontendFieldMetaData} with additional flags that indicate
 * whether the virtual field can be used as a query filter criterion and/or
 * included in query output selections.
 *******************************************************************************/
@JsonInclude(Include.NON_NULL)
public class QFrontendVirtualFieldMetaData extends QFrontendFieldMetaData
{
   private boolean isQueryCriteria;
   private boolean isQuerySelectable;

   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public QFrontendVirtualFieldMetaData(QVirtualFieldMetaData fieldMetaData)
   {
      super(fieldMetaData);

      this.isQueryCriteria = fieldMetaData.getIsQueryCriteria();
      this.isQuerySelectable = fieldMetaData.getIsQuerySelectable();
   }



   /*******************************************************************************
    ** Getter for isQuerySelectable
    **
    *******************************************************************************/
   public boolean getIsQuerySelectable()
   {
      return isQuerySelectable;
   }



   /*******************************************************************************
    ** Getter for isQueryCriteria
    **
    *******************************************************************************/
   public boolean getIsQueryCriteria()
   {
      return isQueryCriteria;
   }

}
