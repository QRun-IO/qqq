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

package com.kingsrook.qqq.backend.core.actions.ai.exposure;


import java.util.Locale;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiTableMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKernelSettings;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Decides the effective {@link AiExposure} of tables and fields, from their
 ** explicit {@link AiTableMetaData} / {@link AiFieldMetaData} (which always win)
 ** or else from conservative defaults:
 **
 ** Tables: the assistant's own tables (assistant*), user-session tables
 **    (userSession*) and OAuth tables (*oauth*) are NONE; others get the
 **    settings' defaultExposure.
 ** Fields: hidden fields, password (masked-type) fields and fields with a
 **    FieldSecurityLock (not enforced at runtime by core) are NONE; others get
 **    the settings' defaultExposure.
 **
 ** A field's effective exposure is then capped by its table's exposure.
 *******************************************************************************/
public class AiExposurePolicy
{
   private final AiToolKernelSettings settings;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AiExposurePolicy(AiToolKernelSettings settings)
   {
      this.settings = settings;
   }



   /*******************************************************************************
    ** The effective exposure of a table (NONE for a null table).
    *******************************************************************************/
   public AiExposure getTableExposure(QTableMetaData table)
   {
      if(table == null)
      {
         return (AiExposure.NONE);
      }

      AiTableMetaData aiTableMetaData = AiTableMetaData.of(table);
      if(aiTableMetaData != null && aiTableMetaData.getExposure() != null)
      {
         return (aiTableMetaData.getExposure());
      }

      if(isDefaultNoneTableName(table.getName()))
      {
         return (AiExposure.NONE);
      }

      return (getDefaultExposure());
   }



   /*******************************************************************************
    ** The effective exposure of a field of a table (capped by the table's).
    *******************************************************************************/
   public AiExposure getFieldExposure(QTableMetaData table, QFieldMetaData field)
   {
      if(field == null)
      {
         return (AiExposure.NONE);
      }
      return (getUncappedFieldExposure(field).mostRestrictive(getTableExposure(table)));
   }



   /*******************************************************************************
    ** The exposure of a field on its own - before its table's exposure caps it.
    *******************************************************************************/
   public AiExposure getUncappedFieldExposure(QFieldMetaData field)
   {
      AiFieldMetaData aiFieldMetaData = AiFieldMetaData.of(field);
      if(aiFieldMetaData != null && aiFieldMetaData.getExposure() != null)
      {
         return (aiFieldMetaData.getExposure());
      }

      if(getDefaultNoneReason(field) != null)
      {
         return (AiExposure.NONE);
      }

      return (getDefaultExposure());
   }



   /*******************************************************************************
    ** Why a field's exposure is what it is: "metadata" (explicit AiFieldMetaData),
    ** "hidden", "password", "field security lock", "table" (capped by its
    ** table's exposure) or "default".
    *******************************************************************************/
   public String getFieldExposureReason(QTableMetaData table, QFieldMetaData field)
   {
      AiExposure uncapped = getUncappedFieldExposure(field);
      if(getFieldExposure(table, field) != uncapped)
      {
         return ("table");
      }

      AiFieldMetaData aiFieldMetaData = AiFieldMetaData.of(field);
      if(aiFieldMetaData != null && aiFieldMetaData.getExposure() != null)
      {
         return ("metadata");
      }

      String defaultNoneReason = getDefaultNoneReason(field);
      return (defaultNoneReason == null ? "default" : defaultNoneReason);
   }



   /*******************************************************************************
    ** The reason a field defaults to NONE ("hidden", "password", "field security
    ** lock") - or null if it doesn't.
    *******************************************************************************/
   private static String getDefaultNoneReason(QFieldMetaData field)
   {
      if(field.getIsHidden())
      {
         return ("hidden");
      }
      if(field.getType() != null && field.getType().needsMasked())
      {
         return ("password");
      }
      if(field.getFieldSecurityLock() != null)
      {
         return ("field security lock");
      }
      return (null);
   }



   /*******************************************************************************
    ** Whether a table name is one that defaults to NONE: the assistant's own
    ** tables, user-session tables, and OAuth (token) tables.
    *******************************************************************************/
   public static boolean isDefaultNoneTableName(String tableName)
   {
      if(tableName == null)
      {
         return (true);
      }

      String lowerName = tableName.toLowerCase(Locale.ROOT);
      return (lowerName.startsWith("assistant") || lowerName.startsWith("usersession") || lowerName.contains("oauth"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AiExposure getDefaultExposure()
   {
      return (settings.getDefaultExposure() == null ? AiExposure.VALUES : settings.getDefaultExposure());
   }
}
