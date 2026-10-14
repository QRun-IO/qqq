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

package com.kingsrook.qqq.backend.core.model.metadata.collaboration;


import java.io.Serializable;


/*******************************************************************************
 ** A record follower and notification preferences, with an optional reason for following.
 *******************************************************************************/
public class RecordFollower implements Serializable
{
   private String userId;
   private Boolean notifyOnChange;
   private Boolean notifyOnComment;
   private String reason;



   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Setter for userId
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public RecordFollower withUserId(String userId)
   {
      setUserId(userId);
      return (this);
   }



   /*******************************************************************************
    ** Getter for notifyOnChange
    *******************************************************************************/
   public Boolean getNotifyOnChange()
   {
      return (this.notifyOnChange);
   }



   /*******************************************************************************
    ** Setter for notifyOnChange
    *******************************************************************************/
   public void setNotifyOnChange(Boolean notifyOnChange)
   {
      this.notifyOnChange = notifyOnChange;
   }



   /*******************************************************************************
    ** Fluent setter for notifyOnChange
    *******************************************************************************/
   public RecordFollower withNotifyOnChange(Boolean notifyOnChange)
   {
      setNotifyOnChange(notifyOnChange);
      return (this);
   }



   /*******************************************************************************
    ** Getter for notifyOnComment
    *******************************************************************************/
   public Boolean getNotifyOnComment()
   {
      return (this.notifyOnComment);
   }



   /*******************************************************************************
    ** Setter for notifyOnComment
    *******************************************************************************/
   public void setNotifyOnComment(Boolean notifyOnComment)
   {
      this.notifyOnComment = notifyOnComment;
   }



   /*******************************************************************************
    ** Fluent setter for notifyOnComment
    *******************************************************************************/
   public RecordFollower withNotifyOnComment(Boolean notifyOnComment)
   {
      setNotifyOnComment(notifyOnComment);
      return (this);
   }



   /*******************************************************************************
    ** Getter for reason
    *******************************************************************************/
   public String getReason()
   {
      return (this.reason);
   }



   /*******************************************************************************
    ** Setter for reason
    *******************************************************************************/
   public void setReason(String reason)
   {
      this.reason = reason;
   }



   /*******************************************************************************
    ** Fluent setter for reason
    *******************************************************************************/
   public RecordFollower withReason(String reason)
   {
      setReason(reason);
      return (this);
   }

}
