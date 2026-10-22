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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 **
 *******************************************************************************/
public class RecordFeatureSummary implements Serializable
{
   private String tableName;
   private String primaryKey;
   private List<RecordFeature> features = new ArrayList<>();
   private Map<String, Boolean> capabilities = new LinkedHashMap<>();
   private Boolean isFavorite;
   private Boolean isFollowing;
   private Map<String, Integer> counts = new LinkedHashMap<>();
   private String owner;
   private String assignee;
   private List<Map<String, Serializable>> presence;
   private Map<String, Serializable> extensions = new LinkedHashMap<>();



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public RecordFeatureSummary withTableName(String tableName)
   {
      setTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for primaryKey
    *******************************************************************************/
   public String getPrimaryKey()
   {
      return (this.primaryKey);
   }



   /*******************************************************************************
    ** Setter for primaryKey
    *******************************************************************************/
   public void setPrimaryKey(String primaryKey)
   {
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for primaryKey
    *******************************************************************************/
   public RecordFeatureSummary withPrimaryKey(String primaryKey)
   {
      setPrimaryKey(primaryKey);
      return (this);
   }



   /*******************************************************************************
    ** Getter for features
    *******************************************************************************/
   public List<RecordFeature> getFeatures()
   {
      return (this.features);
   }



   /*******************************************************************************
    ** Setter for features
    *******************************************************************************/
   public void setFeatures(List<RecordFeature> features)
   {
      this.features = features;
   }



   /*******************************************************************************
    ** Fluent setter for features
    *******************************************************************************/
   public RecordFeatureSummary withFeatures(List<RecordFeature> features)
   {
      setFeatures(features);
      return (this);
   }



   /*******************************************************************************
    ** Getter for capabilities
    *******************************************************************************/
   public Map<String, Boolean> getCapabilities()
   {
      return (this.capabilities);
   }



   /*******************************************************************************
    ** Setter for capabilities
    *******************************************************************************/
   public void setCapabilities(Map<String, Boolean> capabilities)
   {
      this.capabilities = capabilities;
   }



   /*******************************************************************************
    ** Fluent setter for capabilities
    *******************************************************************************/
   public RecordFeatureSummary withCapabilities(Map<String, Boolean> capabilities)
   {
      setCapabilities(capabilities);
      return (this);
   }



   /*******************************************************************************
    ** Getter for isFavorite
    *******************************************************************************/
   public Boolean getIsFavorite()
   {
      return (this.isFavorite);
   }



   /*******************************************************************************
    ** Setter for isFavorite
    *******************************************************************************/
   public void setIsFavorite(Boolean isFavorite)
   {
      this.isFavorite = isFavorite;
   }



   /*******************************************************************************
    ** Fluent setter for isFavorite
    *******************************************************************************/
   public RecordFeatureSummary withIsFavorite(Boolean isFavorite)
   {
      setIsFavorite(isFavorite);
      return (this);
   }



   /*******************************************************************************
    ** Getter for isFollowing
    *******************************************************************************/
   public Boolean getIsFollowing()
   {
      return (this.isFollowing);
   }



   /*******************************************************************************
    ** Setter for isFollowing
    *******************************************************************************/
   public void setIsFollowing(Boolean isFollowing)
   {
      this.isFollowing = isFollowing;
   }



   /*******************************************************************************
    ** Fluent setter for isFollowing
    *******************************************************************************/
   public RecordFeatureSummary withIsFollowing(Boolean isFollowing)
   {
      setIsFollowing(isFollowing);
      return (this);
   }



   /*******************************************************************************
    ** Getter for counts
    *******************************************************************************/
   public Map<String, Integer> getCounts()
   {
      return (this.counts);
   }



   /*******************************************************************************
    ** Setter for counts
    *******************************************************************************/
   public void setCounts(Map<String, Integer> counts)
   {
      this.counts = counts;
   }



   /*******************************************************************************
    ** Fluent setter for counts
    *******************************************************************************/
   public RecordFeatureSummary withCounts(Map<String, Integer> counts)
   {
      setCounts(counts);
      return (this);
   }



   /*******************************************************************************
    ** Getter for owner
    *******************************************************************************/
   public String getOwner()
   {
      return (this.owner);
   }



   /*******************************************************************************
    ** Setter for owner
    *******************************************************************************/
   public void setOwner(String owner)
   {
      this.owner = owner;
   }



   /*******************************************************************************
    ** Fluent setter for owner
    *******************************************************************************/
   public RecordFeatureSummary withOwner(String owner)
   {
      setOwner(owner);
      return (this);
   }



   /*******************************************************************************
    ** Getter for assignee
    *******************************************************************************/
   public String getAssignee()
   {
      return (this.assignee);
   }



   /*******************************************************************************
    ** Setter for assignee
    *******************************************************************************/
   public void setAssignee(String assignee)
   {
      this.assignee = assignee;
   }



   /*******************************************************************************
    ** Fluent setter for assignee
    *******************************************************************************/
   public RecordFeatureSummary withAssignee(String assignee)
   {
      setAssignee(assignee);
      return (this);
   }



   /*******************************************************************************
    ** Getter for presence
    *******************************************************************************/
   public List<Map<String, Serializable>> getPresence()
   {
      return (this.presence);
   }



   /*******************************************************************************
    ** Setter for presence
    *******************************************************************************/
   public void setPresence(List<Map<String, Serializable>> presence)
   {
      this.presence = presence;
   }



   /*******************************************************************************
    ** Fluent setter for presence
    *******************************************************************************/
   public RecordFeatureSummary withPresence(List<Map<String, Serializable>> presence)
   {
      setPresence(presence);
      return (this);
   }



   /*******************************************************************************
    ** Getter for extensions
    *******************************************************************************/
   public Map<String, Serializable> getExtensions()
   {
      return (this.extensions);
   }



   /*******************************************************************************
    ** Setter for extensions
    *******************************************************************************/
   public void setExtensions(Map<String, Serializable> extensions)
   {
      this.extensions = extensions;
   }



   /*******************************************************************************
    ** Fluent setter for extensions
    *******************************************************************************/
   public RecordFeatureSummary withExtensions(Map<String, Serializable> extensions)
   {
      setExtensions(extensions);
      return (this);
   }

}
