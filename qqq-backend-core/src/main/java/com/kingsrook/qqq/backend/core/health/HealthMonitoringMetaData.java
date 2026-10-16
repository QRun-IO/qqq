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
   private String homePath = "/systemHealth";
   private int databaseFailureThreshold = 3;
   private int databaseRecoveryThreshold = 2;

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
      validator.assertCondition(databaseFailureThreshold > 0, "Health databaseFailureThreshold must be positive.");
      validator.assertCondition(databaseRecoveryThreshold > 0, "Health databaseRecoveryThreshold must be positive.");
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



   /*******************************************************************************
    ** Getter for homePath: the UI path alerts link to (default /systemHealth;
    ** hosts that place the health table elsewhere, e.g. under Admin, set it).
    *******************************************************************************/
   public String getHomePath()
   {
      return (this.homePath);
   }



   /*******************************************************************************
    ** Setter for homePath
    *******************************************************************************/
   public void setHomePath(String homePath)
   {
      this.homePath = homePath;
   }



   /*******************************************************************************
    ** Fluent setter for homePath
    *******************************************************************************/
   public HealthMonitoringMetaData withHomePath(String homePath)
   {
      setHomePath(homePath);
      return (this);
   }



   /*******************************************************************************
    ** Getter for databaseFailureThreshold: consecutive non-UP results before the
    ** generated core.database.* checks leave UP (default 3, so a single slow
    ** ping does not raise an alert).  Operators can still override it per check
    ** on the health_check row.
    *******************************************************************************/
   public int getDatabaseFailureThreshold()
   {
      return (this.databaseFailureThreshold);
   }



   /*******************************************************************************
    ** Setter for databaseFailureThreshold
    *******************************************************************************/
   public void setDatabaseFailureThreshold(int databaseFailureThreshold)
   {
      this.databaseFailureThreshold = databaseFailureThreshold;
   }



   /*******************************************************************************
    ** Fluent setter for databaseFailureThreshold
    *******************************************************************************/
   public HealthMonitoringMetaData withDatabaseFailureThreshold(int databaseFailureThreshold)
   {
      setDatabaseFailureThreshold(databaseFailureThreshold);
      return (this);
   }



   /*******************************************************************************
    ** Getter for databaseRecoveryThreshold: consecutive UP results before the
    ** generated core.database.* checks return to UP (default 2).  Operators can
    ** still override it per check on the health_check row.
    *******************************************************************************/
   public int getDatabaseRecoveryThreshold()
   {
      return (this.databaseRecoveryThreshold);
   }



   /*******************************************************************************
    ** Setter for databaseRecoveryThreshold
    *******************************************************************************/
   public void setDatabaseRecoveryThreshold(int databaseRecoveryThreshold)
   {
      this.databaseRecoveryThreshold = databaseRecoveryThreshold;
   }



   /*******************************************************************************
    ** Fluent setter for databaseRecoveryThreshold
    *******************************************************************************/
   public HealthMonitoringMetaData withDatabaseRecoveryThreshold(int databaseRecoveryThreshold)
   {
      setDatabaseRecoveryThreshold(databaseRecoveryThreshold);
      return (this);
   }
}
