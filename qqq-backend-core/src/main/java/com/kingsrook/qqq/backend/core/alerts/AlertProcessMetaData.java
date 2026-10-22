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


import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QSupplementalProcessMetaData;


/*******************************************************************************
 ** "Tell me when this process fails" and "tell me when it didn't run", by
 ** meta-data alone:
 **
 **   process.withSupplementalMetaData(new AlertProcessMetaData()
 **      .withAlertOnFailure(AlertSeverity.ERROR)
 **      .withExpectedEvery(Duration.ofHours(24)));
 **
 ** At enrichment this declares the alert types process.(name).failed (when
 ** alertOnFailure is set) and process.(name).missed (when expectedEvery is set,
 ** with a seeded heartbeat monitor), where (name) is the process name in
 ** lowercase-hyphen form, and registers AlertProcessLifecycleListener, which
 ** raises on failure, resolves on the next success and beats on success.
 *******************************************************************************/
public class AlertProcessMetaData extends QSupplementalProcessMetaData
{
   public static final String TYPE = "alerts";

   /** the group the declared types belong to. */
   public static final String GROUP_NAME = "processes";

   private AlertSeverity alertOnFailure;
   private Duration      expectedEvery;



   /*******************************************************************************
    ** The process's alerts meta-data, or null.
    *******************************************************************************/
   public static AlertProcessMetaData of(QProcessMetaData process)
   {
      return (process == null ? null : (AlertProcessMetaData) process.getSupplementalMetaData(TYPE));
   }



   /*******************************************************************************
    ** A process name as an alert-type segment: camelCase and anything that is
    ** not a letter or digit become lowercase words joined by hyphens
    ** (alertsEvaluateMonitors becomes alerts-evaluate-monitors).
    *******************************************************************************/
   public static String toTypeSegment(String processName)
   {
      String hyphenated = Objects.requireNonNullElse(processName, "")
         .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
         .replaceAll("[^A-Za-z0-9]+", "-")
         .toLowerCase(Locale.ROOT)
         .replaceAll("^-+|-+$", "");
      return (hyphenated.isEmpty() ? "unnamed" : hyphenated);
   }



   /*******************************************************************************
    ** The alert type raised when the process fails.
    *******************************************************************************/
   public static String failedTypeName(String processName)
   {
      return ("process." + toTypeSegment(processName) + ".failed");
   }



   /*******************************************************************************
    ** The alert type raised when the process has not completed in time.
    *******************************************************************************/
   public static String missedTypeName(String processName)
   {
      return ("process." + toTypeSegment(processName) + ".missed");
   }



   /*******************************************************************************
    ** The heartbeat key a successful run beats.
    *******************************************************************************/
   public static String heartbeatKey(String processName)
   {
      return ("process." + toTypeSegment(processName));
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
    ** Declare this process's alert types and register the lifecycle listener.
    ** Safe to run more than once.
    *******************************************************************************/
   @Override
   public void enrich(QInstanceEnricher qInstanceEnricher, QProcessMetaData process)
   {
      QInstance qInstance = qInstanceEnricher.getQInstance();
      if(qInstance == null || process == null || process.getName() == null)
      {
         return;
      }

      AlertsInstanceMetaData alertsInstanceMetaData = AlertsInstanceMetaData.ofOrWithNew(qInstance);
      String                 processLabel           = process.getLabel() != null ? process.getLabel() : process.getName();

      if(alertOnFailure != null && alertsInstanceMetaData.getAlertType(failedTypeName(process.getName())) == null)
      {
         alertsInstanceMetaData.withAlertType(new QAlertTypeMetaData()
            .withName(failedTypeName(process.getName()))
            .withLabel(processLabel + " failed")
            .withSourceName("process")
            .withGroupName(GROUP_NAME)
            .withDefaultSeverity(alertOnFailure)
            .withResolveMode(ResolveMode.PRODUCER)
            .withDescription("A run of the process " + processLabel + " failed.")
            .withHowToFix("Open the failed run's error, fix the cause, and run the process again. The alert resolves on the next successful run."));
      }

      if(expectedEvery != null && alertsInstanceMetaData.getAlertType(missedTypeName(process.getName())) == null)
      {
         long expectedSeconds = expectedEvery.toSeconds();
         long graceSeconds    = Math.max(60, expectedSeconds / 10);
         alertsInstanceMetaData.withAlertType(new QAlertTypeMetaData()
            .withName(missedTypeName(process.getName()))
            .withLabel(processLabel + " did not run")
            .withSourceName("process")
            .withGroupName(GROUP_NAME)
            .withDefaultSeverity(alertOnFailure != null ? alertOnFailure : AlertSeverity.WARNING)
            .withResolveMode(ResolveMode.MONITOR)
            .withDescription("The process " + processLabel + " has not completed successfully within its expected interval.")
            .withHowToFix("Check that its schedule is running and that recent runs did not fail.")
            .withDefaultMonitor(AlertDefaultMonitor.heartbeat(heartbeatKey(process.getName()) + ".heartbeat", heartbeatKey(process.getName()), (int) Math.min(Integer.MAX_VALUE, expectedSeconds), (int) Math.min(Integer.MAX_VALUE, graceSeconds))
               .withDefinitionValue("processName", process.getName())));
      }

      String listenerClassName = AlertProcessLifecycleListener.class.getName();
      if(qInstance.getProcessLifecycleListeners() == null || qInstance.getProcessLifecycleListeners().stream().noneMatch(ref -> ref != null && listenerClassName.equals(ref.getName())))
      {
         qInstance.withProcessLifecycleListener(new QCodeReference(AlertProcessLifecycleListener.class));
      }
   }



   /*******************************************************************************
    ** Something must be asked for, and an interval must be positive.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QProcessMetaData process, QInstanceValidator qInstanceValidator)
   {
      String prefix = "Process " + (process == null ? null : process.getName()) + " alerts meta-data: ";
      qInstanceValidator.assertCondition(alertOnFailure != null || expectedEvery != null, prefix + "set alertOnFailure and/or expectedEvery.");
      if(expectedEvery != null)
      {
         qInstanceValidator.assertCondition(!expectedEvery.isNegative() && !expectedEvery.isZero(), prefix + "expectedEvery must be positive.");
      }
   }



   /*******************************************************************************
    ** Getter for alertOnFailure
    *******************************************************************************/
   public AlertSeverity getAlertOnFailure()
   {
      return (this.alertOnFailure);
   }



   /*******************************************************************************
    ** Fluent setter for alertOnFailure - the severity of the alert raised when
    ** a run fails (null: don't alert on failure).
    *******************************************************************************/
   public AlertProcessMetaData withAlertOnFailure(AlertSeverity alertOnFailure)
   {
      this.alertOnFailure = alertOnFailure;
      return (this);
   }



   /*******************************************************************************
    ** Getter for expectedEvery
    *******************************************************************************/
   public Duration getExpectedEvery()
   {
      return (this.expectedEvery);
   }



   /*******************************************************************************
    ** Fluent setter for expectedEvery - how often a successful run is expected
    ** (null: don't watch for missed runs).
    *******************************************************************************/
   public AlertProcessMetaData withExpectedEvery(Duration expectedEvery)
   {
      this.expectedEvery = expectedEvery;
      return (this);
   }
}
