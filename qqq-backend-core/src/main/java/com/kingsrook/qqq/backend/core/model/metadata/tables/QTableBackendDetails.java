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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import java.io.Serializable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.serialization.QTableBackendDetailsDeserializer;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleInterface;


/*******************************************************************************
 ** Base class where backends can specify additional per-table meta-data.
 *******************************************************************************/
@JsonDeserialize(using = QTableBackendDetailsDeserializer.class)
public abstract class QTableBackendDetails implements Cloneable, Serializable
{
   private String backendType;



   /*******************************************************************************
    ** Getter for backendType
    **
    *******************************************************************************/
   public String getBackendType()
   {
      return backendType;
   }



   /*******************************************************************************
    ** Setter for backendType
    **
    *******************************************************************************/
   public void setBackendType(String backendType)
   {
      this.backendType = backendType;
   }



   /*******************************************************************************
    ** Setter for backendType
    **
    *******************************************************************************/
   public void setBackendType(Class<? extends QBackendModuleInterface> backendModuleClass)
   {
      try
      {
         QBackendModuleInterface qBackendModuleInterface = backendModuleClass.getConstructor().newInstance();
         this.backendType = qBackendModuleInterface.getBackendType();
      }
      catch(Exception e)
      {
         throw new IllegalArgumentException("Error dynamically getting backend type (name) from class [" + backendModuleClass.getName() + "], e)");
      }
   }



   /*******************************************************************************
    ** Fluent Setter for backendType
    **
    *******************************************************************************/
   public QTableBackendDetails withBackendType(String backendType)
   {
      this.backendType = backendType;
      return (this);
   }



   /*******************************************************************************
    ** Fluent Setter for backendType
    **
    *******************************************************************************/
   public QTableBackendDetails withBackendType(Class<? extends QBackendModuleInterface> backendModuleClass)
   {
      setBackendType(backendModuleClass);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance qInstance, QTableMetaData table, QInstanceValidator qInstanceValidator)
   {
      ////////////////////////
      // noop in base class //
      ////////////////////////
   }


   /***************************************************************************
    * adding cloneable to this type hierarchy - subclasses need to implement
    * finishClone to copy ther specific state.
    ***************************************************************************/
   public final QTableBackendDetails clone()
   {
      try
      {
         QTableBackendDetails clone = (QTableBackendDetails) super.clone();
         finishClone(clone);
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }


   /***************************************************************************
    * finish the cloning operation started in the base class. copy all state
    * from the subclass into the input clone (which can be safely casted to
    * the subclass's type, as it was obtained by super.clone())
    ***************************************************************************/
   protected abstract QTableBackendDetails finishClone(QTableBackendDetails cloned);

}
