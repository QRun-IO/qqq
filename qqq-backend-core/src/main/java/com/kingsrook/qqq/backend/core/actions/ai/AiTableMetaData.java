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

package com.kingsrook.qqq.backend.core.actions.ai;


import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Supplemental table meta-data (type "ai") for the AI tool kernel: how much of
 ** the table is exposed to a model, and an optional description written for a
 ** model (aiDescription), used by describe_table, search_help and the app
 ** digest.
 **
 ** A null exposure means "use the default": NONE for the assistant's own
 ** tables, user-session tables and OAuth tables, else the kernel settings'
 ** defaultExposure.  An explicit exposure always wins over the default.
 *******************************************************************************/
public class AiTableMetaData extends QSupplementalTableMetaData
{
   public static final String TYPE = "ai";

   private AiExposure         exposure;
   private String             aiDescription;
   private DataClassification classification;



   /*******************************************************************************
    ** The AiTableMetaData on a table, or null if it has none.
    *******************************************************************************/
   public static AiTableMetaData of(QTableMetaData table)
   {
      return (table == null ? null : QSupplementalTableMetaData.of(table, TYPE));
   }



   /*******************************************************************************
    ** The AiTableMetaData on a table - adding a new (empty) one if it has none.
    *******************************************************************************/
   public static AiTableMetaData ofOrWithNew(QTableMetaData table)
   {
      return (QSupplementalTableMetaData.ofOrWithNew(table, TYPE, AiTableMetaData::new));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (TYPE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected QSupplementalTableMetaData finishClone(QSupplementalTableMetaData abstractClone)
   {
      AiTableMetaData clone = (AiTableMetaData) abstractClone;
      clone.exposure = this.exposure;
      clone.aiDescription = this.aiDescription;
      clone.classification = this.classification;
      return (clone);
   }


   /*******************************************************************************
    ** Getter for exposure
    *******************************************************************************/
   public AiExposure getExposure()
   {
      return (this.exposure);
   }



   /*******************************************************************************
    ** Setter for exposure
    *******************************************************************************/
   public void setExposure(AiExposure exposure)
   {
      this.exposure = exposure;
   }



   /*******************************************************************************
    ** Fluent setter for exposure
    *******************************************************************************/
   public AiTableMetaData withExposure(AiExposure exposure)
   {
      this.exposure = exposure;
      return (this);
   }



   /*******************************************************************************
    ** Getter for aiDescription
    *******************************************************************************/
   public String getAiDescription()
   {
      return (this.aiDescription);
   }



   /*******************************************************************************
    ** Setter for aiDescription
    *******************************************************************************/
   public void setAiDescription(String aiDescription)
   {
      this.aiDescription = aiDescription;
   }



   /*******************************************************************************
    ** Fluent setter for aiDescription
    *******************************************************************************/
   public AiTableMetaData withAiDescription(String aiDescription)
   {
      this.aiDescription = aiDescription;
      return (this);
   }



   /*******************************************************************************
    ** Getter for classification (null means not classified)
    *******************************************************************************/
   public DataClassification getClassification()
   {
      return (this.classification);
   }



   /*******************************************************************************
    ** Setter for classification
    *******************************************************************************/
   public void setClassification(DataClassification classification)
   {
      this.classification = classification;
   }



   /*******************************************************************************
    ** Fluent setter for classification
    *******************************************************************************/
   public AiTableMetaData withClassification(DataClassification classification)
   {
      this.classification = classification;
      return (this);
   }
}
