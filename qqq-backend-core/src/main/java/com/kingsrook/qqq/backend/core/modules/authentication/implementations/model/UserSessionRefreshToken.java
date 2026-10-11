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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** QRecord Entity for the UserSessionRefreshToken table - server-side storage
 ** of OAuth2 refresh tokens, one row per userSession (keyed by sessionUuid).
 **
 ** Kept in its own table (rather than as columns on userSession) so that apps
 ** opt in by defining this table (see UserSessionRefreshTokenMetaDataProducer)
 ** - existing userSession schemas need no migration.  Rows from this table must
 ** never be exposed to users: the table is hidden, and the refreshToken field
 ** is hidden (so it is omitted from standard query/get results).
 *******************************************************************************/
public class UserSessionRefreshToken extends QRecordEntity
{
   public static final String TABLE_NAME = "userSessionRefreshToken";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isEditable = false, isHidden = true, maxLength = 40, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String sessionUuid;

   @QField(isEditable = false, isHidden = true)
   private String refreshToken;

   @QField(isEditable = false)
   private Instant refreshTokenExpiresAt;

   @QField(isEditable = false)
   private Instant sessionStartedAt;



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public UserSessionRefreshToken()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public UserSessionRefreshToken(QRecord record)
   {
      populateFromQRecord(record);
   }



   /*******************************************************************************
    ** Getter for id - Primary key
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
   public UserSessionRefreshToken withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate - Create date
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
   public UserSessionRefreshToken withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate - Modify date
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
   public UserSessionRefreshToken withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sessionUuid - UUID of the userSession this refresh token belongs to
    *******************************************************************************/
   public String getSessionUuid()
   {
      return (this.sessionUuid);
   }



   /*******************************************************************************
    ** Setter for sessionUuid
    *******************************************************************************/
   public void setSessionUuid(String sessionUuid)
   {
      this.sessionUuid = sessionUuid;
   }



   /*******************************************************************************
    ** Fluent setter for sessionUuid
    *******************************************************************************/
   public UserSessionRefreshToken withSessionUuid(String sessionUuid)
   {
      this.sessionUuid = sessionUuid;
      return (this);
   }



   /*******************************************************************************
    ** Getter for refreshToken - The refresh token itself - server-side only; never sent to a client or logged
    *******************************************************************************/
   public String getRefreshToken()
   {
      return (this.refreshToken);
   }



   /*******************************************************************************
    ** Setter for refreshToken
    *******************************************************************************/
   public void setRefreshToken(String refreshToken)
   {
      this.refreshToken = refreshToken;
   }



   /*******************************************************************************
    ** Fluent setter for refreshToken
    *******************************************************************************/
   public UserSessionRefreshToken withRefreshToken(String refreshToken)
   {
      this.refreshToken = refreshToken;
      return (this);
   }



   /*******************************************************************************
    ** Getter for refreshTokenExpiresAt - When the refresh token expires, if the provider said (e.g., refresh_expires_in); else null
    *******************************************************************************/
   public Instant getRefreshTokenExpiresAt()
   {
      return (this.refreshTokenExpiresAt);
   }



   /*******************************************************************************
    ** Setter for refreshTokenExpiresAt
    *******************************************************************************/
   public void setRefreshTokenExpiresAt(Instant refreshTokenExpiresAt)
   {
      this.refreshTokenExpiresAt = refreshTokenExpiresAt;
   }



   /*******************************************************************************
    ** Fluent setter for refreshTokenExpiresAt
    *******************************************************************************/
   public UserSessionRefreshToken withRefreshTokenExpiresAt(Instant refreshTokenExpiresAt)
   {
      this.refreshTokenExpiresAt = refreshTokenExpiresAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sessionStartedAt - When the session was first created (the login), for maxSessionLifetimeSeconds
    *******************************************************************************/
   public Instant getSessionStartedAt()
   {
      return (this.sessionStartedAt);
   }



   /*******************************************************************************
    ** Setter for sessionStartedAt
    *******************************************************************************/
   public void setSessionStartedAt(Instant sessionStartedAt)
   {
      this.sessionStartedAt = sessionStartedAt;
   }



   /*******************************************************************************
    ** Fluent setter for sessionStartedAt
    *******************************************************************************/
   public UserSessionRefreshToken withSessionStartedAt(Instant sessionStartedAt)
   {
      this.sessionStartedAt = sessionStartedAt;
      return (this);
   }

}
