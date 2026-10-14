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

package com.kingsrook.qqq.backend.core.delivery;


import java.util.List;


/*******************************************************************************
 ** DeliveryResult.
 *******************************************************************************/
public class DeliveryResult implements java.io.Serializable
{
   /*******************************************************************************
    ** Recorded delivery outcome.
    *******************************************************************************/
   public enum Status
   {
      DELIVERED, QUEUED, REFUSED, FAILED_RETRYABLE, FAILED, UNAUTHORIZED
   }

   private Status status;
   private String externalReference;
   private List<String> reasons = List.of();



   /*******************************************************************************
    ** Get status.
    *******************************************************************************/
   public Status getStatus()
   {
      return (status);
   }



   /*******************************************************************************
    ** Set status.
    *******************************************************************************/
   public void setStatus(Status status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** With status.
    *******************************************************************************/
   public DeliveryResult withStatus(Status status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Get externalReference.
    *******************************************************************************/
   public String getExternalReference()
   {
      return (externalReference);
   }



   /*******************************************************************************
    ** Set externalReference.
    *******************************************************************************/
   public void setExternalReference(String externalReference)
   {
      this.externalReference = externalReference;
   }



   /*******************************************************************************
    ** With externalReference.
    *******************************************************************************/
   public DeliveryResult withExternalReference(String externalReference)
   {
      this.externalReference = externalReference;
      return (this);
   }



   /*******************************************************************************
    ** Get reasons.
    *******************************************************************************/
   public List<String> getReasons()
   {
      return (reasons);
   }



   /*******************************************************************************
    ** Set reasons.
    *******************************************************************************/
   public void setReasons(List<String> reasons)
   {
      this.reasons = reasons;
   }



   /*******************************************************************************
    ** With reasons.
    *******************************************************************************/
   public DeliveryResult withReasons(List<String> reasons)
   {
      this.reasons = reasons;
      return (this);
   }
}
