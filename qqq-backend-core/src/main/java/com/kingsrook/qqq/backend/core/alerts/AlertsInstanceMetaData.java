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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Instance-level alerts meta-data: the sink that QAlerts sends signals to
 ** (default: LoggingAlertSink) and every declared QAlertTypeMetaData.  It is
 ** supplemental meta-data, so any module or qbit can add to it.
 *******************************************************************************/
public class AlertsInstanceMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "alerts";

   /** alert type names: lowercase, dotted, at least two segments. */
   public static final Pattern ALERT_TYPE_NAME_PATTERN = Pattern.compile("^[a-z0-9]+(\\.[a-z0-9-]+)+$");

   private QCodeReference                  sink;
   private Map<String, QAlertTypeMetaData> alertTypes          = new LinkedHashMap<>();
   private List<String>                    duplicateAlertTypes = new ArrayList<>();

   private transient AlertSinkInterface loadedSink;



   /*******************************************************************************
    ** Get the instance's alerts meta-data, or null when nothing added it.
    *******************************************************************************/
   public static AlertsInstanceMetaData of(QInstance qInstance)
   {
      return (qInstance == null ? null : QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** Get the instance's alerts meta-data, adding an empty one if it has none.
    *******************************************************************************/
   public static AlertsInstanceMetaData ofOrWithNew(QInstance qInstance)
   {
      return (QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, AlertsInstanceMetaData::new));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Getter for sink
    *******************************************************************************/
   public QCodeReference getSink()
   {
      return (this.sink);
   }



   /*******************************************************************************
    ** Fluent setter for sink - an AlertSinkInterface implementation.  An
    ** alerts implementation sets this; producers never do.
    *******************************************************************************/
   public AlertsInstanceMetaData withSink(QCodeReference sink)
   {
      this.sink = sink;
      this.loadedSink = null;
      return (this);
   }



   /*******************************************************************************
    ** The sink instance QAlerts loaded for this meta-data (cached; null until
    ** first use).
    *******************************************************************************/
   AlertSinkInterface getLoadedSink()
   {
      return (this.loadedSink);
   }



   /*******************************************************************************
    ** Cache the loaded sink instance.
    *******************************************************************************/
   void setLoadedSink(AlertSinkInterface loadedSink)
   {
      this.loadedSink = loadedSink;
   }



   /*******************************************************************************
    ** Getter for alertTypes
    *******************************************************************************/
   public Map<String, QAlertTypeMetaData> getAlertTypes()
   {
      return (this.alertTypes);
   }



   /*******************************************************************************
    ** Find a declared type by name, or null.
    *******************************************************************************/
   public QAlertTypeMetaData getAlertType(String name)
   {
      return (name == null ? null : alertTypes.get(name));
   }



   /*******************************************************************************
    ** Declare an alert type.  Adding the same object again is a no-op; adding a
    ** different type with a name already taken is a validation error.
    *******************************************************************************/
   public AlertsInstanceMetaData withAlertType(QAlertTypeMetaData alertType)
   {
      if(alertType == null)
      {
         return (this);
      }

      QAlertTypeMetaData existing = alertTypes.get(alertType.getName());
      if(existing != null && existing != alertType)
      {
         duplicateAlertTypes.add(alertType.getName());
         return (this);
      }

      alertTypes.put(alertType.getName(), alertType);
      return (this);
   }



   /*******************************************************************************
    ** Names must be unique and well-formed, subject tables must exist, a
    ** visibility permission must not be blank, heartbeat default monitors need
    ** an interval, and the sink must implement AlertSinkInterface.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      if(sink != null)
      {
         validator.validateSimpleCodeReference("Alerts sink: ", sink, AlertSinkInterface.class);
      }

      for(String duplicate : duplicateAlertTypes)
      {
         validator.assertCondition(false, "Alert type " + duplicate + " is declared more than once.");
      }

      for(Map.Entry<String, QAlertTypeMetaData> entry : alertTypes.entrySet())
      {
         String             name      = entry.getKey();
         QAlertTypeMetaData alertType = entry.getValue();
         String             prefix    = "Alert type " + name + ": ";

         if(!validator.assertCondition(name != null && ALERT_TYPE_NAME_PATTERN.matcher(name).matches(), prefix + "name must be lowercase and dotted (e.g. cdn.purge.dead), matching " + ALERT_TYPE_NAME_PATTERN.pattern()))
         {
            continue;
         }

         validator.assertCondition(alertType.getDefaultSeverity() != null, prefix + "defaultSeverity is required.");

         if(alertType.getSubjectTableName() != null)
         {
            validator.assertCondition(qInstance.getTable(alertType.getSubjectTableName()) != null, prefix + "subjectTableName " + alertType.getSubjectTableName() + " is not a table in the instance.");
         }

         if(alertType.getVisibilityPermission() != null)
         {
            validator.assertCondition(!alertType.getVisibilityPermission().isBlank(), prefix + "visibilityPermission must not be blank.");
         }

         if(alertType.getResolveMode() == ResolveMode.QUIET_PERIOD)
         {
            validator.assertCondition(alertType.getQuietPeriod() != null && !alertType.getQuietPeriod().isNegative() && !alertType.getQuietPeriod().isZero(), prefix + "a QUIET_PERIOD resolve mode needs a positive quietPeriod.");
         }

         for(AlertDefaultMonitor monitor : alertType.getDefaultMonitors())
         {
            validator.assertCondition(monitor.getKind() != null, prefix + "a default monitor needs a kind.");
            if(monitor.getKind() == AlertMonitorKind.HEARTBEAT)
            {
               Object every = monitor.getDefinition().get("expectedEverySeconds");
               validator.assertCondition(every instanceof Number number && number.longValue() > 0, prefix + "a heartbeat default monitor needs a positive expectedEverySeconds.");
            }
         }
      }
   }
}
