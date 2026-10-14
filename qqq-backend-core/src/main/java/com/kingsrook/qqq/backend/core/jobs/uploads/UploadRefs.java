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


package com.kingsrook.qqq.backend.core.jobs.uploads;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsSecurity;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.PERMISSION_ADMIN;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_UPLOAD;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Uploads for jobs and the assistant (spec section 3.2): a file is streamed
 ** straight into the uploads storage table (no temp file) and gets an opaque
 ** ref, bound to the uploading user, that expires (default 7 days; a job that
 ** uses it extends it). The jobs_upload row records the ref, owner, size, and
 ** where the bytes are.
 *******************************************************************************/
public final class UploadRefs
{
   private static final QLogger      LOG    = QLogger.getLogger(UploadRefs.class);
   private static final SecureRandom RANDOM = new SecureRandom();



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private UploadRefs()
   {
   }



   /*******************************************************************************
    ** Stream a file into the uploads storage table as the current user; return
    ** its ref. The stream is read once and never buffered whole.
    *******************************************************************************/
   public static UploadRef store(String fileName, String contentType, InputStream inputStream) throws QException
   {
      DurableJobsMetaData settings = DurableJobs.settings();
      String              userId   = DurableJobsSecurity.userId(QContext.getQSession());
      if(!StringUtils.hasContent(userId))
      {
         throw (new QPermissionDeniedException("Uploading needs a signed-in user."));
      }
      String storageTable = settings.getEffectiveUploadsStorageTableName();
      if(storageTable == null)
      {
         throw (new QUserFacingException("Uploads are not configured in this application (no uploads storage table)."));
      }
      String safeName = sanitize(fileName);
      if(safeName.isEmpty())
      {
         throw (new QBadRequestException("The upload needs a file name."));
      }

      String       reference    = "uploads/" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + UUID.randomUUID() + "/" + safeName;
      StorageInput storageInput = new StorageInput(storageTable).withReference(reference).withContentType(contentType);

      long size = 0;
      try
      {
         size = SystemScope.call(() ->
         {
            long written = 0;
            try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
            {
               byte[] buffer = new byte[64 * 1024];
               int    read;
               while((read = inputStream.read(buffer)) != -1)
               {
                  written += read;
                  if(written > settings.getMaxUploadBytes())
                  {
                     throw (new QBadRequestException("The file is larger than the " + (settings.getMaxUploadBytes() / (1024 * 1024)) + " MB upload limit."));
                  }
                  outputStream.write(buffer, 0, read);
               }
            }
            catch(IOException e)
            {
               throw (new QException("Could not store the upload", e));
            }
            return (written);
         });
      }
      catch(QException e)
      {
         deleteQuietly(storageInput);
         throw (e);
      }

      Instant now       = Instant.now();
      Instant expiresAt = now.plus(settings.getUploadExpiryDays(), ChronoUnit.DAYS);
      String  ref       = newRef();
      long    sizeBytes = size;
      SystemScope.call(() -> new InsertAction().execute(new InsertInput(TABLE_UPLOAD).withRecord(new QRecord()
         .withValue("ref", ref)
         .withValue("userId", userId)
         .withValue("fileName", safeName)
         .withValue("contentType", contentType)
         .withValue("sizeBytes", sizeBytes)
         .withValue("storageTableName", storageTable)
         .withValue("storageReference", reference)
         .withValue("expiresAt", expiresAt)
         .withValue("createDate", now))));

      LOG.info("Stored an upload", logPair("userId", userId), logPair("sizeBytes", sizeBytes), logPair("fileName", safeName));
      return (new UploadRef(ref, safeName, sizeBytes, contentType, expiresAt));
   }



   /*******************************************************************************
    ** The upload row for a ref the current user may use (owner, or jobs_admin),
    ** if it has not expired. Throws QNotFoundException otherwise (never says
    ** whose it was).
    *******************************************************************************/
   public static QRecord get(String ref) throws QException
   {
      if(!StringUtils.hasContent(ref))
      {
         throw (new QNotFoundException("Upload not found."));
      }
      List<QRecord> rows = SystemScope.call(() -> QueryAction.execute(TABLE_UPLOAD, new QQueryFilter(new QFilterCriteria("ref", QCriteriaOperator.EQUALS, ref))));
      if(rows.isEmpty())
      {
         throw (new QNotFoundException("Upload not found."));
      }

      QRecord row    = rows.get(0);
      String  userId = DurableJobsSecurity.userId(QContext.getQSession());
      boolean admin  = QContext.getQSession() != null && QContext.getQSession().hasPermission(PERMISSION_ADMIN);
      if(!admin && (userId == null || !userId.equals(row.getValueString("userId"))))
      {
         throw (new QNotFoundException("Upload not found."));
      }
      Instant expiresAt = row.getValueInstant("expiresAt");
      if(expiresAt != null && expiresAt.isBefore(Instant.now()))
      {
         throw (new QNotFoundException("That upload has expired; upload the file again."));
      }
      return (row);
   }



   /*******************************************************************************
    ** Where an upload's bytes are (checks the ref as get does).
    *******************************************************************************/
   public static StorageInput open(String ref) throws QException
   {
      QRecord row = get(ref);
      return (new StorageInput(row.getValueString("storageTableName")).withReference(row.getValueString("storageReference")));
   }



   /*******************************************************************************
    ** A job uses this upload: keep it at least uploadExpiryDays from now.
    *******************************************************************************/
   public static void extendForJob(String ref, Integer jobId) throws QException
   {
      QRecord             row      = get(ref);
      DurableJobsMetaData settings = DurableJobs.settings();
      Instant             extended = Instant.now().plus(settings.getUploadExpiryDays(), ChronoUnit.DAYS);
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("id", row.getValue("id"));
      values.put("jobId", jobId);
      if(row.getValueInstant("expiresAt") == null || row.getValueInstant("expiresAt").isBefore(extended))
      {
         values.put("expiresAt", extended);
      }
      QRecord update = new QRecord();
      update.setValues(values);
      SystemScope.call(() -> new UpdateAction().execute(new UpdateInput(TABLE_UPLOAD).withRecord(update)));
   }



   /*******************************************************************************
    ** The upload row as an UploadRef.
    *******************************************************************************/
   public static UploadRef toUploadRef(QRecord row)
   {
      return (new UploadRef(row.getValueString("ref"), row.getValueString("fileName"), row.getValueLong("sizeBytes"), row.getValueString("contentType"), row.getValueInstant("expiresAt")));
   }



   /*******************************************************************************
    ** A file name safe to use as the last part of a storage key.
    *******************************************************************************/
   static String sanitize(String fileName)
   {
      if(fileName == null)
      {
         return ("");
      }
      String name = fileName.replace('\\', '/');
      name = name.substring(name.lastIndexOf('/') + 1);
      name = name.replaceAll("[^A-Za-z0-9._ -]", "_").strip();
      return (StringUtils.safeTruncate(name, 200));
   }



   /*******************************************************************************
    ** 32 random bytes, URL-safe.
    *******************************************************************************/
   private static String newRef()
   {
      byte[] bytes = new byte[32];
      RANDOM.nextBytes(bytes);
      return ("up_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void deleteQuietly(StorageInput storageInput)
   {
      try
      {
         SystemScope.call(() ->
         {
            new StorageAction().deleteObject(storageInput);
            return (null);
         });
      }
      catch(Exception e)
      {
         LOG.debug("Could not delete a partial upload", e);
      }
   }

}
