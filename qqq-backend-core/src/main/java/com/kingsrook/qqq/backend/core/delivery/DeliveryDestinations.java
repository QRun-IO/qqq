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


import java.util.Comparator;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;


/*******************************************************************************
 ** Shared permission boundary for pickers and delivery execution.
 *******************************************************************************/
public final class DeliveryDestinations
{
   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private DeliveryDestinations()
   {
   }



   /*******************************************************************************
    ** Only destinations visible to the authenticated user.
    *******************************************************************************/
   public static List<QDeliveryDestinationMetaData> visible() throws QException
   {
      if(QContext.getQSession() == null || QContext.getQSession().getUser() == null)
      {
         throw new QPermissionDeniedException("Sign in to use delivery destinations.");
      }
      DeliveryDestinationsMetaData registry = DeliveryDestinationsMetaData.of(QContext.getQInstance());
      return (registry == null ? List.of() : registry.getDestinations().stream()
         .filter(destination -> destination.getPermissionName() == null || QContext.getQSession().hasPermission(destination.getPermissionName()))
         .sorted(Comparator.comparing(QDeliveryDestinationMetaData::getOrder, Comparator.nullsLast(Comparator.naturalOrder())))
         .toList());
   }



   /*******************************************************************************
    ** Hidden and unknown names have the same result.
    *******************************************************************************/
   public static QDeliveryDestinationMetaData find(String name) throws QException
   {
      return (visible().stream().filter(destination -> destination.getName().equals(name)).findFirst().orElse(null));
   }



   /*******************************************************************************
    ** Load a validated implementation.
    *******************************************************************************/
   public static DeliveryDestinationInterface implementation(QDeliveryDestinationMetaData destination) throws QException
   {
      return (QCodeLoader.getAdHoc(DeliveryDestinationInterface.class, destination.getCodeReference()));
   }
}
