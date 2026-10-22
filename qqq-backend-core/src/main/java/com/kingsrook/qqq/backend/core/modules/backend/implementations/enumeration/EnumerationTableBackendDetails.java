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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.enumeration;


import com.kingsrook.qqq.backend.core.model.data.QRecordEnum;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableBackendDetails;


/*******************************************************************************
 **
 *******************************************************************************/
public class EnumerationTableBackendDetails extends QTableBackendDetails
{
   private Class<? extends QRecordEnum> enumClass;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public EnumerationTableBackendDetails()
   {
      super();
      setBackendType(EnumerationBackendModule.class);
   }



   /*******************************************************************************
    ** Getter for enumClass
    **
    *******************************************************************************/
   public Class<? extends QRecordEnum> getEnumClass()
   {
      return enumClass;
   }



   /*******************************************************************************
    ** Setter for enumClass
    **
    *******************************************************************************/
   public void setEnumClass(Class<? extends QRecordEnum> enumClass)
   {
      this.enumClass = enumClass;
   }



   /*******************************************************************************
    ** Fluent setter for enumClass
    **
    *******************************************************************************/
   public EnumerationTableBackendDetails withEnumClass(Class<? extends QRecordEnum> enumClass)
   {
      this.enumClass = enumClass;
      return (this);
   }



   /***************************************************************************
    * finish the cloning operation started in the base class. copy all state
    * from the subclass into the input clone (which can be safely casted to
    * the subclass's type, as it was obtained by super.clone())
    ***************************************************************************/
   @Override
   protected QTableBackendDetails finishClone(QTableBackendDetails abstractClone)
   {
      EnumerationTableBackendDetails clone = (EnumerationTableBackendDetails) abstractClone;
      clone.enumClass = enumClass;
      return (clone);
   }

}
