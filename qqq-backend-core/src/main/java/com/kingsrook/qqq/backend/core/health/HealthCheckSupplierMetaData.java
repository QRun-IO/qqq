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

package com.kingsrook.qqq.backend.core.health;


import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** HealthCheckSupplierMetaData
 *******************************************************************************/
public class HealthCheckSupplierMetaData implements QMetaDataObject
{
   private String namePrefix;
   private String component;
   private QCodeReference supplier;



   /*******************************************************************************
    ** Getter for namePrefix
    *******************************************************************************/
   public String getNamePrefix()
   {
      return (namePrefix);
   }



   /*******************************************************************************
    ** Setter for namePrefix
    *******************************************************************************/
   public void setNamePrefix(String namePrefix)
   {
      this.namePrefix = namePrefix;
   }



   /*******************************************************************************
    ** Fluent setter for namePrefix
    *******************************************************************************/
   public HealthCheckSupplierMetaData withNamePrefix(String namePrefix)
   {
      setNamePrefix(namePrefix);
      return (this);
   }



   /*******************************************************************************
    ** Getter for component
    *******************************************************************************/
   public String getComponent()
   {
      return (component);
   }



   /*******************************************************************************
    ** Setter for component
    *******************************************************************************/
   public void setComponent(String component)
   {
      this.component = component;
   }



   /*******************************************************************************
    ** Fluent setter for component
    *******************************************************************************/
   public HealthCheckSupplierMetaData withComponent(String component)
   {
      setComponent(component);
      return (this);
   }



   /*******************************************************************************
    ** Getter for supplier
    *******************************************************************************/
   public QCodeReference getSupplier()
   {
      return (supplier);
   }



   /*******************************************************************************
    ** Setter for supplier
    *******************************************************************************/
   public void setSupplier(QCodeReference supplier)
   {
      this.supplier = supplier;
   }



   /*******************************************************************************
    ** Fluent setter for supplier
    *******************************************************************************/
   public HealthCheckSupplierMetaData withSupplier(QCodeReference supplier)
   {
      setSupplier(supplier);
      return (this);
   }
}
