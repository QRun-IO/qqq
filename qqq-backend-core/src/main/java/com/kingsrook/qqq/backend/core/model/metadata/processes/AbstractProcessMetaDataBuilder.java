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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullConfiguration;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class AbstractProcessMetaDataBuilder
{
   private static final QLogger LOG = QLogger.getLogger(AbstractProcessMetaDataBuilder.class);

   protected QProcessMetaData processMetaData;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder(QProcessMetaData processMetaData)
   {
      this.processMetaData = processMetaData;
   }



   /*******************************************************************************
    ** Getter for processMetaData
    **
    *******************************************************************************/
   public QProcessMetaData getProcessMetaData()
   {
      return processMetaData;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withName(String name)
   {
      processMetaData.setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withLabel(String name)
   {
      processMetaData.setLabel(name);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withTableName(String tableName)
   {
      processMetaData.setTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for icon
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withIcon(QIcon icon)
   {
      processMetaData.setIcon(icon);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void setInputFieldDefaultValue(String fieldName, Serializable value)
   {
      processMetaData.getInputFields().stream()
         .filter(f -> f.getName().equals(fieldName)).findFirst()
         .ifPresentOrElse(f -> f.setDefaultValue(value),
            () -> LOG.warn("Could not find process input field for setting default value", logPair("processName", () -> processMetaData.getName()), logPair("fieldName", fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withInputFieldDefaultValue(String fieldName, Serializable value)
   {
      setInputFieldDefaultValue(fieldName, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withBasepullConfiguration(BasepullConfiguration basepullConfiguration)
   {
      processMetaData.setBasepullConfiguration(basepullConfiguration);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withSchedule(QScheduleMetaData schedule)
   {
      processMetaData.setSchedule(schedule);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withVariantRunStrategy(VariantRunStrategy variantRunStrategy)
   {
      processMetaData.setVariantRunStrategy(variantRunStrategy);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public AbstractProcessMetaDataBuilder withVariantBackend(String variantBackend)
   {
      processMetaData.setVariantBackend(variantBackend);
      return (this);
   }

}
