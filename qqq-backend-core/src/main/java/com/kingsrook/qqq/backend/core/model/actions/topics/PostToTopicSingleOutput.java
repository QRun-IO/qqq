/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.topics;


/*******************************************************************************
 * Output wrapper for a single message that was attempted to be posted to a topic
 *  as an element of a {@link PostToTopicOutput}.
 *******************************************************************************/
public class PostToTopicSingleOutput
{
   private TopicMessage input;

   private Boolean hadError;
   private String  errorMessage;



   /*******************************************************************************
    ** Constructor that takes the input it is tied to
    **
    *******************************************************************************/
   public PostToTopicSingleOutput(TopicMessage input)
   {
      setInput(input);
   }



   /*******************************************************************************
    * Getter for input
    * @see #withInput(TopicMessage)
    *******************************************************************************/
   public TopicMessage getInput()
   {
      return (this.input);
   }



   /*******************************************************************************
    * Setter for input
    * @see #withInput(TopicMessage)
    *******************************************************************************/
   public void setInput(TopicMessage input)
   {
      this.input = input;
   }



   /*******************************************************************************
    * Fluent setter for input
    *
    * @param input
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public PostToTopicSingleOutput withInput(TopicMessage input)
   {
      this.input = input;
      return (this);
   }



   /*******************************************************************************
    * Getter for hadError
    * @see #withHadError(Boolean)
    *******************************************************************************/
   public Boolean getHadError()
   {
      return (this.hadError);
   }



   /*******************************************************************************
    * Setter for hadError
    * @see #withHadError(Boolean)
    *******************************************************************************/
   public void setHadError(Boolean hadError)
   {
      this.hadError = hadError;
   }



   /*******************************************************************************
    * Fluent setter for hadError
    *
    * @param hadError
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public PostToTopicSingleOutput withHadError(Boolean hadError)
   {
      this.hadError = hadError;
      return (this);
   }



   /*******************************************************************************
    * Getter for errorMessage
    * @see #withErrorMessage(String)
    *******************************************************************************/
   public String getErrorMessage()
   {
      return (this.errorMessage);
   }



   /*******************************************************************************
    * Setter for errorMessage
    * @see #withErrorMessage(String)
    *******************************************************************************/
   public void setErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
   }



   /*******************************************************************************
    * Fluent setter for errorMessage
    *
    * @param errorMessage
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public PostToTopicSingleOutput withErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
      return (this);
   }

}
