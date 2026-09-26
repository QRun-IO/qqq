/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QuickSightChart;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QuickSightChartMetaData;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.quicksight.QuickSightClient;
import software.amazon.awssdk.services.quicksight.model.GenerateEmbedUrlForRegisteredUserRequest;
import software.amazon.awssdk.services.quicksight.model.GenerateEmbedUrlForRegisteredUserResponse;
import software.amazon.awssdk.services.quicksight.model.RegisteredUserDashboardEmbeddingConfiguration;
import software.amazon.awssdk.services.quicksight.model.RegisteredUserEmbeddingExperienceConfiguration;


/*******************************************************************************
 ** Widget implementation for amazon QuickSight charts
 **
 *******************************************************************************/
public class QuickSightChartRenderer extends AbstractWidgetRenderer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      ActionHelper.validateSession(input);

      try
      {
         QuickSightChartMetaData quickSightMetaData = (QuickSightChartMetaData) input.getWidgetMetaData();
         QuickSightClient        quickSightClient   = getQuickSightClient(quickSightMetaData);

         final RegisteredUserEmbeddingExperienceConfiguration experienceConfiguration = RegisteredUserEmbeddingExperienceConfiguration.builder()
            .dashboard(
               RegisteredUserDashboardEmbeddingConfiguration.builder()
                  .initialDashboardId(quickSightMetaData.getDashboardId())
                  .build())
            .build();

         final GenerateEmbedUrlForRegisteredUserRequest generateEmbedUrlForRegisteredUserRequest = GenerateEmbedUrlForRegisteredUserRequest.builder()
            .awsAccountId(quickSightMetaData.getAccountId())
            .userArn(quickSightMetaData.getUserArn())
            .experienceConfiguration(experienceConfiguration)
            .build();

         final GenerateEmbedUrlForRegisteredUserResponse generateEmbedUrlForRegisteredUserResponse = quickSightClient.generateEmbedUrlForRegisteredUser(generateEmbedUrlForRegisteredUserRequest);

         String      embedUrl = generateEmbedUrlForRegisteredUserResponse.embedUrl();
         QWidgetData widget   = new QuickSightChart(input.getWidgetMetaData().getName(), quickSightMetaData.getLabel(), embedUrl);
         return (new RenderWidgetOutput(widget));
      }
      catch(Exception e)
      {
         throw (new QException("Error rendering widget", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QuickSightClient getQuickSightClient(QuickSightChartMetaData metaData)
   {
      AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(metaData.getAccessKey(), metaData.getSecretKey());

      QuickSightClient amazonQuickSightClient = QuickSightClient.builder()
         .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
         .region(Region.of(metaData.getRegion()))
         .build();

      return (amazonQuickSightClient);
   }

}
