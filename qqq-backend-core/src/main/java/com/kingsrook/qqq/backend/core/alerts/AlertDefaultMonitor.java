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
import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 ** A monitor that an alert type ships with ("seeded").  An alerts
 ** implementation inserts it once; after that it belongs to the admin, who may
 ** tune or disable it.  Without an implementation installed it does nothing.
 **
 ** The definition's keys depend on the kind (e.g. HEARTBEAT takes heartbeatKey
 ** or processName, plus expectedEverySeconds and graceSeconds; TABLE_QUERY
 ** takes tableName and filter).  Values must be JSON-friendly.
 *******************************************************************************/
public class AlertDefaultMonitor implements Serializable
{
   private String                    name;
   private AlertMonitorKind          kind;
   private Map<String, Serializable> definition = new LinkedHashMap<>();
   private Boolean                   perRecord  = false;
   private AlertSeverity             severity;
   private Integer                   scheduleSeconds;
   private Integer                   forMinutes = 0;



   /*******************************************************************************
    ** A heartbeat monitor: raise when the key has not beat within the expected
    ** interval plus grace.
    *******************************************************************************/
   public static AlertDefaultMonitor heartbeat(String name, String heartbeatKey, int expectedEverySeconds, int graceSeconds)
   {
      return (new AlertDefaultMonitor()
         .withName(name)
         .withKind(AlertMonitorKind.HEARTBEAT)
         .withDefinitionValue("heartbeatKey", heartbeatKey)
         .withDefinitionValue("expectedEverySeconds", expectedEverySeconds)
         .withDefinitionValue("graceSeconds", graceSeconds));
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public AlertDefaultMonitor withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public AlertMonitorKind getKind()
   {
      return (this.kind);
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public AlertDefaultMonitor withKind(AlertMonitorKind kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for definition
    *******************************************************************************/
   public Map<String, Serializable> getDefinition()
   {
      return (this.definition);
   }



   /*******************************************************************************
    ** Fluent setter for definition
    *******************************************************************************/
   public AlertDefaultMonitor withDefinition(Map<String, Serializable> definition)
   {
      this.definition = definition == null ? new LinkedHashMap<>() : new LinkedHashMap<>(definition);
      return (this);
   }



   /*******************************************************************************
    ** Set one definition value.
    *******************************************************************************/
   public AlertDefaultMonitor withDefinitionValue(String key, Serializable value)
   {
      this.definition.put(key, value);
      return (this);
   }



   /*******************************************************************************
    ** Getter for perRecord
    *******************************************************************************/
   public Boolean getPerRecord()
   {
      return (this.perRecord);
   }



   /*******************************************************************************
    ** Fluent setter for perRecord (one alert per matching record, instead of one
    ** aggregate alert)
    *******************************************************************************/
   public AlertDefaultMonitor withPerRecord(Boolean perRecord)
   {
      this.perRecord = perRecord;
      return (this);
   }



   /*******************************************************************************
    ** Getter for severity
    *******************************************************************************/
   public AlertSeverity getSeverity()
   {
      return (this.severity);
   }



   /*******************************************************************************
    ** Fluent setter for severity (default: the type's severity)
    *******************************************************************************/
   public AlertDefaultMonitor withSeverity(AlertSeverity severity)
   {
      this.severity = severity;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scheduleSeconds
    *******************************************************************************/
   public Integer getScheduleSeconds()
   {
      return (this.scheduleSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for scheduleSeconds (how often to evaluate)
    *******************************************************************************/
   public AlertDefaultMonitor withScheduleSeconds(Integer scheduleSeconds)
   {
      this.scheduleSeconds = scheduleSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for forMinutes
    *******************************************************************************/
   public Integer getForMinutes()
   {
      return (this.forMinutes);
   }



   /*******************************************************************************
    ** Fluent setter for forMinutes (how long a condition must hold before it fires)
    *******************************************************************************/
   public AlertDefaultMonitor withForMinutes(Integer forMinutes)
   {
      this.forMinutes = forMinutes;
      return (this);
   }
}
