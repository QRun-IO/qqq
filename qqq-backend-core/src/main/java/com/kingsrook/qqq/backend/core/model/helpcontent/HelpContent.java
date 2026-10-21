/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.helpcontent;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** QRecord Entity for HelpContent table
 *******************************************************************************/
public class HelpContent extends QRecordEntity
{
   public static final String TABLE_NAME = "helpContent";

   @QField(possibleValueSourceName = HelpContentSource.NAME)
   private String source;

   @QField(maxLength = 100)
   private String bundleId;

   @QField(maxLength = 50)
   private String bundleVersion;

   @QField(maxLength = 64)
   private String lastSeededHash;

   @QField(possibleValueSourceName = HelpContentUpstreamStatus.NAME)
   private String upstreamStatus;

   @QField(maxLength = 500)
   private String upstreamSummary;

   @QField()
   private String upstreamContent;

   @QField(maxLength = 100)
   private String modifiedBy;

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String key;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String summary;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String permissionName;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String clientChannel;

   @QField()
   private String content;

   @QField(possibleValueSourceName = HelpContentFormat.NAME)
   private String format;

   @QField(possibleValueSourceName = HelpContentRole.NAME, isRequired = true)
   private String role;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public HelpContent()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public HelpContent(QRecord record)
   {
      populateFromQRecord(record);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public HelpContent withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    ** Setter for createDate
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public HelpContent withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    ** Setter for modifyDate
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public HelpContent withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for key
    *******************************************************************************/
   public String getKey()
   {
      return (this.key);
   }



   /*******************************************************************************
    ** Setter for key
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** Fluent setter for key
    *******************************************************************************/
   public HelpContent withKey(String key)
   {
      this.key = key;
      return (this);
   }



   /*******************************************************************************
    ** Getter for content
    *******************************************************************************/
   public String getContent()
   {
      return (this.content);
   }



   /*******************************************************************************
    ** Setter for content
    *******************************************************************************/
   public void setContent(String content)
   {
      this.content = content;
   }



   /*******************************************************************************
    ** Fluent setter for content
    *******************************************************************************/
   public HelpContent withContent(String content)
   {
      this.content = content;
      return (this);
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   public String getFormat()
   {
      return (this.format);
   }



   /*******************************************************************************
    ** Setter for format
    *******************************************************************************/
   public void setFormat(String format)
   {
      this.format = format;
   }



   /*******************************************************************************
    ** Fluent setter for format
    *******************************************************************************/
   public HelpContent withFormat(String format)
   {
      this.format = format;
      return (this);
   }



   /*******************************************************************************
    ** Getter for role
    *******************************************************************************/
   public String getRole()
   {
      return (this.role);
   }



   /*******************************************************************************
    ** Setter for role
    *******************************************************************************/
   public void setRole(String role)
   {
      this.role = role;
   }



   /*******************************************************************************
    ** Fluent setter for role
    *******************************************************************************/
   public HelpContent withRole(String role)
   {
      this.role = role;
      return (this);
   }




   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public HelpContent withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for permissionName
    *******************************************************************************/
   public String getPermissionName()
   {
      return (this.permissionName);
   }



   /*******************************************************************************
    ** Setter for permissionName
    *******************************************************************************/
   public void setPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
   }



   /*******************************************************************************
    ** Fluent setter for permissionName
    *******************************************************************************/
   public HelpContent withPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clientChannel
    *******************************************************************************/
   public String getClientChannel()
   {
      return (this.clientChannel);
   }



   /*******************************************************************************
    ** Setter for clientChannel
    *******************************************************************************/
   public void setClientChannel(String clientChannel)
   {
      this.clientChannel = clientChannel;
   }



   /*******************************************************************************
    ** Fluent setter for clientChannel
    *******************************************************************************/
   public HelpContent withClientChannel(String clientChannel)
   {
      this.clientChannel = clientChannel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for source
    *******************************************************************************/
   public String getSource()
   {
      return (source);
   }



   /*******************************************************************************
    ** Setter for source
    *******************************************************************************/
   public void setSource(String source)
   {
      this.source = source;
   }



   /*******************************************************************************
    ** Fluent setter for source
    *******************************************************************************/
   public HelpContent withSource(String source)
   {
      this.source = source;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bundleId
    *******************************************************************************/
   public String getBundleId()
   {
      return (bundleId);
   }



   /*******************************************************************************
    ** Setter for bundleId
    *******************************************************************************/
   public void setBundleId(String bundleId)
   {
      this.bundleId = bundleId;
   }



   /*******************************************************************************
    ** Fluent setter for bundleId
    *******************************************************************************/
   public HelpContent withBundleId(String bundleId)
   {
      this.bundleId = bundleId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bundleVersion
    *******************************************************************************/
   public String getBundleVersion()
   {
      return (bundleVersion);
   }



   /*******************************************************************************
    ** Setter for bundleVersion
    *******************************************************************************/
   public void setBundleVersion(String bundleVersion)
   {
      this.bundleVersion = bundleVersion;
   }



   /*******************************************************************************
    ** Fluent setter for bundleVersion
    *******************************************************************************/
   public HelpContent withBundleVersion(String bundleVersion)
   {
      this.bundleVersion = bundleVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastSeededHash
    *******************************************************************************/
   public String getLastSeededHash()
   {
      return (lastSeededHash);
   }



   /*******************************************************************************
    ** Setter for lastSeededHash
    *******************************************************************************/
   public void setLastSeededHash(String lastSeededHash)
   {
      this.lastSeededHash = lastSeededHash;
   }



   /*******************************************************************************
    ** Fluent setter for lastSeededHash
    *******************************************************************************/
   public HelpContent withLastSeededHash(String lastSeededHash)
   {
      this.lastSeededHash = lastSeededHash;
      return (this);
   }



   /*******************************************************************************
    ** Getter for upstreamStatus
    *******************************************************************************/
   public String getUpstreamStatus()
   {
      return (upstreamStatus);
   }



   /*******************************************************************************
    ** Setter for upstreamStatus
    *******************************************************************************/
   public void setUpstreamStatus(String upstreamStatus)
   {
      this.upstreamStatus = upstreamStatus;
   }



   /*******************************************************************************
    ** Fluent setter for upstreamStatus
    *******************************************************************************/
   public HelpContent withUpstreamStatus(String upstreamStatus)
   {
      this.upstreamStatus = upstreamStatus;
      return (this);
   }



   /*******************************************************************************
    ** Getter for upstreamSummary
    *******************************************************************************/
   public String getUpstreamSummary()
   {
      return (upstreamSummary);
   }



   /*******************************************************************************
    ** Setter for upstreamSummary
    *******************************************************************************/
   public void setUpstreamSummary(String upstreamSummary)
   {
      this.upstreamSummary = upstreamSummary;
   }



   /*******************************************************************************
    ** Fluent setter for upstreamSummary
    *******************************************************************************/
   public HelpContent withUpstreamSummary(String upstreamSummary)
   {
      this.upstreamSummary = upstreamSummary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for upstreamContent
    *******************************************************************************/
   public String getUpstreamContent()
   {
      return (upstreamContent);
   }



   /*******************************************************************************
    ** Setter for upstreamContent
    *******************************************************************************/
   public void setUpstreamContent(String upstreamContent)
   {
      this.upstreamContent = upstreamContent;
   }



   /*******************************************************************************
    ** Fluent setter for upstreamContent
    *******************************************************************************/
   public HelpContent withUpstreamContent(String upstreamContent)
   {
      this.upstreamContent = upstreamContent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifiedBy
    *******************************************************************************/
   public String getModifiedBy()
   {
      return (modifiedBy);
   }



   /*******************************************************************************
    ** Setter for modifiedBy
    *******************************************************************************/
   public void setModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
   }



   /*******************************************************************************
    ** Fluent setter for modifiedBy
    *******************************************************************************/
   public HelpContent withModifiedBy(String modifiedBy)
   {
      this.modifiedBy = modifiedBy;
      return (this);
   }
}
