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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 * Output wrapper for the action to post to a topic.
 *******************************************************************************/
public class PostToTopicOutput extends AbstractActionOutput
{
   public List<PostToTopicSingleOutput> postToTopicSingleOutputList;



   /***************************************************************************
    *
    ***************************************************************************/
   public static class Builder
   {
      private PostToTopicInput input;

      private Map<TopicMessage, PostToTopicSingleOutput> inputToOutputMap = new HashMap<>();



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public Builder(PostToTopicInput input)
      {
         this.input = input;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      public void add(PostToTopicSingleOutput singleOutput)
      {
         inputToOutputMap.put(singleOutput.getInput(), singleOutput);
      }



      /***************************************************************************
       *
       ***************************************************************************/
      public PostToTopicOutput build()
      {
         PostToTopicOutput output = new PostToTopicOutput();
         for(TopicMessage topicMessage : input.getTopicMessageList())
         {
            PostToTopicSingleOutput postToTopicSingleOutput = inputToOutputMap.get(topicMessage);
            if(postToTopicSingleOutput == null)
            {
               postToTopicSingleOutput = new PostToTopicSingleOutput(topicMessage).withHadError(true).withErrorMessage("Result of topic post could not be found");
            }
            output.withSingleOutput(postToTopicSingleOutput);
         }

         return (output);
      }
   }



   /***************************************************************************
    * fluently add a single output object to this action output.
    ***************************************************************************/
   public PostToTopicOutput withSingleOutput(PostToTopicSingleOutput postToTopicSingleOutput)
   {
      if(this.postToTopicSingleOutputList == null)
      {
         this.postToTopicSingleOutputList = new ArrayList<>();
      }
      this.postToTopicSingleOutputList.add(postToTopicSingleOutput);
      return (this);
   }



   /*******************************************************************************
    * Getter for postToTopicSingleOutputList
    * @see #withPostToTopicSingleOutputList(List)
    *******************************************************************************/
   public List<PostToTopicSingleOutput> getPostToTopicSingleOutputList()
   {
      return (this.postToTopicSingleOutputList);
   }



   /*******************************************************************************
    * Setter for postToTopicSingleOutputList
    * @see #withPostToTopicSingleOutputList(List)
    *******************************************************************************/
   public void setPostToTopicSingleOutputList(List<PostToTopicSingleOutput> postToTopicSingleOutputList)
   {
      this.postToTopicSingleOutputList = postToTopicSingleOutputList;
   }



   /*******************************************************************************
    * Fluent setter for postToTopicSingleOutputList
    *
    * @param postToTopicSingleOutputList
    * list of individual outputs
    * @return this
    *******************************************************************************/
   public PostToTopicOutput withPostToTopicSingleOutputList(List<PostToTopicSingleOutput> postToTopicSingleOutputList)
   {
      this.postToTopicSingleOutputList = postToTopicSingleOutputList;
      return (this);
   }

}
