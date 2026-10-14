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


import com.kingsrook.qqq.backend.core.alerts.AlertSeverity;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** HealthCheckMetaData
 *******************************************************************************/
public class HealthCheckMetaData implements QMetaDataObject
{
   private String name;
   private String label;
   private String component;
   private HealthCategory category = HealthCategory.OTHER;
   private HealthCheckKind kind = HealthCheckKind.DEPENDENCY;
   private int intervalSeconds = 60;
   private int timeoutMs = 5000;
   private AlertSeverity severity = AlertSeverity.WARNING;
   private int failureThreshold = 2;
   private int recoveryThreshold = 1;
   private boolean includeInReadiness = false;
   private QCodeReference check;
   private transient HealthCheck checkInstance;



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public HealthCheckMetaData withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public HealthCheckMetaData withLabel(String label)
   {
      setLabel(label);
      return (this);
   }



   /*******************************************************************************
    ** Getter for component
    *******************************************************************************/
   public String getComponent()
   {
      return (component);
   }



   /*******************************************************************************
    ** Setter for component
    *******************************************************************************/
   public void setComponent(String component)
   {
      this.component = component;
   }



   /*******************************************************************************
    ** Fluent setter for component
    *******************************************************************************/
   public HealthCheckMetaData withComponent(String component)
   {
      setComponent(component);
      return (this);
   }



   /*******************************************************************************
    ** Getter for category
    *******************************************************************************/
   public HealthCategory getCategory()
   {
      return (category);
   }



   /*******************************************************************************
    ** Setter for category
    *******************************************************************************/
   public void setCategory(HealthCategory category)
   {
      this.category = category;
   }



   /*******************************************************************************
    ** Fluent setter for category
    *******************************************************************************/
   public HealthCheckMetaData withCategory(HealthCategory category)
   {
      setCategory(category);
      return (this);
   }



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public HealthCheckKind getKind()
   {
      return (kind);
   }



   /*******************************************************************************
    ** Setter for kind
    *******************************************************************************/
   public void setKind(HealthCheckKind kind)
   {
      this.kind = kind;
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public HealthCheckMetaData withKind(HealthCheckKind kind)
   {
      setKind(kind);
      return (this);
   }



   /*******************************************************************************
    ** Getter for intervalSeconds
    *******************************************************************************/
   public int getIntervalSeconds()
   {
      return (intervalSeconds);
   }



   /*******************************************************************************
    ** Setter for intervalSeconds
    *******************************************************************************/
   public void setIntervalSeconds(int intervalSeconds)
   {
      this.intervalSeconds = intervalSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for intervalSeconds
    *******************************************************************************/
   public HealthCheckMetaData withIntervalSeconds(int intervalSeconds)
   {
      setIntervalSeconds(intervalSeconds);
      return (this);
   }



   /*******************************************************************************
    ** Getter for timeoutMs
    *******************************************************************************/
   public int getTimeoutMs()
   {
      return (timeoutMs);
   }



   /*******************************************************************************
    ** Setter for timeoutMs
    *******************************************************************************/
   public void setTimeoutMs(int timeoutMs)
   {
      this.timeoutMs = timeoutMs;
   }



   /*******************************************************************************
    ** Fluent setter for timeoutMs
    *******************************************************************************/
   public HealthCheckMetaData withTimeoutMs(int timeoutMs)
   {
      setTimeoutMs(timeoutMs);
      return (this);
   }



   /*******************************************************************************
    ** Getter for severity
    *******************************************************************************/
   public AlertSeverity getSeverity()
   {
      return (severity);
   }



   /*******************************************************************************
    ** Setter for severity
    *******************************************************************************/
   public void setSeverity(AlertSeverity severity)
   {
      this.severity = severity;
   }



   /*******************************************************************************
    ** Fluent setter for severity
    *******************************************************************************/
   public HealthCheckMetaData withSeverity(AlertSeverity severity)
   {
      setSeverity(severity);
      return (this);
   }



   /*******************************************************************************
    ** Getter for failureThreshold
    *******************************************************************************/
   public int getFailureThreshold()
   {
      return (failureThreshold);
   }



   /*******************************************************************************
    ** Setter for failureThreshold
    *******************************************************************************/
   public void setFailureThreshold(int failureThreshold)
   {
      this.failureThreshold = failureThreshold;
   }



   /*******************************************************************************
    ** Fluent setter for failureThreshold
    *******************************************************************************/
   public HealthCheckMetaData withFailureThreshold(int failureThreshold)
   {
      setFailureThreshold(failureThreshold);
      return (this);
   }



   /*******************************************************************************
    ** Getter for recoveryThreshold
    *******************************************************************************/
   public int getRecoveryThreshold()
   {
      return (recoveryThreshold);
   }



   /*******************************************************************************
    ** Setter for recoveryThreshold
    *******************************************************************************/
   public void setRecoveryThreshold(int recoveryThreshold)
   {
      this.recoveryThreshold = recoveryThreshold;
   }



   /*******************************************************************************
    ** Fluent setter for recoveryThreshold
    *******************************************************************************/
   public HealthCheckMetaData withRecoveryThreshold(int recoveryThreshold)
   {
      setRecoveryThreshold(recoveryThreshold);
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeInReadiness
    *******************************************************************************/
   public boolean getIncludeInReadiness()
   {
      return (includeInReadiness);
   }



   /*******************************************************************************
    ** Setter for includeInReadiness
    *******************************************************************************/
   public void setIncludeInReadiness(boolean includeInReadiness)
   {
      this.includeInReadiness = includeInReadiness;
   }



   /*******************************************************************************
    ** Fluent setter for includeInReadiness
    *******************************************************************************/
   public HealthCheckMetaData withIncludeInReadiness(boolean includeInReadiness)
   {
      setIncludeInReadiness(includeInReadiness);
      return (this);
   }



   /*******************************************************************************
    ** Getter for check
    *******************************************************************************/
   public QCodeReference getCheck()
   {
      return (check);
   }



   /*******************************************************************************
    ** Setter for check
    *******************************************************************************/
   public void setCheck(QCodeReference check)
   {
      this.check = check;
   }



   /*******************************************************************************
    ** Fluent setter for check
    *******************************************************************************/
   public HealthCheckMetaData withCheck(QCodeReference check)
   {
      setCheck(check);
      return (this);
   }



   /*******************************************************************************
    ** Getter for checkInstance
    *******************************************************************************/
   public HealthCheck getCheckInstance()
   {
      return (checkInstance);
   }



   /*******************************************************************************
    ** Setter for checkInstance
    *******************************************************************************/
   public void setCheckInstance(HealthCheck checkInstance)
   {
      this.checkInstance = checkInstance;
   }



   /*******************************************************************************
    ** Fluent setter for checkInstance
    *******************************************************************************/
   public HealthCheckMetaData withCheckInstance(HealthCheck checkInstance)
   {
      setCheckInstance(checkInstance);
      return (this);
   }
}
