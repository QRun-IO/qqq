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

package com.kingsrook.qqq.esb.publish;


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 * The result of publishing ESB events (EsbPublisher, EsbPublishAction):
 * whether they were sent, how many, and - when they weren't - why.
 *
 * A publish sends all of its events or none of them, so sent is either the
 * number of events or 0.
 *******************************************************************************/
public class EsbPublishOutput extends AbstractActionOutput
{
   private Boolean success;
   private Integer sent;
   private String  error;



   /*******************************************************************************
    ** Getter for success
    *******************************************************************************/
   public Boolean getSuccess()
   {
      return (this.success);
   }



   /*******************************************************************************
    ** Setter for success
    *******************************************************************************/
   public void setSuccess(Boolean success)
   {
      this.success = success;
   }



   /*******************************************************************************
    ** Fluent setter for success
    *******************************************************************************/
   public EsbPublishOutput withSuccess(Boolean success)
   {
      this.success = success;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sent
    *******************************************************************************/
   public Integer getSent()
   {
      return (this.sent);
   }



   /*******************************************************************************
    ** Setter for sent
    *******************************************************************************/
   public void setSent(Integer sent)
   {
      this.sent = sent;
   }



   /*******************************************************************************
    ** Fluent setter for sent
    *******************************************************************************/
   public EsbPublishOutput withSent(Integer sent)
   {
      this.sent = sent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for error - null when the publish succeeded.
    *******************************************************************************/
   public String getError()
   {
      return (this.error);
   }



   /*******************************************************************************
    ** Setter for error
    *******************************************************************************/
   public void setError(String error)
   {
      this.error = error;
   }



   /*******************************************************************************
    ** Fluent setter for error
    *******************************************************************************/
   public EsbPublishOutput withError(String error)
   {
      this.error = error;
      return (this);
   }

}
