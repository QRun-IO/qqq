/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.columnstats;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QSupplementalTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 * Object with properties to allow configuring behavior of the ColumnStats
 * process.
 *
 * <p>Can be deployed various ways:</p>
 * <ul>
 *    <li>As supplementalTableMetaData (e.g., set on individual tables in QInstance)
 *       <pre>
 * ColumnStatsTableConfig.ofOrWithNew(myTableMetaData).withFailIfCountOverLimit(47)
 *       </pre>
 *    </li>
 *    <li>As a value in the process, named <code>"ColumnStatsTableConfig":</code>
 *    <ul>
 *       <li>Where one can set a default value for the process (e.g., across all
 *       tables) as in:
 *       <pre>
 * processMetaData.getInputField("ColumnStatsTableConfig").ifPresent(f ->
 *    f.setDefaultValue(new ColumnStatsTableConfig().withFailIfCountOverLimit(42)));
 *       </pre>
 *       </li>
 *       <li>Or an application can insert a custom step into this process
 *       to dynamically build the config when the process runs
 *       <pre>
 * processMetaData.withStep(0, new QBackendStepMetaData()
 *    .withName("setConfig")
 *    .withCode(new QCodeReference(ColumnStatusConfigBuilderStep.class)));
 *       </pre>
 *       and then in that step:
 *       <pre>
 * runBackendStepOutput.addValue("ColumnStatsTableConfig", new ColumnStatsTableConfig());
 *       </pre>
 *       </li>
 *    </ul>
 * </ul>
 *******************************************************************************/
public class ColumnStatsTableConfig extends QSupplementalTableMetaData implements Serializable
{
   public static final String NAME = ColumnStatsTableConfig.class.getName();

   private Integer failIfCountOverLimit = null;
   private Integer queryTimeoutSeconds  = null;



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public String getType()
   {
      return NAME;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static ColumnStatsTableConfig of(QTableMetaData tableMetaData)
   {
      return of(tableMetaData, NAME);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static ColumnStatsTableConfig ofOrWithNew(QTableMetaData tableMetaData)
   {
      return ofOrWithNew(tableMetaData, NAME, ColumnStatsTableConfig::new);
   }



   /*******************************************************************************
    * Getter for failIfCountOverLimit
    * @see #withFailIfCountOverLimit(Integer)
    *******************************************************************************/
   public Integer getFailIfCountOverLimit()
   {
      return (this.failIfCountOverLimit);
   }



   /*******************************************************************************
    * Setter for failIfCountOverLimit
    * @see #withFailIfCountOverLimit(Integer)
    *******************************************************************************/
   public void setFailIfCountOverLimit(Integer failIfCountOverLimit)
   {
      this.failIfCountOverLimit = failIfCountOverLimit;
   }



   /*******************************************************************************
    * Fluent setter for failIfCountOverLimit
    *
    * @param failIfCountOverLimit
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public ColumnStatsTableConfig withFailIfCountOverLimit(Integer failIfCountOverLimit)
   {
      this.failIfCountOverLimit = failIfCountOverLimit;
      return (this);
   }



   /*******************************************************************************
    * Getter for queryTimeoutSeconds
    * @see #withQueryTimeoutSeconds(Integer)
    *******************************************************************************/
   public Integer getQueryTimeoutSeconds()
   {
      return (this.queryTimeoutSeconds);
   }



   /*******************************************************************************
    * Setter for queryTimeoutSeconds
    * @see #withQueryTimeoutSeconds(Integer)
    *******************************************************************************/
   public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds)
   {
      this.queryTimeoutSeconds = queryTimeoutSeconds;
   }



   /*******************************************************************************
    * Fluent setter for queryTimeoutSeconds
    *
    * @param queryTimeoutSeconds
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public ColumnStatsTableConfig withQueryTimeoutSeconds(Integer queryTimeoutSeconds)
   {
      this.queryTimeoutSeconds = queryTimeoutSeconds;
      return (this);
   }
}
