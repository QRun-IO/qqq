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

package com.kingsrook.qqq.middleware.javalin;


import java.util.function.Consumer;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Settings for QApplicationLauncher:
 ** - serverCustomizer: configures the QApplicationJavalinServer before it
 **   starts (port, dashboards, route providers, etc).
 ** - failOnMetaDataProducerError: make a producer that fails stop the launch
 **   (needs an AbstractMetaDataProducerBasedQQQApplication).  Default false.
 ** - systemUserSessionSupplier: session the QScheduleManager runs as.  Default
 **   a QSystemUserSession.
 ** - registerShutdownHook: stop everything when the JVM exits.  Default true.
 *******************************************************************************/
public class QApplicationLauncherConfig
{
   private Consumer<QApplicationJavalinServer> serverCustomizer            = null;
   private Boolean                             failOnMetaDataProducerError = false;
   private Supplier<QSession>                  systemUserSessionSupplier   = QSystemUserSession::new;
   private Boolean                             registerShutdownHook        = true;



   /*******************************************************************************
    ** Getter for serverCustomizer
    *******************************************************************************/
   public Consumer<QApplicationJavalinServer> getServerCustomizer()
   {
      return (this.serverCustomizer);
   }



   /*******************************************************************************
    ** Setter for serverCustomizer
    *******************************************************************************/
   public void setServerCustomizer(Consumer<QApplicationJavalinServer> serverCustomizer)
   {
      this.serverCustomizer = serverCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for serverCustomizer - called with the server before it
    ** starts.
    *******************************************************************************/
   public QApplicationLauncherConfig withServerCustomizer(Consumer<QApplicationJavalinServer> serverCustomizer)
   {
      this.serverCustomizer = serverCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for failOnMetaDataProducerError
    *******************************************************************************/
   public Boolean getFailOnMetaDataProducerError()
   {
      return (this.failOnMetaDataProducerError);
   }



   /*******************************************************************************
    ** Setter for failOnMetaDataProducerError
    *******************************************************************************/
   public void setFailOnMetaDataProducerError(Boolean failOnMetaDataProducerError)
   {
      this.failOnMetaDataProducerError = failOnMetaDataProducerError;
   }



   /*******************************************************************************
    ** Fluent setter for failOnMetaDataProducerError - if true, a meta-data
    ** producer that can't be used or fails stops the launch, instead of being
    ** logged as a warning and skipped.
    *******************************************************************************/
   public QApplicationLauncherConfig withFailOnMetaDataProducerError(Boolean failOnMetaDataProducerError)
   {
      this.failOnMetaDataProducerError = failOnMetaDataProducerError;
      return (this);
   }



   /*******************************************************************************
    ** Getter for systemUserSessionSupplier
    *******************************************************************************/
   public Supplier<QSession> getSystemUserSessionSupplier()
   {
      return (this.systemUserSessionSupplier);
   }



   /*******************************************************************************
    ** Setter for systemUserSessionSupplier
    *******************************************************************************/
   public void setSystemUserSessionSupplier(Supplier<QSession> systemUserSessionSupplier)
   {
      this.systemUserSessionSupplier = systemUserSessionSupplier;
   }



   /*******************************************************************************
    ** Fluent setter for systemUserSessionSupplier - the session scheduled jobs
    ** run as.
    *******************************************************************************/
   public QApplicationLauncherConfig withSystemUserSessionSupplier(Supplier<QSession> systemUserSessionSupplier)
   {
      this.systemUserSessionSupplier = systemUserSessionSupplier;
      return (this);
   }



   /*******************************************************************************
    ** Getter for registerShutdownHook
    *******************************************************************************/
   public Boolean getRegisterShutdownHook()
   {
      return (this.registerShutdownHook);
   }



   /*******************************************************************************
    ** Setter for registerShutdownHook
    *******************************************************************************/
   public void setRegisterShutdownHook(Boolean registerShutdownHook)
   {
      this.registerShutdownHook = registerShutdownHook;
   }



   /*******************************************************************************
    ** Fluent setter for registerShutdownHook - if true, a JVM shutdown hook
    ** stops everything the launcher started.
    *******************************************************************************/
   public QApplicationLauncherConfig withRegisterShutdownHook(Boolean registerShutdownHook)
   {
      this.registerShutdownHook = registerShutdownHook;
      return (this);
   }

}
