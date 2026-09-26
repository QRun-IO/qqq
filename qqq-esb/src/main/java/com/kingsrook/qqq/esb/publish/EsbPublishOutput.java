/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
