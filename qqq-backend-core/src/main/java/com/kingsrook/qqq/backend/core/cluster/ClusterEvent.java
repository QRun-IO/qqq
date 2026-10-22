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

package com.kingsrook.qqq.backend.core.cluster;


import java.io.Serializable;
import java.util.UUID;


/*******************************************************************************
 ** A control event sent to every node of a cluster (ClusterBus): something a
 ** node changed that the other nodes hold in memory. Plain string fields, so a
 ** transport can send it as JSON.
 *******************************************************************************/
public class ClusterEvent implements Serializable
{
   /** a user's session ended (key: the session UUID): drop its cached tokens and sessions */
   public static final String TYPE_SESSION_ENDED = "session-ended";

   /** the runtime meta data overlay changed: reload it */
   public static final String TYPE_METADATA_OVERLAY_CHANGED = "metadata-overlay-changed";

   /** a setting changed (name: the setting key) */
   public static final String TYPE_SETTING_CHANGED = "setting-changed";

   /** a named cache is stale (name: the cache; key: one entry, or null for all) */
   public static final String TYPE_MEMO_INVALIDATE = "memo-invalidate";

   private String id = UUID.randomUUID().toString();
   private String type;
   private String name;
   private String key;
   private String originNodeId;



   /*******************************************************************************
    ** Constructor (for deserialization).
    *******************************************************************************/
   public ClusterEvent()
   {
   }



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public ClusterEvent(String type, String name, String key)
   {
      this.type = type;
      this.name = name;
      this.key = key;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("ClusterEvent{type=" + type + ", name=" + name + ", origin=" + originNodeId + "}");
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public String getId()
   {
      return (id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(String id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public String getType()
   {
      return (type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(String type)
   {
      this.type = type;
   }



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
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Getter for originNodeId
    *******************************************************************************/
   public String getOriginNodeId()
   {
      return (originNodeId);
   }



   /*******************************************************************************
    ** Setter for originNodeId
    *******************************************************************************/
   public void setOriginNodeId(String originNodeId)
   {
      this.originNodeId = originNodeId;
   }
}
