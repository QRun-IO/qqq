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

package com.kingsrook.qqq.backend.core.alerts;


import java.io.Serializable;
import java.time.Instant;


/*******************************************************************************
 ** A producer's "this problem is gone", passed to QAlerts.resolve.  Resolving
 ** a key that is not open is a no-op.
 *******************************************************************************/
public class AlertResolution implements Serializable
{
   private String  alertTypeName;
   private String  dedupKey;
   private String  note;
   private Instant resolvedAt;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AlertResolution()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AlertResolution(String alertTypeName, String dedupKey, String note)
   {
      this.alertTypeName = alertTypeName;
      this.dedupKey = dedupKey;
      this.note = note;
   }



   /*******************************************************************************
    ** The dedup key to use: the one given, or the type name.
    *******************************************************************************/
   public String getEffectiveDedupKey()
   {
      return (dedupKey == null || dedupKey.isBlank() ? alertTypeName : dedupKey);
   }



   /*******************************************************************************
    ** Getter for alertTypeName
    *******************************************************************************/
   public String getAlertTypeName()
   {
      return (this.alertTypeName);
   }



   /*******************************************************************************
    ** Fluent setter for alertTypeName
    *******************************************************************************/
   public AlertResolution withAlertTypeName(String alertTypeName)
   {
      this.alertTypeName = alertTypeName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dedupKey
    *******************************************************************************/
   public String getDedupKey()
   {
      return (this.dedupKey);
   }



   /*******************************************************************************
    ** Fluent setter for dedupKey
    *******************************************************************************/
   public AlertResolution withDedupKey(String dedupKey)
   {
      this.dedupKey = dedupKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for note
    *******************************************************************************/
   public String getNote()
   {
      return (this.note);
   }



   /*******************************************************************************
    ** Fluent setter for note
    *******************************************************************************/
   public AlertResolution withNote(String note)
   {
      this.note = note;
      return (this);
   }



   /*******************************************************************************
    ** Getter for resolvedAt
    *******************************************************************************/
   public Instant getResolvedAt()
   {
      return (this.resolvedAt);
   }



   /*******************************************************************************
    ** Fluent setter for resolvedAt (default: when QAlerts.resolve is called)
    *******************************************************************************/
   public AlertResolution withResolvedAt(Instant resolvedAt)
   {
      this.resolvedAt = resolvedAt;
      return (this);
   }
}
