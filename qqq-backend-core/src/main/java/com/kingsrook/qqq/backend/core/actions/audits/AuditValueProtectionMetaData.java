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

package com.kingsrook.qqq.backend.core.actions.audits;


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** One protector per instance. Register through ofOrWithNew; configuration cannot be replaced.
 *******************************************************************************/
public class AuditValueProtectionMetaData implements QSupplementalInstanceMetaData, Cloneable
{
   public static final String NAME = "auditValueProtection";

   private QCodeReference protector;
   private transient AuditValueProtectorInterface protectorInstance;
   private transient AuditValueProtectorInterface loadedProtector;



   /*******************************************************************************
    ** Find the configured policy, if any.
    *******************************************************************************/
   public static AuditValueProtectionMetaData of(QInstance instance)
   {
      return (instance == null ? null : QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    ** Get or register the policy metadata.
    *******************************************************************************/
   public static AuditValueProtectionMetaData ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, AuditValueProtectionMetaData::new));
   }



   /*******************************************************************************
    ** Supplemental metadata name.
    *******************************************************************************/
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Getter for protector
    *******************************************************************************/
   public QCodeReference getProtector()
   {
      return (protector);
   }



   /*******************************************************************************
    ** Configure a code reference.
    *******************************************************************************/
   public AuditValueProtectionMetaData withProtector(QCodeReference protector)
   {
      if(this.protectorInstance != null || (this.protector != null && this.protector != protector))
      {
         throw (new IllegalStateException("Only one audit value protector may be registered"));
      }
      this.protector = protector;
      this.loadedProtector = null;
      return (this);
   }



   /*******************************************************************************
    ** Configure the single explicit protector instance.
    *******************************************************************************/
   public AuditValueProtectionMetaData withProtectorInstance(AuditValueProtectorInterface protectorInstance)
   {
      if(this.protector != null || (this.protectorInstance != null && this.protectorInstance != protectorInstance))
      {
         throw (new IllegalStateException("Only one audit value protector may be registered"));
      }
      this.protectorInstance = protectorInstance;
      return (this);
   }



   /*******************************************************************************
    ** Load once per metadata instance, never cache a session decision here.
    *******************************************************************************/
   public synchronized AuditValueProtectorInterface getProtectorInstance()
   {
      if(protectorInstance != null)
      {
         return (protectorInstance);
      }
      if(loadedProtector == null && protector != null)
      {
         try
         {
            loadedProtector = QCodeLoader.getAdHoc(AuditValueProtectorInterface.class, protector);
            if(loadedProtector == null)
            {
               throw (new IllegalStateException("Audit value protector could not be initialized"));
            }
         }
         catch(Exception e)
         {
            throw (new IllegalStateException("Unable to load audit value protector", e));
         }
      }
      return (loadedProtector);
   }



   /*******************************************************************************
    ** Validate the configured code reference.
    *******************************************************************************/
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      if(protector != null)
      {
         validator.validateSimpleCodeReference(NAME + " protector: ", protector, AuditValueProtectorInterface.class);
      }
   }



   /*******************************************************************************
    ** Clone configuration, retaining the explicitly supplied service instance.
    *******************************************************************************/
   public AuditValueProtectionMetaData clone()
   {
      try
      {
         AuditValueProtectionMetaData clone = (AuditValueProtectionMetaData) super.clone();
         clone.protector = protector == null ? null : protector.clone();
         clone.loadedProtector = null;
         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw (new IllegalStateException(e));
      }
   }

}
