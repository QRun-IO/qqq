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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/*******************************************************************************
 ** Who a user notification is for.  Construct these with the static factories;
 ** an alerts implementation resolves them to user ids when it sends:
 ** - ALL_USERS: every active user.
 ** - ROLES: users with any of the named roles (values are role names).
 ** - PERMISSION: users holding one permission (values has one entry).
 ** - USERS: the named user ids, as given.
 **
 ** Immutable.  Null and blank values are dropped.
 *******************************************************************************/
public final class NotificationAudience implements Serializable
{
   /***************************************************************************
    ** The kinds of audience.
    ***************************************************************************/
   public enum Type
   {
      ALL_USERS,
      ROLES,
      PERMISSION,
      USERS
   }



   private final Type         type;
   private final List<String> values;



   /*******************************************************************************
    ** Constructor - use the static factories.
    *******************************************************************************/
   private NotificationAudience(Type type, String... values)
   {
      this.type = type;

      List<String> list = new ArrayList<>();
      if(values != null)
      {
         Arrays.stream(values).filter(v -> v != null && !v.isBlank()).forEach(list::add);
      }
      this.values = Collections.unmodifiableList(list);
   }



   /*******************************************************************************
    ** Every active user.
    *******************************************************************************/
   public static NotificationAudience allUsers()
   {
      return (new NotificationAudience(Type.ALL_USERS));
   }



   /*******************************************************************************
    ** Users with any of the named roles.
    *******************************************************************************/
   public static NotificationAudience roles(String... roleNames)
   {
      return (new NotificationAudience(Type.ROLES, roleNames));
   }



   /*******************************************************************************
    ** Users holding the named permission.
    *******************************************************************************/
   public static NotificationAudience permission(String permissionName)
   {
      return (new NotificationAudience(Type.PERMISSION, permissionName));
   }



   /*******************************************************************************
    ** The named user ids.
    *******************************************************************************/
   public static NotificationAudience users(String... userIds)
   {
      return (new NotificationAudience(Type.USERS, userIds));
   }



   /*******************************************************************************
    ** One user.
    *******************************************************************************/
   public static NotificationAudience user(String userId)
   {
      return (new NotificationAudience(Type.USERS, userId));
   }



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public Type getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Getter for values (unmodifiable; empty for ALL_USERS)
    *******************************************************************************/
   public List<String> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return (true);
      }
      if(!(o instanceof NotificationAudience that))
      {
         return (false);
      }
      return (type == that.type && values.equals(that.values));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return (Objects.hash(type, values));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (type == Type.ALL_USERS ? type.name() : type.name() + values);
   }
}
