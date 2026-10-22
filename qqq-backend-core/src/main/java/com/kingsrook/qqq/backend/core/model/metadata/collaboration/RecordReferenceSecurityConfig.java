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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordReferenceCleanupInterface;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordReferenceSecurityHelper;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 **
 *******************************************************************************/
public class RecordReferenceSecurityConfig implements QSupplementalInstanceMetaData
{
   private List<String> mirroredSecurityKeyTypes = new ArrayList<>();
   private List<QCodeReference> cleanupProviders = new ArrayList<>();
   public static final String NAME = "recordReferenceSecurity";



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordReferenceSecurityConfig of(QInstance instance)
   {
      return ((RecordReferenceSecurityConfig) instance.getSupplementalMetaData(NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RecordReferenceSecurityConfig ofOrWithNew(QInstance instance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(instance, NAME, RecordReferenceSecurityConfig::new));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String lockFieldName(String keyTypeName)
   {
      return ("lockKey_" + keyTypeName.replaceAll("[^A-Za-z0-9_]", "_"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void enrich(QInstance instance)
   {
      RecordReferenceSecurityHelper.register(instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      Set<String> fieldNames = new HashSet<>();
      for(String keyType : mirroredSecurityKeyTypes)
      {
         validator.assertCondition(keyType != null && !keyType.isBlank() && fieldNames.add(lockFieldName(keyType)), "Mirrored security key types must have distinct valid field names");
      }
      for(QCodeReference cleanup : cleanupProviders)
      {
         validator.validateSimpleCodeReference(NAME + " cleanup provider", cleanup, RecordReferenceCleanupInterface.class);
      }
   }



   /*******************************************************************************
    ** Getter for mirroredSecurityKeyTypes
    *******************************************************************************/
   public List<String> getMirroredSecurityKeyTypes()
   {
      return (this.mirroredSecurityKeyTypes);
   }



   /*******************************************************************************
    ** Setter for mirroredSecurityKeyTypes
    *******************************************************************************/
   public void setMirroredSecurityKeyTypes(List<String> mirroredSecurityKeyTypes)
   {
      this.mirroredSecurityKeyTypes = mirroredSecurityKeyTypes;
   }



   /*******************************************************************************
    ** Fluent setter for mirroredSecurityKeyTypes
    *******************************************************************************/
   public RecordReferenceSecurityConfig withMirroredSecurityKeyTypes(List<String> mirroredSecurityKeyTypes)
   {
      setMirroredSecurityKeyTypes(mirroredSecurityKeyTypes);
      return (this);
   }



   /*******************************************************************************
    ** Getter for cleanupProviders
    *******************************************************************************/
   public List<QCodeReference> getCleanupProviders()
   {
      return (this.cleanupProviders);
   }



   /*******************************************************************************
    ** Setter for cleanupProviders
    *******************************************************************************/
   public void setCleanupProviders(List<QCodeReference> cleanupProviders)
   {
      this.cleanupProviders = cleanupProviders;
   }



   /*******************************************************************************
    ** Fluent setter for cleanupProviders
    *******************************************************************************/
   public RecordReferenceSecurityConfig withCleanupProviders(List<QCodeReference> cleanupProviders)
   {
      setCleanupProviders(cleanupProviders);
      return (this);
   }

}
