/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessMetaDataAdjustment;
import com.kingsrook.qqq.backend.core.model.actions.processes.QUploadedFile;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.AbstractBlockWidgetData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.QJavalinUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessInitOrStepOrStatusOutputInterface;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.ProcessInitOrStepOrStatusResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.WidgetBlock;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessSpecUtilsV1
{
   private static final QLogger LOG = QLogger.getLogger(ProcessSpecUtilsV1.class);

   public static final String TABLE_VARIANT_PARAM = "tableVariant";

   private static final String TABLE_VARIANT_DESCRIPTION = "For processes on tables that use variant backends, JSON object naming the variant to use (the same `type` and `id` as in table requests).";
   private static final String TABLE_VARIANT_EXAMPLE     = """
      {"type":"store","id":"1"}""";

   public static final String EXAMPLE_PROCESS_UUID = "01234567-89AB-CDEF-0123-456789ABCDEF";
   public static final String EXAMPLE_JOB_UUID     = "98765432-10FE-DCBA-9876-543210FEDCBA";



   /*******************************************************************************
    ** The (optional) table variant for a process request:  a JSON object with the
    ** variant's `type` and `id` (as for the table routes), read from the form
    ** field or query parameter named `tableVariant`.
    *******************************************************************************/
   public static TableVariant getTableVariantParam(Context context)
   {
      String tableVariantParam = QJavalinUtils.getFormParamOrQueryParam(context, TABLE_VARIANT_PARAM);
      if(!StringUtils.hasContent(tableVariantParam))
      {
         return (null);
      }

      JSONObject variant = new JSONObject(tableVariantParam);
      return (new TableVariant().withType(variant.optString("type", null)).withId(variant.has("id") ? String.valueOf(variant.get("id")) : null));
   }



   /*******************************************************************************
    ** OpenAPI definition of the (optional) tableVariant query parameter.
    *******************************************************************************/
   public static Parameter defineTableVariantQueryParameter()
   {
      return (new Parameter()
         .withName(TABLE_VARIANT_PARAM)
         .withDescription(TABLE_VARIANT_DESCRIPTION)
         .withRequired(false)
         .withSchema(new Schema().withType(Type.STRING))
         .withExample(TABLE_VARIANT_EXAMPLE)
         .withIn(In.QUERY));
   }



   /*******************************************************************************
    ** OpenAPI definition of the (optional) tableVariant form field.
    *******************************************************************************/
   public static Schema defineTableVariantFormProperty()
   {
      return (new Schema()
         .withType(Type.STRING)
         .withDescription(TABLE_VARIANT_DESCRIPTION + "  May also be given as a query parameter.")
         .withExample(TABLE_VARIANT_EXAMPLE));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static String getResponseSchemaRefName()
   {
      return ("ProcessStepResponseV1");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static Schema buildResponseSchema()
   {
      return new Schema().withOneOf(List.of(
         new Schema().withRef("#/components/schemas/ProcessStepComplete"),
         new Schema().withRef("#/components/schemas/ProcessStepJobStarted"),
         new Schema().withRef("#/components/schemas/ProcessStepRunning"),
         new Schema().withRef("#/components/schemas/ProcessStepError")
      ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static LinkedHashMap<String, Example> buildResponseExample()
   {
      ProcessInitOrStepOrStatusResponseV1 completeResponse = new ProcessInitOrStepOrStatusResponseV1();
      completeResponse.setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      completeResponse.setProcessUUID(EXAMPLE_PROCESS_UUID);
      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put("totalAge", 32768);
      values.put("firstLastName", "Aabramson");
      completeResponse.setValues(values);
      completeResponse.setNextStep("reviewScreen");

      ProcessInitOrStepOrStatusResponseV1 completeResponseWithMetaDataAdjustment = new ProcessInitOrStepOrStatusResponseV1();
      completeResponseWithMetaDataAdjustment.setType(ProcessInitOrStepOrStatusOutputInterface.Type.COMPLETE);
      completeResponseWithMetaDataAdjustment.setProcessUUID(EXAMPLE_PROCESS_UUID);
      completeResponseWithMetaDataAdjustment.setValues(values);
      completeResponseWithMetaDataAdjustment.setNextStep("inputScreen");
      completeResponseWithMetaDataAdjustment.setProcessMetaDataAdjustment(new ProcessMetaDataAdjustment()
         .withUpdatedField(new QFieldMetaData("someField", QFieldType.STRING).withIsRequired(true))
         .withUpdatedFrontendStepList(List.of(
            new QFrontendStepMetaData()
               .withName("inputScreen")
               .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
               .withFormField(new QFieldMetaData("someField", QFieldType.STRING)),
            new QFrontendStepMetaData()
               .withName("resultScreen")
               .withComponent(new QFrontendComponentMetaData().withType(QComponentType.PROCESS_SUMMARY_RESULTS))
         )));

      ProcessInitOrStepOrStatusResponseV1 jobStartedResponse = new ProcessInitOrStepOrStatusResponseV1();
      jobStartedResponse.setType(ProcessInitOrStepOrStatusOutputInterface.Type.JOB_STARTED);
      jobStartedResponse.setProcessUUID(EXAMPLE_PROCESS_UUID);
      jobStartedResponse.setJobUUID(EXAMPLE_JOB_UUID);

      ProcessInitOrStepOrStatusResponseV1 runningResponse = new ProcessInitOrStepOrStatusResponseV1();
      runningResponse.setType(ProcessInitOrStepOrStatusOutputInterface.Type.RUNNING);
      runningResponse.setProcessUUID(EXAMPLE_PROCESS_UUID);
      runningResponse.setMessage("Processing person records");
      runningResponse.setCurrent(47);
      runningResponse.setTotal(1701);

      ProcessInitOrStepOrStatusResponseV1 errorResponse = new ProcessInitOrStepOrStatusResponseV1();
      errorResponse.setType(ProcessInitOrStepOrStatusOutputInterface.Type.RUNNING);
      errorResponse.setProcessUUID(EXAMPLE_PROCESS_UUID);
      errorResponse.setError("Illegal Argument Exception: NaN");
      errorResponse.setUserFacingError("The process could not be completed due to invalid input.");

      Function<ProcessInitOrStepOrStatusResponseV1, Example> responseToExample = response -> new Example().withValue(convertResponseToJSONObject(response).toMap());

      return MapBuilder.of(() -> new LinkedHashMap<String, Example>())
         .with("COMPLETE", responseToExample.apply(completeResponse))
         .with("COMPLETE with metaDataAdjustment", responseToExample.apply(completeResponseWithMetaDataAdjustment))
         .with("JOB_STARTED", responseToExample.apply(jobStartedResponse))
         .with("RUNNING", responseToExample.apply(runningResponse))
         .with("ERROR", responseToExample.apply(errorResponse))
         .build();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static void handleOutput(Context context, ProcessInitOrStepOrStatusResponseV1 response)
   {
      JSONObject outputJsonObject = convertResponseToJSONObject(response);

      String json = outputJsonObject.toString(3);
      context.result(json);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static JSONObject convertResponseToJSONObject(ProcessInitOrStepOrStatusResponseV1 response)
   {
      ////////////////////////////////////////////////////////////////////////////////
      // normally, we like the JsonUtils behavior of excluding null/empty elements. //
      // but it turns out we want those in the values sub-map.                      //
      // so, go through a loop of object → JSON String → JSONObject → String...     //
      // also - work with the TypedResponse sub-object within this response class   //
      ////////////////////////////////////////////////////////////////////////////////
      ProcessInitOrStepOrStatusResponseV1.TypedResponse typedOutput = response.getTypedResponse();

      String     outputJson       = JsonUtils.toJson(typedOutput);
      JSONObject outputJsonObject = new JSONObject(outputJson);

      if(typedOutput instanceof ProcessInitOrStepOrStatusResponseV1.ProcessStepComplete complete)
      {
         ////////////////////////////////////////////////////////////////////////////////////
         // here's where we'll handle the values map specially - note - that we'll also    //
         // be mapping some specific object types into their API-versioned responses types //
         ////////////////////////////////////////////////////////////////////////////////////
         Map<String, Serializable> values = complete.getValues();
         if(values != null)
         {
            JSONObject valuesAsJsonObject = new JSONObject();
            for(Map.Entry<String, Serializable> valueEntry : values.entrySet())
            {
               String       name  = valueEntry.getKey();
               Serializable value = valueEntry.getValue();

               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               // follow the strategy that we use for JsonUtils.nullKeyToEmptyStringSerializer in this rare case... //
               ///////////////////////////////////////////////////////////////////////////////////////////////////////
               if(name == null)
               {
                  name = "";
               }

               Serializable valueToMakeIntoJson = value;
               if(value instanceof String s)
               {
                  valuesAsJsonObject.put(name, s);
                  continue;
               }
               else if(value instanceof Boolean b)
               {
                  valuesAsJsonObject.put(name, b);
                  continue;
               }
               else if(value instanceof Number n)
               {
                  valuesAsJsonObject.put(name, n);
                  continue;
               }
               else if(value == null)
               {
                  valuesAsJsonObject.put(name, JSONObject.NULL);
                  continue;
               }
               //////////////////////////////////////////////////////////////////////////////////
               // if there are any types that we want to make sure we send back using this API //
               // version's mapped objects, then add cases for them here, and wrap them.       //
               //////////////////////////////////////////////////////////////////////////////////
               else if(value instanceof AbstractBlockWidgetData<?, ?, ?, ?> abstractBlockWidgetData)
               {
                  valueToMakeIntoJson = new WidgetBlock(abstractBlockWidgetData);
               }

               ///////////////////////////////////////////////
               // ok now, make the value into a JSON string //
               ///////////////////////////////////////////////
               String valueAsJsonString;
               try
               {
                  valueAsJsonString = JsonUtils.toJsonWithMapper(valueToMakeIntoJson, mapper ->
                  {
                     mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

                     /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     // use this custom serializer to convert null map-keys to empty-strings (rather than having an exception!) //
                     /////////////////////////////////////////////////////////////////////////////////////////////////////////////
                     mapper.getSerializerProvider().setNullKeySerializer(JsonUtils.nullKeyToEmptyStringSerializer);
                  });
               }
               catch(Exception e)
               {
                  LOG.warn("Error deserializing process results with serializationInclusion:ALWAYS - will retry with default settings", e);
                  valueAsJsonString = JsonUtils.toJson(valueToMakeIntoJson);
               }

               /////////////////////////////////////////////////////////////////////////////////////////////////////////
               // THEN - make it back into a JSONObject or JSONArray, and add it to the valuesAsJsonObject JSONObject //
               /////////////////////////////////////////////////////////////////////////////////////////////////////////
               if(valueAsJsonString.startsWith("["))
               {
                  valuesAsJsonObject.put(name, new JSONArray(valueAsJsonString));
               }
               else if(valueAsJsonString.startsWith("{"))
               {
                  valuesAsJsonObject.put(name, new JSONObject(valueAsJsonString));
               }
               else
               {
                  ///////////////////////////////////////////////////////////////////////////////////
                  // curious, if/when this ever happens, since we should get all "primitive" types //
                  // above, and everything else, I think, would be an object or an array, right?   //
                  ///////////////////////////////////////////////////////////////////////////////////
                  valuesAsJsonObject.put(name, valueAsJsonString);
               }
            }

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // ... this might be a concept for us at some point in time - but might be better to not do as a value itself? //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Serializable backendOnlyValues = values.get("_qqqBackendOnlyValues");
            // if(backendOnlyValues instanceof String backendOnlyValuesString)
            // {
            //    for(String key : backendOnlyValuesString.split(","))
            //    {
            //       jsonObject.remove(key);
            //    }
            //    jsonObject.remove("_qqqBackendOnlyValues");
            // }

            outputJsonObject.put("values", valuesAsJsonObject);
         }
      }
      return outputJsonObject;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void archiveUploadedFile(String processName, QUploadedFile qUploadedFile)
   {
      String fileName = QValueFormatter.formatDate(LocalDate.now())
         + File.separator + processName
         + File.separator + qUploadedFile.getFilename();

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(QJavalinImplementation.getJavalinMetaData().getUploadedFileArchiveTableName());
      insertInput.setRecords(List.of(new QRecord()
         .withValue("fileName", fileName)
         .withValue("contents", qUploadedFile.getBytes())
      ));

      new InsertAction().executeAsync(insertInput);
   }



   /***************************************************************************
    ** Read the files uploaded with a process init or step request (QRun-IO/qqq#543)
    ** the same way the legacy process routes do: every value is validated and
    ** every filename checked before anything is stored; each file is streamed to
    ** the uploaded-file archive table, and the field's process value becomes the
    ** list of StorageInputs that reference the stored files.
    ***************************************************************************/
   public static void addUploadedFiles(Context context, ProcessInitOrStepInput input) throws IOException, QException
   {
      Map<String, List<UploadedFile>> uploadedFileMap = context.isMultipartFormData() ? context.uploadedFileMap() : Map.of();

      RunProcessInput validationInput = new RunProcessInput();
      validationInput.setProcessName(input.getProcessName());
      validationInput.setValues(new LinkedHashMap<>(input.getValues()));
      RunProcessAction.validateUserInputValues(validationInput);

      for(List<UploadedFile> uploadedFiles : uploadedFileMap.values())
      {
         for(UploadedFile uploadedFile : uploadedFiles)
         {
            String filename = uploadedFile.filename();
            if(filename.isBlank() || filename.equals(".") || filename.equals("..") || filename.contains("/") || filename.contains("\\")
               || filename.contains(":") || filename.chars().anyMatch(Character::isISOControl))
            {
               throw new QBadRequestException("Uploaded filename must be a nonempty filename without directory separators, colons or control characters.");
            }
         }
      }

      for(Map.Entry<String, List<UploadedFile>> entry : uploadedFileMap.entrySet())
      {
         ArrayList<StorageInput> storageInputs = new ArrayList<>();
         input.getValues().put(entry.getKey(), storageInputs);

         String storageTableName = QJavalinImplementation.getJavalinMetaData() == null ? null : QJavalinImplementation.getJavalinMetaData().getUploadedFileArchiveTableName();
         if(!StringUtils.hasContent(storageTableName))
         {
            throw (new QException("UploadFileArchiveTableName was not specified in javalinMetaData.  Cannot accept file uploads."));
         }

         for(UploadedFile uploadedFile : entry.getValue())
         {
            String reference = QValueFormatter.formatDate(LocalDate.now())
                               + File.separator + input.getProcessName()
                               + File.separator + UUID.randomUUID()
                               + File.separator + uploadedFile.filename();

            StorageInput storageInput = new StorageInput(storageTableName).withReference(reference);
            storageInputs.add(storageInput);

            try(InputStream content = uploadedFile.content(); OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
            {
               content.transferTo(outputStream);
               LOG.info("Streamed uploaded file", logPair("storageTable", storageTableName), logPair("reference", reference), logPair("processName", input.getProcessName()), logPair("uploadFileName", uploadedFile.filename()));
            }
         }
      }
   }

}
