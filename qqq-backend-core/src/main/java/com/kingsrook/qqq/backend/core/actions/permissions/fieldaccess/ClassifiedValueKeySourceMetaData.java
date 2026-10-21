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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** One key source per instance. Register through ofOrWithNew; configuration cannot be replaced.
 *******************************************************************************/
public class ClassifiedValueKeySourceMetaData implements QSupplementalInstanceMetaData, Cloneable
{
   public static final String NAME = "classifiedValueKeySource";

   private QCodeReference keySource;
   private transient ClassifiedValueKeySource keySourceInstance;
   private transient ClassifiedValueKeySource loadedKeySource;



   /*******************************************************************************
    ** Find the configured policy, if any.
    *******************************************************************************/
   public static ClassifiedValueKeySourceMetaData of(QInstance instance)
   {
      return (instance == null ? null : QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    ** Get or register the policy metadata.
    *******************************************************************************/
   public static ClassifiedValueKeySourceMetaData ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, ClassifiedValueKeySourceMetaData::new));
   }



   /*******************************************************************************
    ** Supplemental metadata name.
    *******************************************************************************/
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Getter for keySource
    *******************************************************************************/
   public QCodeReference getKeySource()
   {
      return (keySource);
   }



   /*******************************************************************************
    ** Configure a code reference.
    *******************************************************************************/
   public ClassifiedValueKeySourceMetaData withKeySource(QCodeReference keySource)
   {
      if(this.keySourceInstance != null || (this.keySource != null && this.keySource != keySource))
      {
         throw (new IllegalStateException("Only one classified value key source may be registered"));
      }
      this.keySource = keySource;
      this.loadedKeySource = null;
      return (this);
   }



   /*******************************************************************************
    ** Configure the single explicit key source instance.
    *******************************************************************************/
   public ClassifiedValueKeySourceMetaData withKeySourceInstance(ClassifiedValueKeySource keySourceInstance)
   {
      if(this.keySource != null || (this.keySourceInstance != null && this.keySourceInstance != keySourceInstance))
      {
         throw (new IllegalStateException("Only one classified value key source may be registered"));
      }
      this.keySourceInstance = keySourceInstance;
      return (this);
   }



   /*******************************************************************************
    ** Load once per metadata instance, never cache a session decision here.
    *******************************************************************************/
   public synchronized ClassifiedValueKeySource getKeySourceInstance()
   {
      if(keySourceInstance != null)
      {
         return (keySourceInstance);
      }
      if(loadedKeySource == null && keySource != null)
      {
         try
         {
            loadedKeySource = QCodeLoader.getAdHoc(ClassifiedValueKeySource.class, keySource);
            if(loadedKeySource == null)
            {
               throw (new IllegalStateException("Classified value key source could not be initialized"));
            }
         }
         catch(Exception e)
         {
            throw (new IllegalStateException("Unable to load classified value key source", e));
         }
      }
      return (loadedKeySource);
   }



   /*******************************************************************************
    ** Validate the configured code reference.
    *******************************************************************************/
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      if(keySource != null)
      {
         validator.validateSimpleCodeReference(NAME + " keySource: ", keySource, ClassifiedValueKeySource.class);
      }
   }



   /*******************************************************************************
    ** Clone configuration, retaining the explicitly supplied service instance.
    *******************************************************************************/
   public ClassifiedValueKeySourceMetaData clone()
   {
      try
      {
         ClassifiedValueKeySourceMetaData clone = (ClassifiedValueKeySourceMetaData) super.clone();
         clone.keySource = keySource == null ? null : keySource.clone();
         clone.loadedKeySource = null;
         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw (new IllegalStateException(e));
      }
   }

}
