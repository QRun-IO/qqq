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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Typed realtime UI API payload. Qbits publish their own event names through the same bus.
 *******************************************************************************/
public class RecordConflictV1 implements ToSchema
{
   private Serializable currentVersion;
   private List<String> changedFields;
   private Serializable modifiedByUserId;
   private Serializable modifiedAt;



   /*******************************************************************************
    ** Explicit schema for clients in any language.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Map<String, Schema> properties = new TreeMap<>();
      properties.put("currentVersion", new Schema().withAnyOf(List.of(new Schema().withType(Type.STRING), new Schema().withType(Type.NUMBER), new Schema().withType(Type.BOOLEAN))).withNullable(true));
      properties.put("changedFields", new Schema().withType(Type.ARRAY).withItems(new Schema().withType(Type.STRING)));
      properties.put("modifiedByUserId", new Schema().withAnyOf(List.of(new Schema().withType(Type.STRING), new Schema().withType(Type.NUMBER), new Schema().withType(Type.BOOLEAN))).withNullable(true));
      properties.put("modifiedAt", new Schema().withAnyOf(List.of(new Schema().withType(Type.STRING), new Schema().withType(Type.NUMBER), new Schema().withType(Type.BOOLEAN))).withNullable(true));
      return (new Schema().withType(Type.OBJECT).withProperties(properties));
   }




   /*******************************************************************************
    ** Getter for currentVersion
    *******************************************************************************/
   public Serializable getCurrentVersion()
   {
      return (currentVersion);
   }




   /*******************************************************************************
    ** Setter for currentVersion
    *******************************************************************************/
   public void setCurrentVersion(Serializable currentVersion)
   {
      this.currentVersion = currentVersion;
   }




   /*******************************************************************************
    ** Fluent setter for currentVersion
    *******************************************************************************/
   public RecordConflictV1 withCurrentVersion(Serializable currentVersion)
   {
      setCurrentVersion(currentVersion);
      return (this);
   }




   /*******************************************************************************
    ** Getter for changedFields
    *******************************************************************************/
   public List<String> getChangedFields()
   {
      return (changedFields);
   }




   /*******************************************************************************
    ** Setter for changedFields
    *******************************************************************************/
   public void setChangedFields(List<String> changedFields)
   {
      this.changedFields = changedFields;
   }




   /*******************************************************************************
    ** Fluent setter for changedFields
    *******************************************************************************/
   public RecordConflictV1 withChangedFields(List<String> changedFields)
   {
      setChangedFields(changedFields);
      return (this);
   }




   /*******************************************************************************
    ** Getter for modifiedByUserId
    *******************************************************************************/
   public Serializable getModifiedByUserId()
   {
      return (modifiedByUserId);
   }




   /*******************************************************************************
    ** Setter for modifiedByUserId
    *******************************************************************************/
   public void setModifiedByUserId(Serializable modifiedByUserId)
   {
      this.modifiedByUserId = modifiedByUserId;
   }




   /*******************************************************************************
    ** Fluent setter for modifiedByUserId
    *******************************************************************************/
   public RecordConflictV1 withModifiedByUserId(Serializable modifiedByUserId)
   {
      setModifiedByUserId(modifiedByUserId);
      return (this);
   }




   /*******************************************************************************
    ** Getter for modifiedAt
    *******************************************************************************/
   public Serializable getModifiedAt()
   {
      return (modifiedAt);
   }




   /*******************************************************************************
    ** Setter for modifiedAt
    *******************************************************************************/
   public void setModifiedAt(Serializable modifiedAt)
   {
      this.modifiedAt = modifiedAt;
   }




   /*******************************************************************************
    ** Fluent setter for modifiedAt
    *******************************************************************************/
   public RecordConflictV1 withModifiedAt(Serializable modifiedAt)
   {
      setModifiedAt(modifiedAt);
      return (this);
   }
}

