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

package com.kingsrook.qqq.backend.core.state;


import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


/*******************************************************************************
 **
 *******************************************************************************/
public class UUIDAndTypeStateKey extends AbstractStateKey implements Serializable
{
   private final UUID      uuid;
   private final StateType stateType;
   private final Instant   startTime;



   /*******************************************************************************
    ** Default constructor - assigns a random UUID.
    **
    *******************************************************************************/
   public UUIDAndTypeStateKey(StateType stateType)
   {
      this(UUID.randomUUID(), stateType, Instant.now());
   }



   /*******************************************************************************
    ** Constructor where user can supply the UUID.
    **
    *******************************************************************************/
   public UUIDAndTypeStateKey(UUID uuid, StateType stateType)
   {
      this(uuid, stateType, Instant.now());
   }



   /*******************************************************************************
    ** Constructor where user can supply the UUID.
    **
    *******************************************************************************/
   public UUIDAndTypeStateKey(UUID uuid, StateType stateType, Instant startTime)
   {
      this.uuid = uuid;
      this.stateType = stateType;
      this.startTime = startTime;
   }



   /*******************************************************************************
    ** Getter for uuid
    **
    *******************************************************************************/
   public UUID getUuid()
   {
      return uuid;
   }



   /*******************************************************************************
    ** Getter for stateType
    **
    *******************************************************************************/
   public StateType getStateType()
   {
      return stateType;
   }



   /*******************************************************************************
    ** Make the key give a unique string to identify itself.
    *
    *******************************************************************************/
   @Override
   public String getUniqueIdentifier()
   {
      return (uuid.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      UUIDAndTypeStateKey that = (UUIDAndTypeStateKey) o;
      return Objects.equals(uuid, that.uuid) && stateType == that.stateType;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(uuid, stateType);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "{uuid=" + uuid + ", stateType=" + stateType + '}';
   }



   /*******************************************************************************
    ** Getter for startTime
    *******************************************************************************/
   public Instant getStartTime()
   {
      return (this.startTime);
   }

}
