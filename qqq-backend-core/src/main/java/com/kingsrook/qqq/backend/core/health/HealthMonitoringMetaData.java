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

package com.kingsrook.qqq.backend.core.health;


import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;


/*******************************************************************************
 ** HealthMonitoringMetaData
 *******************************************************************************/
public class HealthMonitoringMetaData implements QSupplementalInstanceMetaData
{
   private String backendName;
   private boolean runnerEnabled = true;
   private int tickSeconds = 10;
   private int maxConcurrentChecks = 4;
   private int resultRetentionDays = 30;
   private boolean prometheusEnabled = false;
   private String runnerUserName = "health_runner";

   public static final String NAME = "healthMonitoring";



   /*******************************************************************************
    ** Get or install monitoring configuration.
    *******************************************************************************/
   public static HealthMonitoringMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, HealthMonitoringMetaData::new));
   }



   /*******************************************************************************
    ** Supplemental metadata name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Reject unsafe scheduling and storage configuration before startup.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      validator.assertCondition(backendName != null && qInstance.getBackend(backendName) != null, "Health monitoring requires a valid backendName.");
      validator.assertCondition(tickSeconds > 0, "Health tickSeconds must be positive.");
      validator.assertCondition(maxConcurrentChecks > 0, "Health maxConcurrentChecks must be positive.");
      validator.assertCondition(resultRetentionDays > 0, "Health resultRetentionDays must be positive.");
      validator.assertCondition(runnerUserName != null && !runnerUserName.isBlank(), "Health runnerUserName is required.");
   }



   /*******************************************************************************
    ** Getter for backendName
    *******************************************************************************/
   public String getBackendName()
   {
      return (backendName);
   }



   /*******************************************************************************
    ** Setter for backendName
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    ** Fluent setter for backendName
    *******************************************************************************/
   public HealthMonitoringMetaData withBackendName(String backendName)
   {
      setBackendName(backendName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for runnerEnabled
    *******************************************************************************/
   public boolean getRunnerEnabled()
   {
      return (runnerEnabled);
   }



   /*******************************************************************************
    ** Setter for runnerEnabled
    *******************************************************************************/
   public void setRunnerEnabled(boolean runnerEnabled)
   {
      this.runnerEnabled = runnerEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for runnerEnabled
    *******************************************************************************/
   public HealthMonitoringMetaData withRunnerEnabled(boolean runnerEnabled)
   {
      setRunnerEnabled(runnerEnabled);
      return (this);
   }



   /*******************************************************************************
    ** Getter for tickSeconds
    *******************************************************************************/
   public int getTickSeconds()
   {
      return (tickSeconds);
   }



   /*******************************************************************************
    ** Setter for tickSeconds
    *******************************************************************************/
   public void setTickSeconds(int tickSeconds)
   {
      this.tickSeconds = tickSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for tickSeconds
    *******************************************************************************/
   public HealthMonitoringMetaData withTickSeconds(int tickSeconds)
   {
      setTickSeconds(tickSeconds);
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxConcurrentChecks
    *******************************************************************************/
   public int getMaxConcurrentChecks()
   {
      return (maxConcurrentChecks);
   }



   /*******************************************************************************
    ** Setter for maxConcurrentChecks
    *******************************************************************************/
   public void setMaxConcurrentChecks(int maxConcurrentChecks)
   {
      this.maxConcurrentChecks = maxConcurrentChecks;
   }



   /*******************************************************************************
    ** Fluent setter for maxConcurrentChecks
    *******************************************************************************/
   public HealthMonitoringMetaData withMaxConcurrentChecks(int maxConcurrentChecks)
   {
      setMaxConcurrentChecks(maxConcurrentChecks);
      return (this);
   }



   /*******************************************************************************
    ** Getter for resultRetentionDays
    *******************************************************************************/
   public int getResultRetentionDays()
   {
      return (resultRetentionDays);
   }



   /*******************************************************************************
    ** Setter for resultRetentionDays
    *******************************************************************************/
   public void setResultRetentionDays(int resultRetentionDays)
   {
      this.resultRetentionDays = resultRetentionDays;
   }



   /*******************************************************************************
    ** Fluent setter for resultRetentionDays
    *******************************************************************************/
   public HealthMonitoringMetaData withResultRetentionDays(int resultRetentionDays)
   {
      setResultRetentionDays(resultRetentionDays);
      return (this);
   }



   /*******************************************************************************
    ** Getter for prometheusEnabled
    *******************************************************************************/
   public boolean getPrometheusEnabled()
   {
      return (prometheusEnabled);
   }



   /*******************************************************************************
    ** Setter for prometheusEnabled
    *******************************************************************************/
   public void setPrometheusEnabled(boolean prometheusEnabled)
   {
      this.prometheusEnabled = prometheusEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for prometheusEnabled
    *******************************************************************************/
   public HealthMonitoringMetaData withPrometheusEnabled(boolean prometheusEnabled)
   {
      setPrometheusEnabled(prometheusEnabled);
      return (this);
   }



   /*******************************************************************************
    ** Getter for runnerUserName
    *******************************************************************************/
   public String getRunnerUserName()
   {
      return (runnerUserName);
   }



   /*******************************************************************************
    ** Setter for runnerUserName
    *******************************************************************************/
   public void setRunnerUserName(String runnerUserName)
   {
      this.runnerUserName = runnerUserName;
   }



   /*******************************************************************************
    ** Fluent setter for runnerUserName
    *******************************************************************************/
   public HealthMonitoringMetaData withRunnerUserName(String runnerUserName)
   {
      setRunnerUserName(runnerUserName);
      return (this);
   }
}
