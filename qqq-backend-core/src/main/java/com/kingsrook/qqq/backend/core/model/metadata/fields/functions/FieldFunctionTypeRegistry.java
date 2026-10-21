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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * QInstance-level registry of {@link FieldFunctionType} instances.  Providing
 * access to them based on their identifiers.
 *
 * <p>For contexts where all we have is a string name to identify a field function
 * type, then you must go through the singleton (e.g., not per-instance)
 * {@link FieldFunctionIdentifierRegistry} to map a name to an identity.</p>
 *
 * <p>For contexts where we have a {@link FieldFunctionTypeIdentifier} we can
 * directly access the function type instance here.</p>
 *
 * <p>so generally, in java code, functions should be referenced by an identity
 * object (rather than "random strings"); but in non-java contexts (e.g., serialization
 * of a filter criteria from a frontend), a string name gets us to an identity.</p>
 *******************************************************************************/
public class FieldFunctionTypeRegistry implements QSupplementalInstanceMetaData, QMetaDataObject
{
   private static final QLogger LOG = QLogger.getLogger(FieldFunctionTypeRegistry.class);

   public static final String NAME = FieldFunctionTypeRegistry.class.getName();

   //////////////////////////////////////////
   // keys here are names from identifiers //
   //////////////////////////////////////////
   private Map<String, FieldFunctionType> functionTypeRegistry = new ConcurrentHashMap<>();



   /***************************************************************************
    * Convenience overload that wraps the class in a QCodeReference before
    * delegating to {@link #register(FieldFunctionTypeIdentifier, QCodeReference)}.
    ***************************************************************************/
   public void register(FieldFunctionTypeIdentifier identifier, Class<? extends FieldFunctionType> fieldFunctionTypeClass)
   {
      register(identifier, new QCodeReference(fieldFunctionTypeClass));
   }



   /***************************************************************************
    * Registers a FieldFunctionType implementation for the given identifier.
    * Validates the code reference by instantiating it, and replaces any existing
    * registration with a warning log.
    ***************************************************************************/
   public void register(FieldFunctionTypeIdentifier identifier, QCodeReference fieldFunctionTypeCodeReference)
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure the code reference is valid - that is - that it can be instantiated as a FieldFunctionType //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      try
      {
         FieldFunctionType fieldFunctionType = QCodeLoader.getAdHoc(FieldFunctionType.class, fieldFunctionTypeCodeReference);

         //////////////////////////////////////////////////////////////////////////////////
         // if a different value is already registered under this key, then log about it //
         //////////////////////////////////////////////////////////////////////////////////
         String key = identifier.getName();
         FieldFunctionType existingRegisteredType = functionTypeRegistry.get(key);
         if(existingRegisteredType != null)
         {
            if(!existingRegisteredType.getClass().equals(fieldFunctionType.getClass()))
            {
               LOG.info("Replacing FieldFunction type in registry.", logPair("identifier", key), logPair("old", existingRegisteredType.getClass()), logPair("new", fieldFunctionTypeCodeReference));
            }
         }

         /////////////////////////////////////////////
         // put the function type into the registry //
         /////////////////////////////////////////////
         functionTypeRegistry.put(key, fieldFunctionType);
      }
      catch(Exception e)
      {
         LOG.error("Error registering FieldFunctionType [" + identifier.getName() + "] code reference [" + fieldFunctionTypeCodeReference + "]", e);
         return;
      }
   }



   /***************************************************************************
    * Returns the FieldFunctionType instance registered for the given identifier,
    * or null (with a warning log) if none is registered.
    ***************************************************************************/
   public FieldFunctionType getFieldFunctionType(FieldFunctionTypeIdentifier identifier)
   {
      if(identifier == null)
      {
         LOG.info("Null identifier passed into getFieldFunctionType - returning null.");
         return (null);
      }

      FieldFunctionType fieldFunctionType = functionTypeRegistry.get(identifier.getName());
      if(fieldFunctionType == null)
      {
         LOG.info("No FieldFunctionType registered for requested identifier", logPair("identifierName", identifier.getName()));
      }

      return (fieldFunctionType);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public String getName()
   {
      return NAME;
   }



   /*******************************************************************************
    * Returns the FieldFunctionTypeRegistryMetaData supplemental metadata from the
    * given QInstance, or null if not present.
    *******************************************************************************/
   public static FieldFunctionTypeRegistry of(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.of(qInstance, NAME);
   }



   /*******************************************************************************
    * Returns the FieldFunctionTypeRegistryMetaData supplemental metadata from the
    * given QInstance, creating and adding a new empty registry if one does not
    * yet exist.
    *******************************************************************************/
   public static FieldFunctionTypeRegistry ofOrWithNew(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, FieldFunctionTypeRegistry::new);
   }

}
