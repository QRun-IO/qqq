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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.io.VersionInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.VersionOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.UiApiExtensionInfo;


/*******************************************************************************
 ** Executor for the version endpoint: describes the API's semantic version,
 ** features and registered extensions, so clients can check compatibility
 ** before they sign in.
 *******************************************************************************/
public class VersionExecutor extends AbstractMiddlewareExecutor<VersionInput, VersionOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(VersionInput input, VersionOutputInterface output) throws QException
   {
      AbstractMiddlewareVersion version = input.getMiddlewareVersion();
      String                    semver  = version.getSemanticVersion();

      output.setApiVersion(semver);
      output.setBasePath(("/qqq/" + version.getVersionBasePath()).replaceAll("/+", "/").replaceFirst("/$", ""));
      output.setSupportedMajorVersions(List.of(majorOf(semver)));
      output.setFeatures(version.getFeatures());

      List<UiApiExtensionInfo> extensions = new ArrayList<>();
      for(MiddlewareSpecExtension extension : version.getExtensions().values())
      {
         extensions.add(new UiApiExtensionInfo()
            .withName(extension.getName())
            .withVersion(extension.getSemanticVersion())
            .withPathPrefix(extension.getEffectivePathPrefix())
            .withDescription(extension.getDescription()));
      }
      output.setExtensions(extensions);
      output.setClientChannels(new ArrayList<>(ClientChannelsMetaData.of(QContext.getQInstance()).getChannels().keySet()));
   }



   /***************************************************************************
    ** The MAJOR of a semantic version (1 when it has none, e.g. "v1").
    ***************************************************************************/
   static int majorOf(String semver)
   {
      String major = semver == null ? "" : semver.replaceFirst("\\..*", "").replaceAll("\\D", "");
      return (major.matches("\\d{1,9}") ? Integer.parseInt(major) : 1);
   }
}
