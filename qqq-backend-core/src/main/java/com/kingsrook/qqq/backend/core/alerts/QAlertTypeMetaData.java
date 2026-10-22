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
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.SourceQBitAware;


/*******************************************************************************
 ** Declares a kind of problem a qbit or app can report (e.g. cdn.purge.dead),
 ** so the system "knows about" every alert source.  An alerts implementation
 ** syncs these into a table at startup, where admins can see and tune them.
 **
 ** Add it to an instance like any top-level meta-data (qInstance.add(...), or
 ** from a MetaDataProducer); it lands in AlertsInstanceMetaData.alertTypes.
 ** Names are lowercase and dotted: ^[a-z0-9]+(\.[a-z0-9-]+)+$
 *******************************************************************************/
public class QAlertTypeMetaData implements TopLevelMetaDataInterface, SourceQBitAware
{
   private String                    name;
   private String                    label;
   private String                    sourceName;
   private String                    sourceQBitName;
   private String                    groupName;
   private AlertSeverity             defaultSeverity       = AlertSeverity.WARNING;
   private String                    description;
   private String                    howToFix;
   private String                    subjectTableName;
   private String                    visibilityPermission;
   private ResolveMode               resolveMode           = ResolveMode.PRODUCER;
   private Duration                  quietPeriod;
   private List<AlertDefaultMonitor> defaultMonitors       = new ArrayList<>();
   private DetailLevel               externalDetailCeiling = DetailLevel.STANDARD;



   /*******************************************************************************
    ** Find a declared alert type by name, or null.
    *******************************************************************************/
   public static QAlertTypeMetaData of(QInstance qInstance, String name)
   {
      AlertsInstanceMetaData alertsInstanceMetaData = AlertsInstanceMetaData.of(qInstance);
      return (alertsInstanceMetaData == null ? null : alertsInstanceMetaData.getAlertType(name));
   }



   /*******************************************************************************
    ** Adds this type to the instance's AlertsInstanceMetaData (creating it).
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      AlertsInstanceMetaData.ofOrWithNew(qInstance).withAlertType(this);
   }



   /*******************************************************************************
    ** The source shown to people: sourceName if set, otherwise the qbit that
    ** produced this meta-data.
    *******************************************************************************/
   public String getEffectiveSourceName()
   {
      return (sourceName != null ? sourceName : sourceQBitName);
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (this.name);
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
   public QAlertTypeMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public QAlertTypeMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceName
    *******************************************************************************/
   public String getSourceName()
   {
      return (this.sourceName);
   }



   /*******************************************************************************
    ** Fluent setter for sourceName
    *******************************************************************************/
   public QAlertTypeMetaData withSourceName(String sourceName)
   {
      this.sourceName = sourceName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceQBitName
    *******************************************************************************/
   @Override
   public String getSourceQBitName()
   {
      return (this.sourceQBitName);
   }



   /*******************************************************************************
    ** Setter for sourceQBitName
    *******************************************************************************/
   @Override
   public void setSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;
   }



   /*******************************************************************************
    ** Fluent setter for sourceQBitName
    *******************************************************************************/
   @Override
   public QAlertTypeMetaData withSourceQBitName(String sourceQBitName)
   {
      this.sourceQBitName = sourceQBitName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for groupName
    *******************************************************************************/
   public String getGroupName()
   {
      return (this.groupName);
   }



   /*******************************************************************************
    ** Fluent setter for groupName (the default group; created if missing)
    *******************************************************************************/
   public QAlertTypeMetaData withGroupName(String groupName)
   {
      this.groupName = groupName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultSeverity
    *******************************************************************************/
   public AlertSeverity getDefaultSeverity()
   {
      return (this.defaultSeverity);
   }



   /*******************************************************************************
    ** Fluent setter for defaultSeverity
    *******************************************************************************/
   public QAlertTypeMetaData withDefaultSeverity(AlertSeverity defaultSeverity)
   {
      this.defaultSeverity = defaultSeverity;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public QAlertTypeMetaData withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for howToFix
    *******************************************************************************/
   public String getHowToFix()
   {
      return (this.howToFix);
   }



   /*******************************************************************************
    ** Fluent setter for howToFix
    *******************************************************************************/
   public QAlertTypeMetaData withHowToFix(String howToFix)
   {
      this.howToFix = howToFix;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subjectTableName
    *******************************************************************************/
   public String getSubjectTableName()
   {
      return (this.subjectTableName);
   }



   /*******************************************************************************
    ** Fluent setter for subjectTableName (drives visibility and the record panel)
    *******************************************************************************/
   public QAlertTypeMetaData withSubjectTableName(String subjectTableName)
   {
      this.subjectTableName = subjectTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for visibilityPermission
    *******************************************************************************/
   public String getVisibilityPermission()
   {
      return (this.visibilityPermission);
   }



   /*******************************************************************************
    ** Fluent setter for visibilityPermission (an extra gate on who may see it)
    *******************************************************************************/
   public QAlertTypeMetaData withVisibilityPermission(String visibilityPermission)
   {
      this.visibilityPermission = visibilityPermission;
      return (this);
   }



   /*******************************************************************************
    ** Getter for resolveMode
    *******************************************************************************/
   public ResolveMode getResolveMode()
   {
      return (this.resolveMode);
   }



   /*******************************************************************************
    ** Fluent setter for resolveMode
    *******************************************************************************/
   public QAlertTypeMetaData withResolveMode(ResolveMode resolveMode)
   {
      this.resolveMode = resolveMode;
      return (this);
   }



   /*******************************************************************************
    ** Resolve automatically after this long with no raise (sets the mode to
    ** QUIET_PERIOD).
    *******************************************************************************/
   public QAlertTypeMetaData withQuietPeriod(Duration quietPeriod)
   {
      this.quietPeriod = quietPeriod;
      this.resolveMode = ResolveMode.QUIET_PERIOD;
      return (this);
   }



   /*******************************************************************************
    ** Getter for quietPeriod
    *******************************************************************************/
   public Duration getQuietPeriod()
   {
      return (this.quietPeriod);
   }



   /*******************************************************************************
    ** Getter for defaultMonitors
    *******************************************************************************/
   public List<AlertDefaultMonitor> getDefaultMonitors()
   {
      return (this.defaultMonitors);
   }



   /*******************************************************************************
    ** Add a seeded monitor for this type.
    *******************************************************************************/
   public QAlertTypeMetaData withDefaultMonitor(AlertDefaultMonitor defaultMonitor)
   {
      if(defaultMonitor != null)
      {
         this.defaultMonitors.add(defaultMonitor);
      }
      return (this);
   }



   /*******************************************************************************
    ** Getter for externalDetailCeiling
    *******************************************************************************/
   public DetailLevel getExternalDetailCeiling()
   {
      return (this.externalDetailCeiling);
   }



   /*******************************************************************************
    ** Fluent setter for externalDetailCeiling (the most any channel may include)
    *******************************************************************************/
   public QAlertTypeMetaData withExternalDetailCeiling(DetailLevel externalDetailCeiling)
   {
      this.externalDetailCeiling = externalDetailCeiling;
      return (this);
   }
}
