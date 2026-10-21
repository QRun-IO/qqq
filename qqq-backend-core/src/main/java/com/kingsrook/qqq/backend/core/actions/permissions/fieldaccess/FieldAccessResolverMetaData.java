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
 ** One resolver per instance. Register through ofOrWithNew; configuration cannot be replaced.
 *******************************************************************************/
public class FieldAccessResolverMetaData implements QSupplementalInstanceMetaData, Cloneable
{
   public static final String NAME = "fieldAccessResolver";

   private QCodeReference resolver;
   private transient FieldAccessResolverInterface resolverInstance;
   private transient FieldAccessResolverInterface loadedResolver;



   /*******************************************************************************
    ** Find the configured policy, if any.
    *******************************************************************************/
   public static FieldAccessResolverMetaData of(QInstance instance)
   {
      return (instance == null ? null : QSupplementalInstanceMetaData.of(instance, NAME));
   }



   /*******************************************************************************
    ** Get or register the policy metadata.
    *******************************************************************************/
   public static FieldAccessResolverMetaData ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, FieldAccessResolverMetaData::new));
   }



   /*******************************************************************************
    ** Supplemental metadata name.
    *******************************************************************************/
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Getter for resolver
    *******************************************************************************/
   public QCodeReference getResolver()
   {
      return (resolver);
   }



   /*******************************************************************************
    ** Configure a code reference.
    *******************************************************************************/
   public FieldAccessResolverMetaData withResolver(QCodeReference resolver)
   {
      if(this.resolverInstance != null || (this.resolver != null && this.resolver != resolver))
      {
         throw (new IllegalStateException("Only one field access resolver may be registered"));
      }
      this.resolver = resolver;
      this.loadedResolver = null;
      return (this);
   }



   /*******************************************************************************
    ** Configure the single explicit resolver instance.
    *******************************************************************************/
   public FieldAccessResolverMetaData withResolverInstance(FieldAccessResolverInterface resolverInstance)
   {
      if(this.resolver != null || (this.resolverInstance != null && this.resolverInstance != resolverInstance))
      {
         throw (new IllegalStateException("Only one field access resolver may be registered"));
      }
      this.resolverInstance = resolverInstance;
      return (this);
   }



   /*******************************************************************************
    ** Load once per metadata instance, never cache a session decision here.
    *******************************************************************************/
   public synchronized FieldAccessResolverInterface getResolverInstance()
   {
      if(resolverInstance != null)
      {
         return (resolverInstance);
      }
      if(loadedResolver == null && resolver != null)
      {
         try
         {
            loadedResolver = QCodeLoader.getAdHoc(FieldAccessResolverInterface.class, resolver);
            if(loadedResolver == null)
            {
               throw (new IllegalStateException("Field access resolver could not be initialized"));
            }
         }
         catch(Exception e)
         {
            throw (new IllegalStateException("Unable to load field access resolver", e));
         }
      }
      return (loadedResolver);
   }



   /*******************************************************************************
    ** Validate the configured code reference.
    *******************************************************************************/
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      if(resolver != null)
      {
         validator.validateSimpleCodeReference(NAME + " resolver: ", resolver, FieldAccessResolverInterface.class);
      }
   }



   /*******************************************************************************
    ** Clone configuration, retaining the explicitly supplied service instance.
    *******************************************************************************/
   public FieldAccessResolverMetaData clone()
   {
      try
      {
         FieldAccessResolverMetaData clone = (FieldAccessResolverMetaData) super.clone();
         clone.resolver = resolver == null ? null : resolver.clone();
         clone.loadedResolver = null;
         return (clone);
      }
      catch(CloneNotSupportedException e)
      {
         throw (new IllegalStateException(e));
      }
   }

}
