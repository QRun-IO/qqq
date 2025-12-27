/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.javalin;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import org.apache.logging.log4j.Level;


/*******************************************************************************
 ** MetaData specific to a QQQ Javalin server.
 *******************************************************************************/
public class QJavalinMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "javalin";

   private String uploadedFileArchiveTableName;

   private boolean loggerDisabled = false;

   // todo - should be a code reference!!
   private Function<QJavalinAccessLogger.LogEntry, Boolean> logFilter;

   private boolean queryWithoutLimitAllowed  = false;
   private Integer queryWithoutLimitDefault  = 1000;
   private Level   queryWithoutLimitLogLevel = Level.INFO;

   private List<JavalinRouteProviderMetaData> routeProviders;
   private List<QCodeReference>               additionalRouteProviderReferences;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QJavalinMetaData of(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.of(qInstance, NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QJavalinMetaData ofOrWithNew(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, QJavalinMetaData::new);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QJavalinMetaData()
   {
      loggerDisabled = System.getProperty(QJavalinAccessLogger.DISABLED_PROPERTY, "false").equals("true");
   }



   /*******************************************************************************
    ** Getter for uploadedFileArchiveTableName
    **
    *******************************************************************************/
   public String getUploadedFileArchiveTableName()
   {
      return uploadedFileArchiveTableName;
   }



   /*******************************************************************************
    ** Setter for uploadedFileArchiveTableName
    **
    *******************************************************************************/
   public void setUploadedFileArchiveTableName(String uploadedFileArchiveTableName)
   {
      this.uploadedFileArchiveTableName = uploadedFileArchiveTableName;
   }



   /*******************************************************************************
    ** Fluent setter for uploadedFileArchiveTableName
    **
    *******************************************************************************/
   public QJavalinMetaData withUploadedFileArchiveTableName(String uploadedFileArchiveTableName)
   {
      this.uploadedFileArchiveTableName = uploadedFileArchiveTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for loggerDisabled
    *******************************************************************************/
   public boolean getLoggerDisabled()
   {
      return (this.loggerDisabled);
   }



   /*******************************************************************************
    ** Setter for loggerDisabled
    *******************************************************************************/
   public void setLoggerDisabled(boolean loggerDisabled)
   {
      this.loggerDisabled = loggerDisabled;
   }



   /*******************************************************************************
    ** Fluent setter for loggerDisabled
    *******************************************************************************/
   public QJavalinMetaData withLoggerDisabled(boolean loggerDisabled)
   {
      this.loggerDisabled = loggerDisabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for logFilter
    *******************************************************************************/
   public Function<QJavalinAccessLogger.LogEntry, Boolean> getLogFilter()
   {
      return (this.logFilter);
   }



   /*******************************************************************************
    ** Setter for logFilter
    *******************************************************************************/
   public void setLogFilter(Function<QJavalinAccessLogger.LogEntry, Boolean> logFilter)
   {
      this.logFilter = logFilter;
   }



   /*******************************************************************************
    ** Fluent setter for logFilter
    *******************************************************************************/
   public QJavalinMetaData withLogFilter(Function<QJavalinAccessLogger.LogEntry, Boolean> logFilter)
   {
      this.logFilter = logFilter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryWithoutLimitAllowed
    *******************************************************************************/
   public boolean getQueryWithoutLimitAllowed()
   {
      return (this.queryWithoutLimitAllowed);
   }



   /*******************************************************************************
    ** Setter for queryWithoutLimitAllowed
    *******************************************************************************/
   public void setQueryWithoutLimitAllowed(boolean queryWithoutLimitAllowed)
   {
      this.queryWithoutLimitAllowed = queryWithoutLimitAllowed;
   }



   /*******************************************************************************
    ** Fluent setter for queryWithoutLimitAllowed
    *******************************************************************************/
   public QJavalinMetaData withQueryWithoutLimitAllowed(boolean queryWithoutLimitAllowed)
   {
      this.queryWithoutLimitAllowed = queryWithoutLimitAllowed;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryWithoutLimitDefault
    *******************************************************************************/
   public Integer getQueryWithoutLimitDefault()
   {
      return (this.queryWithoutLimitDefault);
   }



   /*******************************************************************************
    ** Setter for queryWithoutLimitDefault
    *******************************************************************************/
   public void setQueryWithoutLimitDefault(Integer queryWithoutLimitDefault)
   {
      this.queryWithoutLimitDefault = queryWithoutLimitDefault;
   }



   /*******************************************************************************
    ** Fluent setter for queryWithoutLimitDefault
    *******************************************************************************/
   public QJavalinMetaData withQueryWithoutLimitDefault(Integer queryWithoutLimitDefault)
   {
      this.queryWithoutLimitDefault = queryWithoutLimitDefault;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryWithoutLimitLogLevel
    *******************************************************************************/
   public Level getQueryWithoutLimitLogLevel()
   {
      return (this.queryWithoutLimitLogLevel);
   }



   /*******************************************************************************
    ** Setter for queryWithoutLimitLogLevel
    *******************************************************************************/
   public void setQueryWithoutLimitLogLevel(Level queryWithoutLimitLogLevel)
   {
      this.queryWithoutLimitLogLevel = queryWithoutLimitLogLevel;
   }



   /*******************************************************************************
    ** Fluent setter for queryWithoutLimitLogLevel
    *******************************************************************************/
   public QJavalinMetaData withQueryWithoutLimitLogLevel(Level queryWithoutLimitLogLevel)
   {
      this.queryWithoutLimitLogLevel = queryWithoutLimitLogLevel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for routeProviders
    *******************************************************************************/
   public List<JavalinRouteProviderMetaData> getRouteProviders()
   {
      return (this.routeProviders);
   }



   /*******************************************************************************
    ** Setter for routeProviders
    *******************************************************************************/
   public void setRouteProviders(List<JavalinRouteProviderMetaData> routeProviders)
   {
      this.routeProviders = routeProviders;
   }



   /*******************************************************************************
    ** Fluent setter for routeProviders
    *******************************************************************************/
   public QJavalinMetaData withRouteProviders(List<JavalinRouteProviderMetaData> routeProviders)
   {
      this.routeProviders = routeProviders;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add 1 routeProvider
    *******************************************************************************/
   public QJavalinMetaData withRouteProvider(JavalinRouteProviderMetaData routeProvider)
   {
      if(this.routeProviders == null)
      {
         this.routeProviders = new ArrayList<>();
      }
      this.routeProviders.add(routeProvider);
      return (this);
   }



   /*******************************************************************************
    ** Getter for additionalRouteProviderReferences
    *******************************************************************************/
   public List<QCodeReference> getAdditionalRouteProviderReferences()
   {
      return (this.additionalRouteProviderReferences);
   }



   /*******************************************************************************
    ** Setter for additionalRouteProviderReferences
    *******************************************************************************/
   public void setAdditionalRouteProviderReferences(List<QCodeReference> additionalRouteProviderReferences)
   {
      this.additionalRouteProviderReferences = additionalRouteProviderReferences;
   }



   /*******************************************************************************
    ** Fluent setter for additionalRouteProviderReferences
    *******************************************************************************/
   public QJavalinMetaData withAdditionalRouteProviderReferences(List<QCodeReference> additionalRouteProviderReferences)
   {
      this.additionalRouteProviderReferences = additionalRouteProviderReferences;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter to add a single additionalRouteProviderReference
    *******************************************************************************/
   public QJavalinMetaData withAdditionalRouteProviderReference(QCodeReference additionalRouteProviderReference)
   {
      if(this.additionalRouteProviderReferences == null)
      {
         this.additionalRouteProviderReferences = new ArrayList<>();
      }
      this.additionalRouteProviderReferences.add(additionalRouteProviderReference);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(JavalinRouteProviderMetaData routeProviderMetaData : CollectionUtils.nonNullList(routeProviders))
      {
         routeProviderMetaData.validate(qInstance, validator);
      }
   }
}
