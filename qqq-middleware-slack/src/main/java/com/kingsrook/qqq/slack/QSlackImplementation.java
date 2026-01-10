/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.slack;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.adapters.QInstanceAdapter;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.exceptions.QModuleDispatchException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.LineChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.javalin.QJavalinImplementation;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.files.FilesUploadRequest;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;


/*******************************************************************************
 ** QQQ Slack implementation.  Given a QInstance, defines all routes needed
 ** to respond to Slack http requests and give appropriate responses
 **
 *******************************************************************************/
public class QSlackImplementation
{
   private static final QLogger LOG = QLogger.getLogger(QSlackImplementation.class);

   static QInstance qInstance;



   /*******************************************************************************
    **
    *******************************************************************************/
   public QSlackImplementation(QInstance qInstance) throws QInstanceValidationException
   {
      QSlackImplementation.qInstance = qInstance;
      new QInstanceValidator().validate(qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QSlackImplementation(String qInstanceFilePath) throws IOException
   {
      LOG.info("Loading qInstance from file (assuming json): " + qInstanceFilePath);
      String qInstanceJson = FileUtils.readFileToString(new File(qInstanceFilePath), StandardCharsets.UTF_8);
      QSlackImplementation.qInstance = new QInstanceAdapter().jsonToQInstanceIncludingBackends(qInstanceJson);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public EndpointGroup getRoutes()
   {
      return (() ->
      {
         path("/slack", () ->
         {
            post("/", QSlackImplementation::slack);
         });
      });
   }



   /*******************************************************************************
    ** handle slack command posts
    *******************************************************************************/
   private static void slack(Context context)
   {
      try
      {
         ///////////////////////////////////////////
         // get the input data from slack request //
         ///////////////////////////////////////////
         String              result = context.body();
         List<NameValuePair> params = URLEncodedUtils.parse(new URI("?" + result), StandardCharsets.UTF_8);

         String command = null;
         String text    = null;
         for(NameValuePair nameValuePair : params)
         {
            if(nameValuePair.getName().equalsIgnoreCase("command"))
            {
               command = nameValuePair.getValue();
            }
            else if(nameValuePair.getName().equalsIgnoreCase("text"))
            {
               text = nameValuePair.getValue();
            }
         }

         if(command == null)
         {
            buildSlackMessage(context, "Error", "No command was entered");
         }
         else if(text == null || !StringUtils.hasContent(text))
         {
            buildSlackMetaData(context);
         }
         else
         {
            String[] parts = text.trim().split("\\W");
            if(text.trim().startsWith("table"))
            {
               String tableName = null;
               String action    = null;

               if(parts.length < 2)
               {
                  buildSlackMessage(context, "Error", "A valid table name must be provided");
                  return;
               }
               if(!qInstance.getTables().containsKey(parts[1]))
               {
                  buildSlackMessage(context, "Error", "Table [" + parts[1] + "] could not be found");
                  return;
               }

               tableName = parts[1];
               if(parts.length < 3 || (!parts[2].equalsIgnoreCase("query") && !parts[2].equalsIgnoreCase("get") && !parts[2].equalsIgnoreCase("export")))
               {
                  buildSlackMessage(context, "Error", "A valid table action must be provided: export, get, or query");
                  return;
               }
               action = parts[2];

               if(action.equals("query"))
               {
                  buildQueryResultMessage(context, tableName);
                  return;
               }
               else if(action.equals("export"))
               {
                  if(parts.length < 4)
                  {
                     buildSlackMessage(context, "Error", "A format must be provided for the export action: csv, json, xlsx");
                     return;
                  }

                  ////////////////////////////
                  // make sure valid format //
                  ////////////////////////////
                  ReportFormat format = ReportFormat.fromString(parts[3]);
                  if(format == null)
                  {
                     buildSlackMessage(context, "Error", "A format must be one of the following: csv, json, xlsx");
                     return;

                  }

                  buildExportMessage(context, tableName, format.name());
                  return;
               }
               else if(action.equals("get"))
               {
                  if(parts.length < 4)
                  {
                     buildSlackMessage(context, "Error", "A primary key must be provided for the get action");
                     return;
                  }

                  buildGetResultMessage(context, tableName, parts[3]);
                  return;
               }

               buildSlackMessage(context, "Success", "Load table data");
            }
            if(text.trim().startsWith("widget"))
            {
               String widgetName = null;
               if(parts.length < 2)
               {
                  buildSlackMessage(context, "Error", "A valid widget name name must be provided");
                  return;
               }
               widgetName = parts[1];
               buildWidgetMessage(context, widgetName);
            }
            else
            {
               buildSlackMessage(context, "Error", "Invalid command was entered");
            }
         }
      }
      catch(Exception e)
      {
         handleException(context, e);
      }
   }



   /*******************************************************************************
    ** build up slack message
    *******************************************************************************/
   private static void buildSlackMessage(Context context, String title, String message)
   {
      context.contentType(ContentType.JSON);

      JSONObject response    = new JSONObject();
      JSONArray  blocksArray = new JSONArray();
      blocksArray.put(createSlackBlock(title, message));

      response.put("response_type", "in_channel");
      response.put("blocks", blocksArray);
      context.result(response.toString());
   }



   /*******************************************************************************
    ** handle slack command posts
    *******************************************************************************/
   private static void buildSlackMetaData(Context context) throws QException
   {
      MetaDataInput metaDataInput = new MetaDataInput();
      setupSession(context, metaDataInput);
      MetaDataAction metaDataAction = new MetaDataAction();
      MetaDataOutput metaDataOutput = metaDataAction.execute(metaDataInput);
      context.contentType(ContentType.JSON);

      JSONObject response    = new JSONObject();
      JSONArray  blocksArray = new JSONArray();

      response.put("response_type", "in_channel");
      response.put("blocks", blocksArray);

      ////////////
      // tables //
      ////////////
      StringBuilder tables        = new StringBuilder("\n*Tables*\n");
      List<String>  tableNameList = metaDataOutput.getTables().keySet().stream().sorted().toList();
      for(int i = 0; i < tableNameList.size(); i++)
      {
         tables.append(metaDataOutput.getTables().get(tableNameList.get(i)).getLabel()).append(" [").append(tableNameList.get(i)).append("]\n");
         if(i == 4 && tableNameList.size() > 5)
         {
            tables.append("... and ").append(tableNameList.size() - 5).append(" more\n");
            break;
         }
      }
      blocksArray.put(createSlackBlock("Tables", tables.toString()));

      ///////////////
      // processes //
      ///////////////
      final int     MAX_PROCESSES_TO_DISPLAY = 10;
      StringBuilder processes                = new StringBuilder();
      List<String>  processNameList          = metaDataOutput.getProcesses().keySet().stream().sorted().toList();
      for(int i = 0; i < processNameList.size(); i++)
      {
         processes.append(metaDataOutput.getProcesses().get(processNameList.get(i)).getLabel()).append(" [").append(processNameList.get(i)).append("]\n");
         if(i == MAX_PROCESSES_TO_DISPLAY - 1 && processNameList.size() > MAX_PROCESSES_TO_DISPLAY)
         {
            processes.append("... and ").append(processNameList.size() - MAX_PROCESSES_TO_DISPLAY).append(" more\n");
            break;
         }
      }
      blocksArray.put(createSlackBlock("Processes", processes.toString()));

      /////////////
      // reports //
      /////////////
      StringBuilder reports        = new StringBuilder();
      List<String>  reportNameList = metaDataOutput.getReports().keySet().stream().sorted().toList();
      for(int i = 0; i < reportNameList.size(); i++)
      {
         reports.append(metaDataOutput.getReports().get(reportNameList.get(i)).getLabel()).append(" [").append(reportNameList.get(i)).append("]\n");
         if(i == 9 && reportNameList.size() > 10)
         {
            reports.append("... and ").append(reportNameList.size() - 10).append(" more\n");
            break;
         }
      }
      blocksArray.put(createSlackBlock("Reports", reports.toString()));

      /////////////
      // widgets //
      /////////////
      StringBuilder widgets        = new StringBuilder();
      List<String>  widgetNameList = metaDataOutput.getWidgets().keySet().stream().sorted().toList();
      for(int i = 0; i < widgetNameList.size(); i++)
      {
         //////////////////////////////
         // only allow certain types //
         //////////////////////////////
         String widgetName = widgetNameList.get(i);
         String widgetType = qInstance.getWidget(widgetName).getType();
         if(!widgetType.contains("tatistics") && !widgetType.equals("table") && !widgetType.contains("ineChart"))
         {
            continue;
         }
         if(widgetName.contains("YTDSpend") || widgetName.contains("ShipmentsByWarehouse") || widgetName.contains("Overrides") || widgetName.contains("TNT") || widgetName.contains("ParcelInvoice") || widgetName.contains("BillingWork") || widgetName.contains("AssociatedParcel") || widgetName.contains("FreightStudy"))
         {
            continue;
         }

         widgets.append(metaDataOutput.getWidgets().get(widgetName).getLabel()).append(" [").append(widgetName).append("]\n");
      }
      blocksArray.put(createSlackBlock("Widgets", widgets.toString()));

      context.result(response.toString());
   }



   /*******************************************************************************
    ** handle slack command posts
    *******************************************************************************/
   private static void buildQueryResultMessage(Context context, String tableName) throws QException
   {
      try
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setFilter(new QQueryFilter().withLimit(10));
         queryInput.setTableName(tableName);
         setupSession(context, queryInput);
         QueryOutput output = new QueryAction().execute(queryInput);

         StringBuilder results = new StringBuilder();
         if(CollectionUtils.nullSafeHasContents(output.getRecords()))
         {
            for(QRecord qRecord : output.getRecords())
            {
               results.append(qRecord.toString()).append("\n");
            }
         }
         else
         {
            results.append("No results were found");
         }

         JSONObject response    = new JSONObject();
         JSONArray  blocksArray = new JSONArray();

         blocksArray.put(createSlackBlock("Query on table [" + tableName + "] results", results.toString()));
         response.put("response_type", "in_channel");
         response.put("blocks", blocksArray);
         context.result(response.toString());
      }
      catch(Exception e)
      {
         buildSlackMessage(context, "Error", e.getMessage());
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   private static void buildExportMessage(Context context, String tableName, String format) throws QException
   {
      try(ByteArrayOutputStream baos = new ByteArrayOutputStream())
      {
         ExportInput exportInput = new ExportInput();
         exportInput.setLimit(1000);
         exportInput.setTableName(tableName);

         exportInput.setReportDestination(new ReportDestination()
            .withReportFormat(ReportFormat.valueOf(format))
            .withReportOutputStream(baos));

         setupSession(context, exportInput);
         ExportOutput output = new ExportAction().execute(exportInput);

         JSONObject response    = new JSONObject();
         JSONArray  blocksArray = new JSONArray();
         blocksArray.put(createSlackBlock("Export of table [" + tableName + "] has been generated", ""));
         response.put("response_type", "in_channel");
         response.put("blocks", blocksArray);
         context.result(response.toString());

         ///////////////////////////////////////////////////////////////////////////////////
         // Upload file to Slack - token and channel should come from QInstance config //
         ///////////////////////////////////////////////////////////////////////////////////
         String slackToken   = System.getenv("SLACK_BOT_TOKEN");
         String slackChannel = System.getenv("SLACK_CHANNEL_ID");
         if(slackToken != null && slackChannel != null)
         {
            var client = Slack.getInstance().methods();
            client.filesUpload(FilesUploadRequest.builder()
               .token(slackToken)
               .channels(List.of(slackChannel))
               .filetype(format)
               .filename("export." + format)
               .fileData(baos.toByteArray())
               .build()
            );
         }
      }
      catch(Exception e)
      {
         buildSlackMessage(context, "Error", e.getMessage());
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   private static void buildGetResultMessage(Context context, String tableName, Serializable id) throws QException
   {
      try
      {
         QTableMetaData tableMetaData = qInstance.getTable(tableName);

         GetInput getInput = new GetInput();
         getInput.setPrimaryKey(id);
         getInput.setTableName(tableName);
         setupSession(context, getInput);
         GetOutput output = new GetAction().execute(getInput);

         StringBuilder results = new StringBuilder();
         if(output.getRecord() != null)
         {
            List<String> fieldNameList = tableMetaData.getFields().keySet().stream().sorted().toList();
            for(String fieldName : fieldNameList)
            {
               QFieldMetaData fieldMetaData = tableMetaData.getField(fieldName);
               results.append(fieldMetaData.getLabel()).append(": ").append(output.getRecord().getValue(fieldName)).append("\n");
            }
         }
         else
         {
            results.append("Record was not found");
         }

         JSONObject response    = new JSONObject();
         JSONArray  blocksArray = new JSONArray();

         blocksArray.put(createSlackBlock("Get [" + id + "] on table [" + tableName + "] results", results.toString()));
         response.put("response_type", "in_channel");
         response.put("blocks", blocksArray);
         context.result(response.toString());
      }
      catch(Exception e)
      {
         buildSlackMessage(context, "Error", e.getMessage());
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   private static void buildWidgetMessage(Context context, String widgetName) throws QException
   {
      try
      {
         JSONObject response    = new JSONObject();
         JSONArray  blocksArray = new JSONArray();

         QWidgetMetaDataInterface widgetMetaData = qInstance.getWidget(widgetName);

         RenderWidgetInput input = new RenderWidgetInput()
            .withWidgetMetaData(widgetMetaData);
         setupSession(context, input);
         RenderWidgetOutput output = new RenderWidgetAction().execute(input);

         blocksArray.put(createSlackBlock(null, convertWidgetDataToSlackMessage(widgetMetaData, output)));

         response.put("response_type", "in_channel");
         response.put("blocks", blocksArray);
         context.result(response.toString());
      }
      catch(Exception e)
      {
         buildSlackMessage(context, "Error", e.getMessage());
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   public static String convertWidgetDataToSlackMessage(QWidgetMetaDataInterface widgetMetaData, RenderWidgetOutput widgetOutput) throws QException
   {
      StringBuilder results = new StringBuilder("\n");
      if(widgetOutput.getWidgetData() instanceof StatisticsData data)
      {
         results.append("\n*").append(widgetMetaData.getLabel()).append("*: ").append(QValueFormatter.formatValue(DisplayFormat.COMMAS, data.getCount()));
      }
      else if(widgetOutput.getWidgetData() instanceof LineChartData data)
      {
         LineChartData.Data lineChartData = data.getChartData();
         if(lineChartData != null && CollectionUtils.nullSafeHasContents(lineChartData.getLabels()))
         {
            for(int i = 0; i < lineChartData.getLabels().size(); i++)
            {
               results.append("*").append(lineChartData.getLabels().get(i)).append("*: ");
               for(LineChartData.Data.Dataset dataset : lineChartData.getDatasets())
               {
                  List<String> dataSetDataStrings = new ArrayList<>();
                  for(Number dataValue : dataset.getData())
                  {
                     dataSetDataStrings.add(QValueFormatter.formatValue(DisplayFormat.COMMAS, dataValue));
                  }

                  results.append(dataset.getLabel()).append(" [").append(StringUtils.join(",", dataSetDataStrings)).append("] ");
               }
               results.append("\n");
            }
         }
         else
         {
            results.append("No data was found");
         }
      }
      else if(widgetOutput.getWidgetData() instanceof ChartData data)
      {
         ChartData.Data chartData = data.getChartData();
         if(chartData != null && CollectionUtils.nullSafeHasContents(chartData.getLabels()))
         {
            for(int i = 0; i < chartData.getLabels().size(); i++)
            {
               results.append("*").append(chartData.getLabels().get(i)).append("*: ");
               for(ChartData.Data.Dataset dataset : chartData.getDatasets())
               {
                  results.append(QValueFormatter.formatValue(DisplayFormat.COMMAS, dataset.getData().get(i)));
               }
               results.append("\n");
            }
         }
         else
         {
            results.append("No data was found");
         }
      }
      /*
      else
      {
         results.append("\n*").append(widgetMetaData.getLabel()).append("*: ").append(widgetOutput.getWidgetData());
      }
       */
      else
      {
         results.append("Unsupported widget type");
      }

      return (results.toString());
   }



   /*******************************************************************************
    ** creates a slack block jsonString
    *******************************************************************************/
   private static JSONObject createSlackBlock(String title, String body)
   {
      JSONObject text  = new JSONObject();
      JSONObject block = new JSONObject();
      block.put("type", "section");
      block.put("text", text);
      text.put("type", "mrkdwn");

      StringBuilder output = new StringBuilder();
      if(title != null)
      {
         output.append("\n*").append(title).append("*\n");
      }
      output.append(body);
      text.put("text", output.toString());
      return (block);
   }



   /*******************************************************************************
    ** send a message to Slack
    **
    *******************************************************************************/
   public static void postMessage(String text)
   {
      //////////////////////////////////////////////////////////////
      // you can get this instance via ctx.client() in a Bolt app //
      //////////////////////////////////////////////////////////////
      var    client       = Slack.getInstance().methods();
      String slackToken   = new QMetaDataVariableInterpreter().interpret("${env.SLACK_TOKEN}");
      String slackChannel = new QMetaDataVariableInterpreter().interpret("${env.SLACK_CHANNEL_ID}");

      try
      {
         ///////////////////////////////////////////////////////////////////
         // Call the chat.postMessage method using the built-in WebClient //
         ///////////////////////////////////////////////////////////////////
         var result = client.chatPostMessage(r -> r
            .token(slackToken)
            .channel(slackChannel)
            .mrkdwn(true)
            .text(text)
         );

         //////////////////////////////////////////////////////////////////////////
         // Print result, which includes information about the message (like TS) //
         //////////////////////////////////////////////////////////////////////////
         LOG.info("Slack post result: " + result);
      }
      catch(IOException | SlackApiException e)
      {
         LOG.error("error", e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void setupSession(Context context, AbstractActionInput input) throws QModuleDispatchException, QAuthenticationException
   {
      QJavalinImplementation.setupSession(context, input);
      /*
      QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
      QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(input.getAuthenticationMetaData());

      boolean needToSetSessionIdCookie = false;
      try
      {
         Map<String, String> authenticationContext = new HashMap<>();

         String sessionIdCookieValue     = context.cookie(SESSION_ID_COOKIE_NAME);
         String authorizationHeaderValue = context.header("Authorization");

         if(StringUtils.hasContent(sessionIdCookieValue))
         {
            ////////////////////////////////////////
            // first, look for a sessionId cookie //
            ////////////////////////////////////////
            authenticationContext.put(SESSION_ID_COOKIE_NAME, sessionIdCookieValue);
            needToSetSessionIdCookie = true;
         }
         else if(authorizationHeaderValue != null)
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////
            // second, look for the authorization header:                                                  //
            // either with a "Basic " prefix (for a username:password pair)                                //
            // or with a "Bearer " prefix (for a token that can be handled the same as a sessionId cookie) //
            /////////////////////////////////////////////////////////////////////////////////////////////////
            String basicPrefix  = "Basic ";
            String bearerPrefix = "Bearer ";
            if(authorizationHeaderValue.startsWith(basicPrefix))
            {
               authorizationHeaderValue = authorizationHeaderValue.replaceFirst(basicPrefix, "");
               authenticationContext.put(BASIC_AUTH_NAME, authorizationHeaderValue);
               needToSetSessionIdCookie = true;
            }
            else if(authorizationHeaderValue.startsWith(bearerPrefix))
            {
               authorizationHeaderValue = authorizationHeaderValue.replaceFirst(bearerPrefix, "");
               authenticationContext.put(SESSION_ID_COOKIE_NAME, authorizationHeaderValue);
            }
            else
            {
               LOG.debug("Authorization header value did not have Basic or Bearer prefix. [" + authorizationHeaderValue + "]");
            }
         }
         else
         {
            LOG.debug("Neither [" + SESSION_ID_COOKIE_NAME + "] cookie nor [Authorization] header was present in request.");
         }

         QSession session = authenticationModule.createSession(qInstance, authenticationContext);

         /////////////////////////////////////////////////////////////////////////////////
         // if we got a session id cookie in, then send it back with updated cookie age //
         /////////////////////////////////////////////////////////////////////////////////
         if(needToSetSessionIdCookie)
         {
            context.cookie(SESSION_ID_COOKIE_NAME, session.getIdReference(), SESSION_COOKIE_AGE);
         }

         setUserTimezoneOffsetMinutesHeaderInSession(context, session);
      }
      catch(QAuthenticationException qae)
      {
         ////////////////////////////////////////////////////////////////////////////////
         // if exception caught, clear out the cookie so the frontend will reauthorize //
         ////////////////////////////////////////////////////////////////////////////////
         if(needToSetSessionIdCookie)
         {
            context.removeCookie(SESSION_ID_COOKIE_NAME);
         }

         throw (qae);
      }

       */
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void handleException(Context context, Exception e)
   {
      buildSlackMessage(context, "Error", e.getMessage());
   }
}
