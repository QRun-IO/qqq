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

package com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata;


/*******************************************************************************
 ** When the S3 client calculates request checksums and validates response
 ** checksums.  Maps to the AWS SDK v2 requestChecksumCalculation and
 ** responseChecksumValidation settings.
 **
 ** Since AWS SDK for Java v2 2.30.0, the SDK default is WHEN_SUPPORTED, which
 ** sends CRC32 checksums (often as aws-chunked trailing checksums) on puts and
 ** part uploads.  Many S3-compatible stores (MinIO, Garage, R2, B2, Ceph, ...)
 ** reject or mishandle those, so a backend with a custom endpoint defaults to
 ** WHEN_REQUIRED (see S3ClientFactory).
 *******************************************************************************/
public enum S3ChecksumMode
{
   /////////////////////////////////////////////////////////////////////
   // only when the operation requires it (the pre-2.30.0 behavior).  //
   /////////////////////////////////////////////////////////////////////
   WHEN_REQUIRED,

   //////////////////////////////////////////////////////////////////
   // whenever the operation supports it (the SDK 2.30.0+ default). //
   //////////////////////////////////////////////////////////////////
   WHEN_SUPPORTED
}
