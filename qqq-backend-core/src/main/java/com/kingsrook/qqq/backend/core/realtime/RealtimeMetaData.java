/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.realtime;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Instance realtime transport configuration. Limits are strictly positive.
 *******************************************************************************/
public class RealtimeMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "realtime";
   @JsonIgnore
   private QCodeReference bus;
   private int replaySeconds = 120;
   private int heartbeatSeconds = 25;
   private int maxChannelsPerConnection = 200;
   private int maxConnectionsPerSession = 20;
   private int recheckMinutes = 5;



   /*******************************************************************************
    ** Read configuration without mutating the instance.
    *******************************************************************************/
   public static RealtimeMetaData of(QInstance instance)
   {
      RealtimeMetaData metadata = QSupplementalInstanceMetaData.of(instance, NAME);
      return (metadata == null ? new RealtimeMetaData() : metadata);
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
    ** Reject unusable retention, scheduling and connection limits.
    *******************************************************************************/
   @Override
   public void validate(QInstance instance, QInstanceValidator validator)
   {
      validator.assertCondition(replaySeconds > 0 && heartbeatSeconds > 0 && maxChannelsPerConnection > 0
         && maxConnectionsPerSession > 0 && recheckMinutes > 0, "Realtime limits must be positive.");
   }




   /*******************************************************************************
    ** Getter for bus
    *******************************************************************************/
   public QCodeReference getBus()
   {
      return (bus);
   }




   /*******************************************************************************
    ** Setter for bus
    *******************************************************************************/
   public void setBus(QCodeReference bus)
   {
      this.bus = bus;
   }




   /*******************************************************************************
    ** Fluent setter for bus
    *******************************************************************************/
   public RealtimeMetaData withBus(QCodeReference bus)
   {
      setBus(bus);
      return (this);
   }




   /*******************************************************************************
    ** Getter for replaySeconds
    *******************************************************************************/
   public int getReplaySeconds()
   {
      return (replaySeconds);
   }




   /*******************************************************************************
    ** Setter for replaySeconds
    *******************************************************************************/
   public void setReplaySeconds(int replaySeconds)
   {
      this.replaySeconds = replaySeconds;
   }




   /*******************************************************************************
    ** Fluent setter for replaySeconds
    *******************************************************************************/
   public RealtimeMetaData withReplaySeconds(int replaySeconds)
   {
      setReplaySeconds(replaySeconds);
      return (this);
   }




   /*******************************************************************************
    ** Getter for heartbeatSeconds
    *******************************************************************************/
   public int getHeartbeatSeconds()
   {
      return (heartbeatSeconds);
   }




   /*******************************************************************************
    ** Setter for heartbeatSeconds
    *******************************************************************************/
   public void setHeartbeatSeconds(int heartbeatSeconds)
   {
      this.heartbeatSeconds = heartbeatSeconds;
   }




   /*******************************************************************************
    ** Fluent setter for heartbeatSeconds
    *******************************************************************************/
   public RealtimeMetaData withHeartbeatSeconds(int heartbeatSeconds)
   {
      setHeartbeatSeconds(heartbeatSeconds);
      return (this);
   }




   /*******************************************************************************
    ** Getter for maxChannelsPerConnection
    *******************************************************************************/
   public int getMaxChannelsPerConnection()
   {
      return (maxChannelsPerConnection);
   }




   /*******************************************************************************
    ** Setter for maxChannelsPerConnection
    *******************************************************************************/
   public void setMaxChannelsPerConnection(int maxChannelsPerConnection)
   {
      this.maxChannelsPerConnection = maxChannelsPerConnection;
   }




   /*******************************************************************************
    ** Fluent setter for maxChannelsPerConnection
    *******************************************************************************/
   public RealtimeMetaData withMaxChannelsPerConnection(int maxChannelsPerConnection)
   {
      setMaxChannelsPerConnection(maxChannelsPerConnection);
      return (this);
   }




   /*******************************************************************************
    ** Getter for maxConnectionsPerSession
    *******************************************************************************/
   public int getMaxConnectionsPerSession()
   {
      return (maxConnectionsPerSession);
   }




   /*******************************************************************************
    ** Setter for maxConnectionsPerSession
    *******************************************************************************/
   public void setMaxConnectionsPerSession(int maxConnectionsPerSession)
   {
      this.maxConnectionsPerSession = maxConnectionsPerSession;
   }




   /*******************************************************************************
    ** Fluent setter for maxConnectionsPerSession
    *******************************************************************************/
   public RealtimeMetaData withMaxConnectionsPerSession(int maxConnectionsPerSession)
   {
      setMaxConnectionsPerSession(maxConnectionsPerSession);
      return (this);
   }




   /*******************************************************************************
    ** Getter for recheckMinutes
    *******************************************************************************/
   public int getRecheckMinutes()
   {
      return (recheckMinutes);
   }




   /*******************************************************************************
    ** Setter for recheckMinutes
    *******************************************************************************/
   public void setRecheckMinutes(int recheckMinutes)
   {
      this.recheckMinutes = recheckMinutes;
   }




   /*******************************************************************************
    ** Fluent setter for recheckMinutes
    *******************************************************************************/
   public RealtimeMetaData withRecheckMinutes(int recheckMinutes)
   {
      setRecheckMinutes(recheckMinutes);
      return (this);
   }
}

