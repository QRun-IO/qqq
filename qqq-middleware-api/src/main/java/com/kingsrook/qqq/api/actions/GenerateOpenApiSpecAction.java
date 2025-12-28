/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.APIVersionRange;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecOutput;
import com.kingsrook.qqq.api.model.actions.GetTableApiFieldsInput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInput;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessInputFieldsContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessOutputInterface;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessUtils;
import com.kingsrook.qqq.api.model.metadata.tables.ApiAssociationMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthResolutionContext;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthenticationResolver;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.openapi.model.Components;
import com.kingsrook.qqq.openapi.model.Contact;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.ExampleWithListValue;
import com.kingsrook.qqq.openapi.model.ExampleWithSingleValue;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Info;
import com.kingsrook.qqq.openapi.model.Method;
import com.kingsrook.qqq.openapi.model.OpenAPI;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Path;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;
import com.kingsrook.qqq.openapi.model.Tag;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import org.apache.commons.lang.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class GenerateOpenApiSpecAction extends AbstractQActionFunction<GenerateOpenApiSpecInput, GenerateOpenApiSpecOutput>
{
   public static final  String  GET_DESCRIPTION         = """
      Get one record from this table, by specifying its primary key as a path parameter.
      """;
   public static final  String  QUERY_DESCRIPTION       = """
      Execute a query on this table, using query criteria as specified in query string parameters.
      
      * Pagination is managed via the `pageNo` & `pageSize` query string parameters.  pageNo starts at 1.  pageSize defaults to 50.
      * By default, the response includes the total count of records that match the query criteria.  The count can be omitted by specifying `includeCount=false`
      * By default, results are sorted by the table's primary key, descending.  This can be changed by specifying the `orderBy` query string parameter, following SQL ORDER BY syntax (e.g., `fieldName1 ASC, fieldName2 DESC`)
      * By default, all given query criteria are combined using logical AND.  This can be changed by specifying the query string parameter `booleanOperator=OR`.
      * Each field on the table can be used as a query criteria.  Each query criteria field can be specified on the query string any number of times.
      * By default, all criteria use the equals operator (e.g., `myField=value` means records will be returned where myField equals value).  Alternative operators can be used as follows:
        * Equals: `myField=value`
        * Not Equals: `myField=!value`
        * Less Than: `myField=&lt;value`
        * Greater Than: `myField=&gt;value`
        * Less Than or Equals: `myField=&lt;=value`
        * Greater Than or Equals: `myField=&gt;=value`
        * Empty (or null): `myField=EMPTY`
        * Not Empty: `myField=!EMPTY`
        * Between: `myField=BETWEEN value1,value2` (two values must be given, separated by commas)
        * Not Between: `myField=!BETWEEN value1,value2` (two values must be given, separated by commas)
        * In: `myField=IN value1,value2,...,valueN` (one or more values must be given, separated by commas)
        * Not In: `myField=!IN value1,value2,...,valueN` (one or more values must be given, separated by commas)
        * Like: `myField=LIKE value` (using standard SQL % and _ wildcards)
        * Not Like: `myField=!LIKE value` (using standard SQL % and _ wildcards)
      """;
   public static final  String  INSERT_DESCRIPTION      = """
      Insert one record into this table by supplying the values to be inserted in the request body.
      * The request body should not include a value for the table's primary key.  Rather, a value will be generated and returned in a successful response's body.
      * Any unrecognized field names in the body will cause a 400 error.
      * Any read-only (non-editable) fields provided in the body will be silently ignored.
      
      Upon success, a status code of 201 (`Created`) is returned, and the generated value for the primary key will be returned in the response body object.
      """;
   public static final  String  UPDATE_DESCRIPTION      = """
      Update one record in this table, by specifying its primary key as a path parameter, and by supplying values to be updated in the request body.
      
      * Only the fields provided in the request body will be updated.
      * To remove a value from a field, supply the key for the field, with a null value.
      * The request body does not need to contain all fields from the table.  Rather, only the fields to be updated should be supplied.
      * Any unrecognized field names in the body will cause a 400 error.
      * Any read-only (non-editable) fields provided in the body will be silently ignored.
      * Note that if the request body includes the primary key, it will be ignored.  Only the primary key value path parameter will be used.
      
      Upon success, a status code of 204 (`No Content`) is returned, with no response body.
      """;
   public static final  String  DELETE_DESCRIPTION      = """
      Delete one record from this table, by specifying its primary key as a path parameter.
      
      Upon success, a status code of 204 (`No Content`) is returned, with no response body.
      """;
   public static final  String  BULK_INSERT_DESCRIPTION = """
      Insert one or more records into this table by supplying array of records with values to be inserted, in the request body.
      * The objects in the request body should not include a value for the table's primary key.  Rather, a value will be generated and returned in a successful response's body
      * Any unrecognized field names in the body will cause a 400 error.
      * Any read-only (non-editable) fields provided in the body will be silently ignored.
      
      An HTTP 207 (`Multi-Status`) code is generally returned, with an array of objects giving the individual sub-status codes for each record in the request body.
      * The 1st record in the request will have its response in the 1st object in the response, and so-forth.
      * For sub-status codes of 201 (`Created`), and the generated value for the primary key will be returned in the response body object.
      """;
   public static final  String  BULK_UPDATE_DESCRIPTION = """
      Update one or more records in this table, by supplying an array of records, with primary keys and values to be updated, in the request body.
      * Only the fields provided in the request body will be updated.
      * To remove a value from a field, supply the key for the field, with a null value.
      * The request body does not need to contain all fields from the table.  Rather, only the fields to be updated should be supplied.
      * Any unrecognized field names in the body will cause a 400 error.
      * Any read-only (non-editable) fields provided in the body will be silently ignored.
      
      An HTTP 207 (`Multi-Status`) code is generally returned, with an array of objects giving the individual sub-status codes for each record in the request body.
      * The 1st record in the request will have its response in the 1st object in the response, and so-forth.
      * Each input object's primary key will also be included in the corresponding response object.
      """;
   public static final  String  BULK_DELETE_DESCRIPTION = """
      Delete one or more records from this table, by supplying an array of primary key values in the request body.
      
      An HTTP 207 (`Multi-Status`) code is generally returned, with an array of objects giving the individual sub-status codes for each record in the request body.
      * The 1st primary key in the request will have its response in the 1st object in the response, and so-forth.
      * Each input primary key will also be included in the corresponding response object.
      """;
   private static final QLogger LOG                     = QLogger.getLogger(GenerateOpenApiSpecAction.class);
   private static final boolean LOG_OMITTED_TABLES      = false;

   private Set<String> neededTableSchemas = new HashSet<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean doesProcessLabelNeedTheWordProcessAppended(String tag)
   {
      return !tag.matches("(?i).* process$");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String getProcessSummary(ApiProcessMetaData apiProcessMetaData, QProcessMetaData processMetaData)
   {
      return ObjectUtils.requireConditionElse(apiProcessMetaData.getSummary(), StringUtils::hasContent, processMetaData.getLabel());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Example> getComponentExamples()
   {
      Map<String, Example> rs = new LinkedHashMap<>();
      rs.put("criteriaNotQueried", new ExampleWithListValue().withSummary("no query on this field").withValue(ListBuilder.of("")));

      rs.put("criteriaNumberEquals", new ExampleWithListValue().withSummary("equal to 47").withValue(ListBuilder.of("47")));
      rs.put("criteriaNumberNotEquals", new ExampleWithListValue().withSummary("not equal to 47").withValue(ListBuilder.of("!47")));
      rs.put("criteriaNumberLessThan", new ExampleWithListValue().withSummary("less than 47").withValue(ListBuilder.of("<47")));
      rs.put("criteriaNumberGreaterThan", new ExampleWithListValue().withSummary("greater than 47").withValue(ListBuilder.of(">47")));
      rs.put("criteriaNumberLessThanOrEquals", new ExampleWithListValue().withSummary("less than or equal to 47").withValue(ListBuilder.of("<=47")));
      rs.put("criteriaNumberGreaterThanOrEquals", new ExampleWithListValue().withSummary("greater than or equal to 47").withValue(ListBuilder.of(">=47")));
      rs.put("criteriaNumberEmpty", new ExampleWithListValue().withSummary("null value").withValue(ListBuilder.of("EMPTY")));
      rs.put("criteriaNumberNotEmpty", new ExampleWithListValue().withSummary("non-null value").withValue(ListBuilder.of("!EMPTY")));
      rs.put("criteriaNumberBetween", new ExampleWithListValue().withSummary("between 42 and 47").withValue(ListBuilder.of("BETWEEN 42,47")));
      rs.put("criteriaNumberNotBetween", new ExampleWithListValue().withSummary("not between 42 and 47").withValue(ListBuilder.of("!BETWEEN 42,47")));
      rs.put("criteriaNumberIn", new ExampleWithListValue().withSummary("any of 1701, 74205, or 74656").withValue(ListBuilder.of("IN 1701,74205,74656")));
      rs.put("criteriaNumberNotIn", new ExampleWithListValue().withSummary("not any of 1701, 74205, or 74656").withValue(ListBuilder.of("!IN 1701,74205,74656")));
      rs.put("criteriaNumberMultiple", new ExampleWithListValue().withSummary("multiple criteria: between 42 and 47 and not equal to 45").withValue(ListBuilder.of("BETWEEN 42,47", "!45")));

      rs.put("criteriaBooleanEquals", new ExampleWithListValue().withSummary("equal to true").withValue(ListBuilder.of("true")));
      rs.put("criteriaBooleanNotEquals", new ExampleWithListValue().withSummary("not equal to true").withValue(ListBuilder.of("!true")));
      rs.put("criteriaBooleanEmpty", new ExampleWithListValue().withSummary("null value").withValue(ListBuilder.of("EMPTY")));
      rs.put("criteriaBooleanNotEmpty", new ExampleWithListValue().withSummary("non-null value").withValue(ListBuilder.of("!EMPTY")));

      String epoch = "2001-01-01T00:00:00Z";
      String now   = Instant.parse(epoch).truncatedTo(ChronoUnit.SECONDS).toString();
      String then  = Instant.parse(epoch).minus(90, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
      String when  = Instant.parse(epoch).plus(90, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
      rs.put("criteriaDateTimeEquals", new ExampleWithListValue().withSummary("equal to " + now).withValue(ListBuilder.of(now)));
      rs.put("criteriaDateTimeNotEquals", new ExampleWithListValue().withSummary("not equal to " + now).withValue(ListBuilder.of("!" + now)));
      rs.put("criteriaDateTimeLessThan", new ExampleWithListValue().withSummary("less than " + now).withValue(ListBuilder.of("<" + now)));
      rs.put("criteriaDateTimeGreaterThan", new ExampleWithListValue().withSummary("greater than " + now).withValue(ListBuilder.of(">" + now)));
      rs.put("criteriaDateTimeLessThanOrEquals", new ExampleWithListValue().withSummary("less than or equal to " + now).withValue(ListBuilder.of("<=" + now)));
      rs.put("criteriaDateTimeGreaterThanOrEquals", new ExampleWithListValue().withSummary("greater than or equal to " + now).withValue(ListBuilder.of(">=" + now)));
      rs.put("criteriaDateTimeEmpty", new ExampleWithListValue().withSummary("null value").withValue(ListBuilder.of("EMPTY")));
      rs.put("criteriaDateTimeNotEmpty", new ExampleWithListValue().withSummary("non-null value").withValue(ListBuilder.of("!EMPTY")));
      rs.put("criteriaDateTimeBetween", new ExampleWithListValue().withSummary("between " + then + " and " + now).withValue(ListBuilder.of("BETWEEN " + then + "," + now)));
      rs.put("criteriaDateTimeNotBetween", new ExampleWithListValue().withSummary("not between " + then + " and " + now).withValue(ListBuilder.of("!BETWEEN " + then + "," + now)));
      rs.put("criteriaDateTimeIn", new ExampleWithListValue().withSummary("any of " + then + ", " + now + ", or " + when).withValue(ListBuilder.of("IN " + then + "," + now + "," + when)));
      rs.put("criteriaDateTimeNotIn", new ExampleWithListValue().withSummary("not any of " + then + ", " + now + ", or " + when).withValue(ListBuilder.of("!IN " + then + "," + now + "," + when)));
      rs.put("criteriaDateTimeMultiple", new ExampleWithListValue().withSummary("multiple criteria: between " + then + " and " + when + " and not equal to " + now).withValue(ListBuilder.of("BETWEEN " + then + "," + when, "!" + now)));

      epoch = "2001-01-01";
      now = LocalDate.parse(epoch).toString();
      then = LocalDate.parse(epoch).minus(90, ChronoUnit.DAYS).toString();
      when = LocalDate.parse(epoch).plus(90, ChronoUnit.DAYS).toString();
      rs.put("criteriaDateEquals", new ExampleWithListValue().withSummary("equal to " + now).withValue(ListBuilder.of(now)));
      rs.put("criteriaDateNotEquals", new ExampleWithListValue().withSummary("not equal to " + now).withValue(ListBuilder.of("!" + now)));
      rs.put("criteriaDateLessThan", new ExampleWithListValue().withSummary("less than " + now).withValue(ListBuilder.of("<" + now)));
      rs.put("criteriaDateGreaterThan", new ExampleWithListValue().withSummary("greater than " + now).withValue(ListBuilder.of(">" + now)));
      rs.put("criteriaDateLessThanOrEquals", new ExampleWithListValue().withSummary("less than or equal to " + now).withValue(ListBuilder.of("<=" + now)));
      rs.put("criteriaDateGreaterThanOrEquals", new ExampleWithListValue().withSummary("greater than or equal to " + now).withValue(ListBuilder.of(">=" + now)));
      rs.put("criteriaDateEmpty", new ExampleWithListValue().withSummary("null value").withValue(ListBuilder.of("EMPTY")));
      rs.put("criteriaDateNotEmpty", new ExampleWithListValue().withSummary("non-null value").withValue(ListBuilder.of("!EMPTY")));
      rs.put("criteriaDateBetween", new ExampleWithListValue().withSummary("between " + then + " and " + now).withValue(ListBuilder.of("BETWEEN " + then + "," + now)));
      rs.put("criteriaDateNotBetween", new ExampleWithListValue().withSummary("not between " + then + " and " + now).withValue(ListBuilder.of("!BETWEEN " + then + "," + now)));
      rs.put("criteriaDateIn", new ExampleWithListValue().withSummary("any of " + then + ", " + now + ", or " + when).withValue(ListBuilder.of("IN " + then + "," + now + "," + when)));
      rs.put("criteriaDateNotIn", new ExampleWithListValue().withSummary("not any of " + then + ", " + now + ", or " + when).withValue(ListBuilder.of("!IN " + then + "," + now + "," + when)));
      rs.put("criteriaDateMultiple", new ExampleWithListValue().withSummary("multiple criteria: between " + then + " and " + when + " and not equal to " + now).withValue(ListBuilder.of("BETWEEN " + then + "," + when, "!" + now)));

      rs.put("criteriaStringEquals", new ExampleWithListValue().withSummary("equal to foo").withValue(ListBuilder.of("foo")));
      rs.put("criteriaStringNotEquals", new ExampleWithListValue().withSummary("not equal to foo").withValue(ListBuilder.of("!foo")));
      rs.put("criteriaStringLessThan", new ExampleWithListValue().withSummary("less than foo").withValue(ListBuilder.of("<foo")));
      rs.put("criteriaStringGreaterThan", new ExampleWithListValue().withSummary("greater than foo").withValue(ListBuilder.of(">foo")));
      rs.put("criteriaStringLessThanOrEquals", new ExampleWithListValue().withSummary("less than or equal to foo").withValue(ListBuilder.of("<=foo")));
      rs.put("criteriaStringGreaterThanOrEquals", new ExampleWithListValue().withSummary("greater than or equal to foo").withValue(ListBuilder.of(">=foo")));
      rs.put("criteriaStringEmpty", new ExampleWithListValue().withSummary("null value").withValue(ListBuilder.of("EMPTY")));
      rs.put("criteriaStringNotEmpty", new ExampleWithListValue().withSummary("non-null value").withValue(ListBuilder.of("!EMPTY")));
      rs.put("criteriaStringBetween", new ExampleWithListValue().withSummary("between bar and foo").withValue(ListBuilder.of("BETWEEN bar,foo")));
      rs.put("criteriaStringNotBetween", new ExampleWithListValue().withSummary("not between bar and foo").withValue(ListBuilder.of("!BETWEEN bar,foo")));
      rs.put("criteriaStringIn", new ExampleWithListValue().withSummary("any of foo, bar, or baz").withValue(ListBuilder.of("IN foo,bar,baz")));
      rs.put("criteriaStringNotIn", new ExampleWithListValue().withSummary("not any of foo, bar, or baz").withValue(ListBuilder.of("!IN foo,bar,baz")));
      rs.put("criteriaStringLike", new ExampleWithListValue().withSummary("starting with f").withValue(ListBuilder.of("LIKE f%")));
      rs.put("criteriaStringNotLike", new ExampleWithListValue().withSummary("not starting with f").withValue(ListBuilder.of("!LIKE f%")));
      rs.put("criteriaStringMultiple", new ExampleWithListValue().withSummary("multiple criteria: between bar and foo and not equal to baz").withValue(ListBuilder.of("BETWEEN bar,foo", "!baz")));

      rs.put("criteriaBlobEmpty", new ExampleWithListValue().withSummary("null value").withValue(ListBuilder.of("EMPTY")));
      rs.put("criteriaBlobNotEmpty", new ExampleWithListValue().withSummary("non-null value").withValue(ListBuilder.of("!EMPTY")));

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Example> getCriteriaExamples(Map<String, Example> componentExamples, QFieldMetaData tableApiField)
   {
      List<String> exampleRefs = new ArrayList<>();
      exampleRefs.add("criteriaNotQueried");

      if(tableApiField.getType().isStringLike())
      {
         componentExamples.keySet().stream().filter(s -> s.startsWith("criteriaString")).forEach(exampleRefs::add);
      }
      else if(tableApiField.getType().isNumeric())
      {
         componentExamples.keySet().stream().filter(s -> s.startsWith("criteriaNumber")).forEach(exampleRefs::add);
      }
      else if(tableApiField.getType().equals(QFieldType.DATE_TIME))
      {
         componentExamples.keySet().stream().filter(s -> s.startsWith("criteriaDateTime")).forEach(exampleRefs::add);
      }
      else if(tableApiField.getType().equals(QFieldType.DATE))
      {
         componentExamples.keySet().stream().filter(s -> s.startsWith("criteriaDate") && !s.startsWith("criteriaDateTime")).forEach(exampleRefs::add);
      }
      else if(tableApiField.getType().equals(QFieldType.BOOLEAN))
      {
         componentExamples.keySet().stream().filter(s -> s.startsWith("criteriaBoolean")).forEach(exampleRefs::add);
      }
      else if(tableApiField.getType().equals(QFieldType.BLOB))
      {
         componentExamples.keySet().stream().filter(s -> s.startsWith("criteriaBlob")).forEach(exampleRefs::add);
      }

      Map<String, Example> rs = new LinkedHashMap<>();

      for(String exampleRef : exampleRefs)
      {
         rs.put(exampleRef, new Example().withRef("#components/examples/" + exampleRef));
      }

      return (rs);
   }



   /*******************************************************************************
    ** Resolve scoped authentication provider and infer/validate security schemes.
    **
    ** <p>This method:
    ** <ol>
    **   <li>Resolves the scoped authentication provider for the API</li>
    **   <li>If explicit security schemes are provided, validates they match the provider type</li>
    **   <li>If no explicit schemes, infers appropriate schemes from the provider type</li>
    ** </ol>
    ** </p>
    **
    ** @param qInstance The QInstance
    ** @param apiInstanceMetaData The API instance metadata
    ** @param securitySchemes The security schemes map to populate/validate
    *******************************************************************************/
   private static void resolveAndValidateSecuritySchemes(QInstance qInstance, ApiInstanceMetaData apiInstanceMetaData, LinkedHashMap<String, SecurityScheme> securitySchemes)
   {
      ///////////////////////////////////////////////////////////////////////////
      // Resolve the scoped authentication provider for this API            //
      ///////////////////////////////////////////////////////////////////////////
      QAuthenticationMetaData authMetaData = null;
      try
      {
         AuthResolutionContext resolutionContext = new AuthResolutionContext()
            .withApiMetaData(apiInstanceMetaData);
         authMetaData = AuthenticationResolver.resolve(qInstance, resolutionContext);
      }
      catch(QException e)
      {
         // If resolver fails, fall back to instance default
         LOG.trace("Could not resolve scoped auth provider, using instance default",
            logPair("apiName", apiInstanceMetaData.getName()));
         authMetaData = qInstance.getAuthentication();
      }

      if(authMetaData == null)
      {
         LOG.warn("No authentication provider found for API",
            logPair("apiName", apiInstanceMetaData.getName()));
         return;
      }

      ///////////////////////////////////////////////////////////////////////////
      // Get explicit security schemes from metadata                          //
      ///////////////////////////////////////////////////////////////////////////
      Map<String, SecurityScheme> explicitSchemes =
         CollectionUtils.nonNullMap(apiInstanceMetaData.getSecuritySchemes());

      ///////////////////////////////////////////////////////////////////////////
      // Infer expected security scheme type from auth provider               //
      ///////////////////////////////////////////////////////////////////////////
      SecuritySchemeType expectedType = inferSecuritySchemeType(authMetaData.getType());

      ///////////////////////////////////////////////////////////////////////////
      // If explicit schemes exist, validate they match provider type         //
      ///////////////////////////////////////////////////////////////////////////
      if(!explicitSchemes.isEmpty())
      {
         for(Map.Entry<String, SecurityScheme> entry : explicitSchemes.entrySet())
         {
            SecurityScheme scheme = entry.getValue();
            if(scheme != null && scheme.getTypeEnum() != null)
            {
               SecuritySchemeType actualType = scheme.getTypeEnum();
               if(expectedType != null && !actualType.equals(expectedType))
               {
                  LOG.warn("Security scheme type mismatch",
                     logPair("apiName", apiInstanceMetaData.getName()),
                     logPair("schemeName", entry.getKey()),
                     logPair("expectedType", expectedType != null ? expectedType.getType() : "null"),
                     logPair("actualType", actualType.getType()),
                     logPair("authProviderType", authMetaData.getType() != null ? authMetaData.getType().getName() : "null"));
               }
            }
         }
         // Use explicit schemes as-is (even if mismatch - let user decide)
         securitySchemes.putAll(explicitSchemes);
      }
      else
      {
         ///////////////////////////////////////////////////////////////////////////
         // No explicit schemes - infer from provider type                       //
         ///////////////////////////////////////////////////////////////////////////
         if(expectedType != null)
         {
            SecurityScheme inferredScheme = createInferredSecurityScheme(expectedType, authMetaData);
            if(inferredScheme != null)
            {
               String schemeName = getDefaultSchemeName(expectedType, authMetaData);
               securitySchemes.put(schemeName, inferredScheme);
               LOG.debug("Inferred security scheme from authentication provider",
                  logPair("apiName", apiInstanceMetaData.getName()),
                  logPair("schemeName", schemeName),
                  logPair("schemeType", expectedType.getType()));
            }
         }
      }
   }



   /*******************************************************************************
    ** Infer SecuritySchemeType from QAuthenticationType.
    **
    ** @param authType The authentication type
    ** @return The corresponding security scheme type, or null if no mapping
    *******************************************************************************/
   private static SecuritySchemeType inferSecuritySchemeType(QAuthenticationType authType)
   {
      if(authType == null)
      {
         return null;
      }

      return switch(authType)
      {
         case OAUTH2, AUTH_0 -> SecuritySchemeType.OAUTH2;
         case TABLE_BASED -> SecuritySchemeType.API_KEY; // Table-based auth typically uses API keys
         case FULLY_ANONYMOUS, MOCK -> null; // No security scheme for anonymous/mock
      };
   }



   /*******************************************************************************
    ** Create an inferred security scheme based on the type and auth metadata.
    **
    ** @param schemeType The security scheme type
    ** @param authMetaData The authentication metadata
    ** @return A new SecurityScheme instance, or null if cannot be inferred
    *******************************************************************************/
   private static SecurityScheme createInferredSecurityScheme(SecuritySchemeType schemeType, QAuthenticationMetaData authMetaData)
   {
      SecurityScheme scheme = new SecurityScheme().withType(schemeType);

      switch(schemeType)
      {
         case API_KEY ->
         {
            // Default API key configuration
            scheme.withName("x-api-key").withIn("header");
         }
         case OAUTH2 ->
         {
            // OAuth2 scheme - basic configuration
            // Note: Full OAuth2 flow configuration would require more details
            // This is a minimal inferred scheme
            scheme.withName("OAuth2");
         }
         case HTTP ->
         {
            // HTTP scheme (Basic/Bearer)
            scheme.withScheme("bearer").withBearerFormat("JWT");
         }
         default ->
         {
            return null;
         }
      }

      return scheme;
   }



   /*******************************************************************************
    ** Get default scheme name based on type and auth metadata.
    **
    ** @param schemeType The security scheme type
    ** @param authMetaData The authentication metadata
    ** @return A default scheme name
    *******************************************************************************/
   private static String getDefaultSchemeName(SecuritySchemeType schemeType, QAuthenticationMetaData authMetaData)
   {
      return switch(schemeType)
      {
         case API_KEY -> "apiKey";
         case OAUTH2 -> "OAuth2";
         case HTTP -> "bearerAuth";
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<Map<String, List<String>>> getSecurity(ApiInstanceMetaData apiInstanceMetaData, String permissionName)
   {
      List<Map<String, List<String>>> rs = new ArrayList<>();
      for(Map.Entry<String, SecurityScheme> entry : CollectionUtils.nonNullMap(apiInstanceMetaData.getSecuritySchemes()).entrySet())
      {
         rs.add(MapBuilder.of(entry.getKey(), List.of(permissionName)));
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Type getFieldType(QFieldMetaData field)
   {
      return (getFieldType(field.getType()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Type getFieldType(QFieldType type)
   {
      return switch(type)
      {
         case STRING, DATE, TIME, DATE_TIME, TEXT, HTML, PASSWORD, BLOB -> Type.STRING;
         case INTEGER, LONG -> Type.INTEGER; // todo - we could give 'format' w/ int32 & int64 to further specify
         case DECIMAL -> Type.NUMBER;
         case BOOLEAN -> Type.BOOLEAN;
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<Integer, Response> buildStandardErrorResponses(ApiInstanceMetaData apiInstanceMetaData)
   {
      Map<Integer, Response> rs = MapBuilder.of(
         HttpStatus.BAD_REQUEST.getCode(), new Response().withRef("#/components/responses/error" + HttpStatus.BAD_REQUEST.getCode()),
         HttpStatus.UNAUTHORIZED.getCode(), new Response().withRef("#/components/responses/error" + HttpStatus.UNAUTHORIZED.getCode()),
         HttpStatus.FORBIDDEN.getCode(), new Response().withRef("#/components/responses/error" + HttpStatus.FORBIDDEN.getCode()),
         HttpStatus.INTERNAL_SERVER_ERROR.getCode(), new Response().withRef("#/components/responses/error" + HttpStatus.INTERNAL_SERVER_ERROR.getCode())
      );

      if(apiInstanceMetaData.getIncludeErrorTooManyRequests())
      {
         rs.put(HttpStatus.TOO_MANY_REQUESTS.getCode(), new Response().withRef("#/components/responses/error" + HttpStatus.TOO_MANY_REQUESTS.getCode()));
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Response buildStandardErrorResponse(String description)
   {
      return buildStandardErrorResponse(description, description);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Response buildStandardErrorResponse(String description, String example)
   {
      return new Response()
         .withDescription(description)
         .withContent(MapBuilder.of("application/json", new Content()
            .withSchema(new Schema()
               .withType(Type.OBJECT)
               .withProperties(MapBuilder.of("error", new Schema()
                  .withType(Type.STRING)
                  .withExample(example)
               ))
            )
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public GenerateOpenApiSpecOutput execute(GenerateOpenApiSpecInput input) throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      String    version   = input.getVersion();

      ApiInstanceMetaDataContainer apiInstanceMetaDataContainer = ApiInstanceMetaDataContainer.of(qInstance);
      if(apiInstanceMetaDataContainer == null)
      {
         throw new QException("No ApiInstanceMetaDataContainer exists in this instance");
      }

      if(!StringUtils.hasContent(input.getApiName()))
      {
         throw new QException("Missing required input: apiName");
      }

      ApiInstanceMetaData apiInstanceMetaData = apiInstanceMetaDataContainer.getApiInstanceMetaData(input.getApiName());
      if(apiInstanceMetaData == null)
      {
         throw new QException("Could not find apiInstanceMetaData named [" + input.getApiName() + "] in this instance");
      }

      if(!StringUtils.hasContent(input.getVersion()))
      {
         throw new QException("Missing required input: version");
      }

      APIVersion apiVersion = new APIVersion(version);
      if(!apiInstanceMetaData.getSupportedVersions().contains(apiVersion))
      {
         throw (new QException("[" + version + "] is not a supported API Version."));
      }

      String basePath = apiInstanceMetaData.getPath() + version + "/";
      String apiName  = apiInstanceMetaData.getName();

      OpenAPI openAPI = new OpenAPI()
         .withVersion("3.0.3")
         .withInfo(new Info()
            .withTitle(apiInstanceMetaData.getLabel())
            .withDescription(apiInstanceMetaData.getDescription())
            .withContact(new Contact()
               .withEmail(apiInstanceMetaData.getContactEmail()))
            .withVersion(version));

      openAPI.withServers(apiInstanceMetaData.getServers());

      openAPI.setTags(new ArrayList<>());
      openAPI.setPaths(new LinkedHashMap<>());

      LinkedHashMap<String, Response>       componentResponses = new LinkedHashMap<>();
      LinkedHashMap<String, Schema>         componentSchemas   = new LinkedHashMap<>();
      LinkedHashMap<String, SecurityScheme> securitySchemes    = new LinkedHashMap<>();
      openAPI.setComponents(new Components()
         .withSchemas(componentSchemas)
         .withResponses(componentResponses)
         .withSecuritySchemes(securitySchemes)
         .withExamples(getComponentExamples()));

      ///////////////////////////////////////////////////////////////////////////
      // Resolve scoped authentication provider and infer/validate schemes   //
      ///////////////////////////////////////////////////////////////////////////
      resolveAndValidateSecuritySchemes(qInstance, apiInstanceMetaData, securitySchemes);

      @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
      LinkedHashMap<String, String> scopes = new LinkedHashMap<>();

      /*
      ////////////////////////////////////////////////////////////////////////////////
      // these are moved to the app to define now, but, leaving here for references //
      ////////////////////////////////////////////////////////////////////////////////
      securitySchemes.put("basicAuth", new SecurityScheme()
         .withType("http")
         .withScheme("basic"));

      securitySchemes.put("bearerAuth", new SecurityScheme()
         .withType("http")
         .withScheme("bearer")
         .withBearerFormat("JWT"));

      securitySchemes.put("OAuth2", new OAuth2()
         .withFlows(MapBuilder.of("clientCredentials", new OAuth2Flow()
            .withTokenUrl("/api/oauth/token"))));

      // todo, or not todo? .withScopes(scopes)
      // seems to make a lot of "noise" on the Auth page, and for no obvious benefit...
       */

      componentSchemas.put("baseSearchResultFields", new Schema()
         .withType(Type.OBJECT)
         .withProperties(MapBuilder.of(
            "count", new Schema()
               .withType(Type.INTEGER)
               .withDescription("Number of records that matched the search criteria"),
            "pageNo", new Schema()
               .withType(Type.INTEGER)
               .withDescription("Requested result page number"),
            "pageSize", new Schema()
               .withType(Type.INTEGER)
               .withDescription("Requested result page size")
         )));

      List<Tag>   tagList          = new ArrayList<>();
      Set<String> usedProcessNames = new HashSet<>();

      /////////////////////////////////////
      // foreach table (sorted by label) //
      /////////////////////////////////////
      List<QTableMetaData> tables = new ArrayList<>(qInstance.getTables().values());
      tables.sort(Comparator.comparing(t -> ObjectUtils.requireNonNullElse(t.getLabel(), t.getName(), "")));
      for(QTableMetaData table : tables)
      {
         String tableName = table.getName();

         if(input.getTableName() != null && !input.getTableName().equals(tableName))
         {
            logOmittedTable("Omitting table [" + tableName + "] because it is not the requested table [" + input.getTableName() + "]");
            continue;
         }

         if(table.getIsHidden())
         {
            logOmittedTable("Omitting table [" + tableName + "] because it is marked as hidden");
            continue;
         }

         ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
         if(apiTableMetaDataContainer == null)
         {
            logOmittedTable("Omitting table [" + tableName + "] because it does not have an apiTableMetaDataContainer");
            continue;
         }

         ApiTableMetaData apiTableMetaData = apiTableMetaDataContainer.getApiTableMetaData(apiName);
         if(apiTableMetaData == null)
         {
            logOmittedTable("Omitting table [" + tableName + "] because it does not have any apiTableMetaData");
            continue;
         }

         if(BooleanUtils.isTrue(apiTableMetaData.getIsExcluded()))
         {
            logOmittedTable("Omitting table [" + tableName + "] because its apiTableMetaData marks it as excluded");
            continue;
         }

         APIVersionRange apiVersionRange = apiTableMetaData.getApiVersionRange();
         if(!apiVersionRange.includes(apiVersion))
         {
            logOmittedTable("Omitting table [" + tableName + "] because its api version range [" + apiVersionRange + "] does not include this version [" + version + "]");
            continue;
         }

         QBackendMetaData tableBackend     = qInstance.getBackendForTable(tableName);
         boolean          queryCapability  = table.isCapabilityEnabled(tableBackend, Capability.TABLE_QUERY);
         boolean          getCapability    = table.isCapabilityEnabled(tableBackend, Capability.TABLE_GET);
         boolean          updateCapability = table.isCapabilityEnabled(tableBackend, Capability.TABLE_UPDATE);
         boolean          deleteCapability = table.isCapabilityEnabled(tableBackend, Capability.TABLE_DELETE);
         boolean          insertCapability = table.isCapabilityEnabled(tableBackend, Capability.TABLE_INSERT);
         boolean          countCapability  = table.isCapabilityEnabled(tableBackend, Capability.TABLE_COUNT); // todo - look at this - if table doesn't have count, don't include it in its input/output, etc

         List<ApiOperation.EnabledOperationsProvider> operationProviders = List.of(apiInstanceMetaData, apiTableMetaData);

         boolean getEnabled                = ApiOperation.GET.isOperationEnabled(operationProviders) && getCapability;
         boolean queryByQueryStringEnabled = ApiOperation.QUERY_BY_QUERY_STRING.isOperationEnabled(operationProviders) && queryCapability;
         boolean insertEnabled             = ApiOperation.UPDATE.isOperationEnabled(operationProviders) && insertCapability;
         boolean insertBulkEnabled         = ApiOperation.BULK_INSERT.isOperationEnabled(operationProviders) && insertCapability;
         boolean updateEnabled             = ApiOperation.INSERT.isOperationEnabled(operationProviders) && updateCapability;
         boolean updateBulkEnabled         = ApiOperation.BULK_UPDATE.isOperationEnabled(operationProviders) && updateCapability;
         boolean deleteEnabled             = ApiOperation.DELETE.isOperationEnabled(operationProviders) && deleteCapability;
         boolean deleteBulkEnabled         = ApiOperation.BULK_DELETE.isOperationEnabled(operationProviders) && deleteCapability;

         List<Pair<ApiProcessMetaData, QProcessMetaData>> apiProcessMetaDataList = getProcessesUnderTable(table, apiName, apiVersion);

         if(!getEnabled && !queryByQueryStringEnabled && !insertEnabled && !insertBulkEnabled && !updateEnabled && !updateBulkEnabled && !deleteEnabled && !deleteBulkEnabled && !CollectionUtils.nullSafeHasContents(apiProcessMetaDataList))
         {
            logOmittedTable("Omitting table [" + tableName + "] because it does not have any supported capabilities / enabled operations or processes");
            continue;
         }

         if(!StringUtils.hasContent(table.getPrimaryKeyField()))
         {
            throw (new QException("Unable to generate OpenAPI spec for table " + tableName + ", because it does not have a primary key."));
         }

         String               tableApiName        = StringUtils.hasContent(apiTableMetaData.getApiTableName()) ? apiTableMetaData.getApiTableName() : tableName;
         String               tableApiNameUcFirst = StringUtils.ucFirst(tableApiName);
         String               tableLabel          = table.getLabel();
         QFieldMetaData       primaryKeyField     = table.getField(table.getPrimaryKeyField());
         String               primaryKeyLabel     = primaryKeyField.getLabel();
         String               primaryKeyApiName   = ApiFieldMetaData.getEffectiveApiFieldName(apiName, primaryKeyField);
         List<QFieldMetaData> tableApiFields      = new GetTableApiFieldsAction().execute(new GetTableApiFieldsInput().withTableName(tableName).withVersion(version).withApiName(apiName)).getFields();

         tagList.add(new Tag()
            .withName(tableLabel)
            .withDescription("Operations on the " + tableLabel + " table."));

         ///////////////////////////////
         // permissions for the table //
         ///////////////////////////////
         String tableReadPermissionName = PermissionsHelper.getTablePermissionName(tableName, TablePermissionSubType.READ);
         if(StringUtils.hasContent(tableReadPermissionName))
         {
            scopes.put(tableReadPermissionName, "Permission to read the " + tableLabel + " table");
         }

         String tableUpdatePermissionName = PermissionsHelper.getTablePermissionName(tableName, TablePermissionSubType.EDIT);
         if(StringUtils.hasContent(tableUpdatePermissionName))
         {
            scopes.put(tableUpdatePermissionName, "Permission to update records in the " + tableLabel + " table");
         }

         String tableInsertPermissionName = PermissionsHelper.getTablePermissionName(tableName, TablePermissionSubType.INSERT);
         if(StringUtils.hasContent(tableInsertPermissionName))
         {
            scopes.put(tableInsertPermissionName, "Permission to insert records in the " + tableLabel + " table");
         }

         String tableDeletePermissionName = PermissionsHelper.getTablePermissionName(tableName, TablePermissionSubType.DELETE);
         if(StringUtils.hasContent(tableDeletePermissionName))
         {
            scopes.put(tableDeletePermissionName, "Permission to delete records in the " + tableLabel + " table");
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////
         // todo - handle non read/edit/insert/delete tables (e.g., w/ just 1 permission, or read/write) //
         //////////////////////////////////////////////////////////////////////////////////////////////////

         //////////////////////////////////////
         // build the schemas for this table //
         //////////////////////////////////////
         Schema tableSchema = buildTableSchema(apiInstanceMetaData, version, table, tableApiFields);
         componentSchemas.put(tableApiName, tableSchema);

         //////////////////////////////////////////////////////////////////////////////
         // table as a search result (the base search result, plus the table itself) //
         //////////////////////////////////////////////////////////////////////////////
         if(queryByQueryStringEnabled)
         {
            componentSchemas.put(tableApiName + "SearchResult", new Schema()
               .withType(Type.OBJECT)
               .withAllOf(ListBuilder.of(new Schema().withRef("#/components/schemas/baseSearchResultFields")))
               .withProperties(MapBuilder.of(
                  "records", new Schema()
                     .withType(Type.ARRAY)
                     .withItems(new Schema()
                        .withAllOf(ListBuilder.of(
                           new Schema().withRef("#/components/schemas/" + tableApiName)))))));
         }

         // todo...?
         // includeAssociatedOrderLines=false&includeAssociatedExtrinsics=false&includeAssociatedOrderLinesExtrinsics
         // includeAssociatedRecords=none
         // includeAssociatedRecords=all
         // includeAssociatedRecords=orderLines
         // includeAssociatedRecords=orderLines,orderLines.extrinsics
         // includeAssociatedRecords=extrinsics,orderLines,orderLines.extrinsics

         //////////////////////////////////////
         // paths and methods for this table //
         //////////////////////////////////////
         Method queryGet = new Method()
            .withSummary("Search for " + tableLabel + " records by query string")
            .withDescription(QUERY_DESCRIPTION)
            .withOperationId("query" + tableApiNameUcFirst)
            .withTags(ListBuilder.of(tableLabel))
            .withParameters(ListBuilder.of(
               new Parameter()
                  .withName("pageNo")
                  .withDescription("Which page of results to return.  Starts at 1.")
                  .withIn(In.QUERY)
                  .withSchema(new Schema().withType(Type.INTEGER)),
               new Parameter()
                  .withName("pageSize")
                  .withDescription("Max number of records to include in a page.  Defaults to 50.  Must be between 1 and 1000.")
                  .withIn(In.QUERY)
                  .withSchema(new Schema().withType(Type.INTEGER)),
               new Parameter()
                  .withName("includeCount")
                  .withDescription("Whether or not to include the count (total matching records) in the result. Default is true.")
                  .withIn(In.QUERY)
                  .withSchema(new Schema().withType(Type.BOOLEAN).withEnumValues(ListBuilder.of("true", "false"))),
               new Parameter()
                  .withName("orderBy")
                  .withDescription("How the results of the query should be sorted. SQL-style, comma-separated list of field names, each optionally followed by ASC or DESC (defaults to ASC).")
                  .withIn(In.QUERY)
                  .withSchema(new Schema().withType(Type.STRING))
                  .withExamples(buildOrderByExamples(apiName, primaryKeyApiName, tableApiFields)),
               new Parameter()
                  .withName("booleanOperator")
                  .withDescription("Whether to combine query field as an AND or an OR.  Default is AND.")
                  .withIn(In.QUERY)
                  .withSchema(new Schema().withType(Type.STRING).withEnumValues(ListBuilder.of("AND", "OR")))))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.OK.getCode(), new Response()
               .withDescription("Successfully searched the " + tableLabel + " table (though may have found 0 records).")
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema().withRef("#/components/schemas/" + tableApiName + "SearchResult")))))
            .withSecurity(getSecurity(apiInstanceMetaData, tableReadPermissionName));

         for(QFieldMetaData tableApiField : tableApiFields)
         {
            String fieldName = ApiFieldMetaData.getEffectiveApiFieldName(apiName, tableApiField);
            String label     = tableApiField.getLabel();

            if(!StringUtils.hasContent(label))
            {
               label = QInstanceEnricher.nameToLabel(fieldName);
            }

            StringBuilder description = new StringBuilder("Query on the " + label + " field.  ");
            if(tableApiField.getType().equals(QFieldType.BLOB))
            {
               description.append("Can only query for EMPTY or !EMPTY.");
            }
            else
            {
               description.append("Can prefix value with an operator, else defaults to = (equals).");
            }

            queryGet.getParameters().add(new Parameter()
               .withName(fieldName)
               .withDescription(description.toString())
               .withIn(In.QUERY)
               .withExplode(true)
               .withSchema(new Schema()
                  .withType(Type.ARRAY)
                  .withItems(new Schema().withType(Type.STRING)))
               .withExamples(getCriteriaExamples(openAPI.getComponents().getExamples(), tableApiField)));
         }

         Method queryPost = new Method()
            .withSummary("Search the " + tableLabel + " table by posting a QueryFilter object.")
            .withTags(ListBuilder.of(tableLabel))
            .withSecurity(getSecurity(apiInstanceMetaData, tableReadPermissionName));

         if(queryByQueryStringEnabled)
         {
            openAPI.getPaths().put(basePath + tableApiName + "/query", new Path()
               // todo!! .withPost(queryPost)
               .withGet(queryGet)
            );
         }

         Method idGet = new Method()
            .withSummary("Get one " + tableLabel + " by " + primaryKeyLabel)
            .withDescription(GET_DESCRIPTION)
            .withOperationId("get" + tableApiNameUcFirst)
            .withTags(ListBuilder.of(tableLabel))
            .withParameters(ListBuilder.of(
               new Parameter()
                  .withName(primaryKeyApiName)
                  .withDescription(primaryKeyLabel + " of the " + tableLabel + " to get.")
                  .withIn(In.PATH)
                  .withRequired(true)
                  .withSchema(new Schema().withType(getFieldType(primaryKeyField)))))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.NOT_FOUND.getCode(), buildStandardErrorResponse("The requested " + tableLabel + " record was not found.", "Could not find " + tableLabel + " with " + primaryKeyLabel + " of 47."))
            .withResponse(HttpStatus.OK.getCode(), new Response()
               .withDescription("Successfully got the requested " + tableLabel)
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema().withRef("#/components/schemas/" + tableApiName)))))
            .withSecurity(getSecurity(apiInstanceMetaData, tableReadPermissionName));

         Method idPatch = new Method()
            .withSummary("Update one " + tableLabel)
            .withDescription(UPDATE_DESCRIPTION)
            .withOperationId("update" + tableApiNameUcFirst)
            .withTags(ListBuilder.of(tableLabel))
            .withParameters(ListBuilder.of(
               new Parameter()
                  .withName(primaryKeyApiName)
                  .withDescription(primaryKeyLabel + " of the " + tableLabel + " to update.")
                  .withIn(In.PATH)
                  .withRequired(true)
                  .withSchema(new Schema().withType(getFieldType(primaryKeyField)))))
            .withRequestBody(new RequestBody()
               .withRequired(true)
               .withDescription("Field values to update in the " + tableLabel + " record.")
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema().withRef("#/components/schemas/" + tableApiName)))))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.NOT_FOUND.getCode(), buildStandardErrorResponse("The requested " + tableLabel + " record was not found.", "Could not find " + tableLabel + " with " + primaryKeyLabel + " of 47."))
            .withResponse(HttpStatus.NO_CONTENT.getCode(), new Response().withDescription("Successfully updated the requested " + tableLabel))
            .withSecurity(getSecurity(apiInstanceMetaData, tableUpdatePermissionName));

         Method idDelete = new Method()
            .withSummary("Delete one " + tableLabel)
            .withDescription(DELETE_DESCRIPTION)
            .withOperationId("delete" + tableApiNameUcFirst)
            .withTags(ListBuilder.of(tableLabel))
            .withParameters(ListBuilder.of(
               new Parameter()
                  .withName(primaryKeyApiName)
                  .withDescription(primaryKeyLabel + " of the " + tableLabel + " to delete.")
                  .withIn(In.PATH)
                  .withRequired(true)
                  .withSchema(new Schema().withType(getFieldType(primaryKeyField)))))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.NOT_FOUND.getCode(), buildStandardErrorResponse("The requested " + tableLabel + " record was not found.", "Could not find " + tableLabel + " with " + primaryKeyLabel + " of 47."))
            .withResponse(HttpStatus.NO_CONTENT.getCode(), new Response().withDescription("Successfully deleted the requested " + tableLabel))
            .withSecurity(getSecurity(apiInstanceMetaData, tableDeletePermissionName));

         if(getEnabled || updateEnabled || deleteEnabled)
         {
            openAPI.getPaths().put(basePath + tableApiName + "/{" + primaryKeyApiName + "}", new Path()
               .withGet(getEnabled ? idGet : null)
               .withPatch(updateEnabled ? idPatch : null)
               .withDelete(deleteEnabled ? idDelete : null)
            );
         }

         Method slashPost = new Method()
            .withSummary("Create one " + tableLabel)
            .withDescription(INSERT_DESCRIPTION)
            .withOperationId("create" + tableApiNameUcFirst)
            .withRequestBody(new RequestBody()
               .withRequired(true)
               .withDescription("Values for the " + tableLabel + " record to create.")
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema().withRef("#/components/schemas/" + tableApiName))
               )))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.CREATED.getCode(), new Response()
               .withDescription("Successfully created the requested " + tableLabel)
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema()
                     .withType(Type.OBJECT)
                     .withProperties(MapBuilder.of(primaryKeyApiName, new Schema()
                        .withType(getFieldType(primaryKeyField))
                        .withExample("47")))))))
            .withTags(ListBuilder.of(tableLabel))
            .withSecurity(getSecurity(apiInstanceMetaData, tableInsertPermissionName));

         if(insertEnabled)
         {
            openAPI.getPaths().put(basePath + tableApiName + "/", new Path()
               .withPost(slashPost));
         }

         ////////////////
         // bulk paths //
         ////////////////
         Method bulkPost = new Method()
            .withSummary("Create multiple " + tableLabel + " records")
            .withDescription(BULK_INSERT_DESCRIPTION)
            .withOperationId("create" + tableApiNameUcFirst + "Bulk")
            .withRequestBody(new RequestBody()
               .withRequired(true)
               .withDescription("Values for the " + tableLabel + " records to create.")
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema()
                     .withType(Type.ARRAY)
                     .withItems(new Schema().withRef("#/components/schemas/" + tableApiName))))))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.MULTI_STATUS.getCode(), buildMultiStatusResponse(tableLabel, primaryKeyApiName, primaryKeyField, "post"))
            .withTags(ListBuilder.of(tableLabel))
            .withSecurity(getSecurity(apiInstanceMetaData, tableInsertPermissionName));

         Method bulkPatch = new Method()
            .withSummary("Update multiple " + tableLabel + " records")
            .withDescription(BULK_UPDATE_DESCRIPTION)
            .withOperationId("update" + tableApiNameUcFirst + "Bulk")
            .withRequestBody(new RequestBody()
               .withRequired(true)
               .withDescription("Values for the " + tableLabel + " records to update.")
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema()
                     .withType(Type.ARRAY)
                     .withItems(new Schema()
                        .withAllOf(ListBuilder.of(new Schema().withRef("#/components/schemas/" + tableApiName)))
                        .withProperties(MapBuilder.of(primaryKeyApiName, new Schema()
                           .withType(getFieldType(primaryKeyField))
                           .withReadOnly(false)
                           .withNullable(false)
                           .withExample("47"))))))))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.MULTI_STATUS.getCode(), buildMultiStatusResponse(tableLabel, primaryKeyApiName, primaryKeyField, "patch"))
            .withTags(ListBuilder.of(tableLabel))
            .withSecurity(getSecurity(apiInstanceMetaData, tableUpdatePermissionName));

         Method bulkDelete = new Method()
            .withSummary("Delete multiple " + tableLabel + " records")
            .withDescription(BULK_DELETE_DESCRIPTION)
            .withOperationId("delete" + tableApiNameUcFirst + "Bulk")
            .withRequestBody(new RequestBody()
               .withRequired(true)
               .withDescription(primaryKeyLabel + " values for the " + tableLabel + " records to delete.")
               .withContent(MapBuilder.of("application/json", new Content()
                  .withSchema(new Schema()
                     .withType(Type.ARRAY)
                     .withItems(new Schema().withType(getFieldType(primaryKeyField)))
                     .withExample(List.of(42, 47))))))
            .withResponses(buildStandardErrorResponses(apiInstanceMetaData))
            .withResponse(HttpStatus.MULTI_STATUS.getCode(), buildMultiStatusResponse(tableLabel, primaryKeyApiName, primaryKeyField, "delete"))
            .withTags(ListBuilder.of(tableLabel))
            .withSecurity(getSecurity(apiInstanceMetaData, tableDeletePermissionName));

         if(insertBulkEnabled || updateBulkEnabled || deleteBulkEnabled)
         {
            openAPI.getPaths().put(basePath + tableApiName + "/bulk", new Path()
               .withPost(insertBulkEnabled ? bulkPost : null)
               .withPatch(updateBulkEnabled ? bulkPatch : null)
               .withDelete(deleteBulkEnabled ? bulkDelete : null));
         }

         ///////////////////////////////////////
         // add processes under the table     //
         // do we want a unique tag for them? //
         ///////////////////////////////////////
         /*
         String tableProcessesTag = tableLabel + " Processes";
         if(CollectionUtils.nullSafeHasContents(apiProcessMetaDataList))
         {
            tagList.add(new Tag()
               .withName(tableProcessesTag)
               .withDescription("Process on the " + tableLabel + " table."));
         }
         */
         String tableProcessesTag = tableLabel;

         for(Pair<ApiProcessMetaData, QProcessMetaData> pair : CollectionUtils.nonNullList(apiProcessMetaDataList))
         {
            ApiProcessMetaData apiProcessMetaData = pair.getA();
            QProcessMetaData   processMetaData    = pair.getB();

            addProcessEndpoints(qInstance, apiInstanceMetaData, basePath, openAPI, tableProcessesTag, apiProcessMetaData, processMetaData, apiVersion);

            usedProcessNames.add(processMetaData.getName());
         }
      }

      /////////////////////////////
      // add non-table processes //
      /////////////////////////////
      if(input.getTableName() == null)
      {
         List<Pair<ApiProcessMetaData, QProcessMetaData>> processesNotUnderTables = getProcessesNotUnderTables(apiName, apiVersion, usedProcessNames);
         for(Pair<ApiProcessMetaData, QProcessMetaData> pair : CollectionUtils.nonNullList(processesNotUnderTables))
         {
            ApiProcessMetaData apiProcessMetaData = pair.getA();
            QProcessMetaData   processMetaData    = pair.getB();

            String tag;
            if(StringUtils.hasContent(apiProcessMetaData.getTag()))
            {
               tag = apiProcessMetaData.getTag();
            }
            else
            {
               tag = processMetaData.getLabel();
               if(doesProcessLabelNeedTheWordProcessAppended(tag))
               {
                  tag += " process";
               }
            }

            tagList.add(new Tag()
               .withName(tag)
               .withDescription(tag));

            addProcessEndpoints(qInstance, apiInstanceMetaData, basePath, openAPI, tag, apiProcessMetaData, processMetaData, apiVersion);

            usedProcessNames.add(processMetaData.getName());
         }
      }

      tagList.sort(Comparator.comparing(Tag::getName));
      openAPI.setTags(tagList);

      ////////////////////////////
      // define standard errors //
      ////////////////////////////
      componentResponses.put("error" + HttpStatus.BAD_REQUEST.getCode(), buildStandardErrorResponse("Bad Request.  Some portion of the request's content was not acceptable to the server.  See error message in body for details.", "Parameter id should be given an integer value, but received string: \"Foo\""));
      componentResponses.put("error" + HttpStatus.UNAUTHORIZED.getCode(), buildStandardErrorResponse("Unauthorized.  The required authentication credentials were missing or invalid.", "The required authentication credentials were missing or invalid."));
      componentResponses.put("error" + HttpStatus.FORBIDDEN.getCode(), buildStandardErrorResponse("Forbidden.  You do not have permission to access the requested resource.", "You do not have permission to access the requested resource."));
      componentResponses.put("error" + HttpStatus.INTERNAL_SERVER_ERROR.getCode(), buildStandardErrorResponse("Internal Server Error.  An error occurred in the server while processing the request.", "Database connection error.  Try again later."));

      if(apiInstanceMetaData.getIncludeErrorTooManyRequests())
      {
         componentResponses.put("error" + HttpStatus.TOO_MANY_REQUESTS.getCode(), buildStandardErrorResponse("Too Many Requests.  Your application has issued too many API requests in too short of a time frame."));
      }

      ensureAllNeededTableSchemasExist(apiInstanceMetaData, version, componentSchemas);

      GenerateOpenApiSpecOutput output = new GenerateOpenApiSpecOutput();
      output.setOpenAPI(openAPI);
      output.setYaml(YamlUtils.toYaml(openAPI));
      output.setJson(JsonUtils.toPrettyJson(openAPI));
      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addProcessEndpoints(QInstance qInstance, ApiInstanceMetaData apiInstanceMetaData, String basePath, OpenAPI openAPI, String tag, ApiProcessMetaData apiProcessMetaData, QProcessMetaData processMetaData, APIVersion apiVersion)
   {
      String processApiPath = ApiProcessUtils.getProcessApiPath(qInstance, processMetaData, apiProcessMetaData, apiInstanceMetaData);

      ///////////////////////////
      // do the process itself //
      ///////////////////////////
      Path path = generateProcessSpecPathObject(apiInstanceMetaData, apiProcessMetaData, processMetaData, ListBuilder.of(tag), apiVersion);
      openAPI.getPaths().put(basePath + processApiPath, path);

      ///////////////////////////////////////////////////////////////////////
      // if the process can run async, then do the status checkin endpoint //
      ///////////////////////////////////////////////////////////////////////
      if(!ApiProcessMetaData.AsyncMode.NEVER.equals(apiProcessMetaData.getAsyncMode()))
      {
         Path statusPath = generateProcessStatusSpecPathObject(apiInstanceMetaData, apiProcessMetaData, processMetaData, ListBuilder.of(tag));
         openAPI.getPaths().put(basePath + processApiPath + "/status/{jobId}", statusPath);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Pair<ApiProcessMetaData, QProcessMetaData>> getProcessesNotUnderTables(String apiName, APIVersion apiVersion, Set<String> usedProcessNames)
   {
      List<Pair<ApiProcessMetaData, QProcessMetaData>> apiProcessMetaDataList = new ArrayList<>();
      for(QProcessMetaData processMetaData : CollectionUtils.nonNullMap(QContext.getQInstance().getProcesses()).values())
      {
         if(usedProcessNames.contains(processMetaData.getName()))
         {
            continue;
         }

         ApiProcessMetaDataContainer apiProcessMetaDataContainer = ApiProcessMetaDataContainer.of(processMetaData);
         if(apiProcessMetaDataContainer == null)
         {
            continue;
         }

         ApiProcessMetaData apiProcessMetaData = apiProcessMetaDataContainer.getApis().get(apiName);
         if(apiProcessMetaData == null)
         {
            continue;
         }

         if(!apiProcessMetaData.getApiVersionRange().includes(apiVersion))
         {
            continue;
         }

         apiProcessMetaDataList.add(Pair.of(apiProcessMetaData, processMetaData));
      }

      apiProcessMetaDataList.sort(Comparator.comparing(apiProcessMetaDataQProcessMetaDataPair -> getProcessSummary(apiProcessMetaDataQProcessMetaDataPair.getA(), apiProcessMetaDataQProcessMetaDataPair.getB())));

      return (apiProcessMetaDataList);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Path generateProcessSpecPathObject(ApiInstanceMetaData apiInstanceMetaData, ApiProcessMetaData apiProcessMetaData, QProcessMetaData processMetaData, List<String> tags, APIVersion apiVersion)
   {
      String description = apiProcessMetaData.getDescription();
      if(!StringUtils.hasContent(description))
      {
         description = "Run the " + processMetaData.getLabel();
         if(doesProcessLabelNeedTheWordProcessAppended(description))
         {
            description += " process";
         }
      }

      ////////////////////////////////
      // start defining the process //
      ////////////////////////////////
      Method methodForProcess = new Method()
         .withOperationId(apiProcessMetaData.getApiProcessName())
         .withTags(tags)
         .withSummary(getProcessSummary(apiProcessMetaData, processMetaData))
         .withDescription(description)
         .withSecurity(getSecurity(apiInstanceMetaData, processMetaData.getName()));

      ////////////////////////////////
      // add inputs for the process //
      ////////////////////////////////
      List<Parameter> parameters      = new ArrayList<>();
      ApiProcessInput apiProcessInput = apiProcessMetaData.getInput();
      String          apiName         = apiInstanceMetaData.getName();
      if(apiProcessInput != null)
      {
         /////////////////
         // path params //
         /////////////////
         ApiProcessInputFieldsContainer pathParams = apiProcessInput.getPathParams();
         if(pathParams != null)
         {
            for(QFieldMetaData field : CollectionUtils.nonNullList(pathParams.getFields()))
            {
               parameters.add(processFieldToParameter(apiInstanceMetaData, field).withIn(In.PATH));
            }
         }

         /////////////////////////
         // query string params //
         /////////////////////////
         ApiProcessInputFieldsContainer queryStringParams = apiProcessInput.getQueryStringParams();
         if(queryStringParams != null)
         {
            if(queryStringParams.getRecordIdsField() != null)
            {
               parameters.add(processFieldToParameter(apiInstanceMetaData, queryStringParams.getRecordIdsField()).withIn(In.QUERY));
            }

            for(QFieldMetaData field : CollectionUtils.nonNullList(queryStringParams.getFields()))
            {
               if(ApiFieldUtils.isIncluded(apiName, field) && ApiFieldUtils.getApiVersionRange(apiName, field).includes(apiVersion))
               {
                  parameters.add(processFieldToParameter(apiInstanceMetaData, field).withIn(In.QUERY));
               }
            }
         }

         /////////////////////
         // Body as content //
         /////////////////////
         QFieldMetaData bodyField = apiProcessInput.getBodyField();
         if(bodyField != null)
         {
            ApiFieldMetaDataContainer apiFieldMetaDataContainer = ApiFieldMetaDataContainer.ofOrNew(bodyField);
            ApiFieldMetaData          apiFieldMetaData          = apiFieldMetaDataContainer.getApiFieldMetaData(apiName);

            String fieldLabel = bodyField.getLabel();
            String fieldName  = ApiFieldMetaData.getEffectiveApiFieldName(apiName, bodyField);
            if(!StringUtils.hasContent(fieldLabel))
            {
               fieldLabel = QInstanceEnricher.nameToLabel(fieldName);
            }

            String bodyDescription = "Value for the " + fieldLabel;
            if(apiFieldMetaData != null && StringUtils.hasContent(apiFieldMetaData.getDescription()))
            {
               bodyDescription = apiFieldMetaData.getDescription();
            }

            Content content = new Content();
            if(apiFieldMetaData != null && apiFieldMetaData.getExample() instanceof ExampleWithSingleValue exampleWithSingleValue)
            {
               content.withSchema(new Schema()
                  .withDescription(bodyDescription)
                  .withType(Type.STRING)
                  .withExample(exampleWithSingleValue.getValue()));
            }

            methodForProcess.withRequestBody(new RequestBody()
               .withDescription(bodyDescription)
               .withRequired(bodyField.getIsRequired())
               .withContent(MapBuilder.of(apiProcessInput.getBodyFieldContentType(), content)));
         }

         // todo - form & record body params
         // todo methodForProcess.withRequestBody();
      }

      ////////////////////////////////////////////////////////
      // add the async input for optionally-async processes //
      ////////////////////////////////////////////////////////
      if(ApiProcessMetaData.AsyncMode.OPTIONAL.equals(apiProcessMetaData.getAsyncMode()))
      {
         parameters.add(new Parameter()
            .withName("async")
            .withIn(In.QUERY)
            .withDescription("""
               Indicates if the job should be ran asynchronously.
               If false or not specified, then the job is ran synchronously and returns with an appropriate response status when completed.
               If true, then the request returns immediately with response status of 202 (Accepted), and a jobId in the response body,
               which can then be sent to the corresponding .../status/{jobId} endpoint to follow-up on the status of the job.
               """)
            .withExamples(MapBuilder.of(
               "false", new ExampleWithSingleValue().withValue(false).withSummary("Run the job synchronously."),
               "true", new ExampleWithSingleValue().withValue(true).withSummary("Run the job asynchronously.")
            ))
            .withSchema(new Schema().withType(Type.BOOLEAN)));
      }

      if(CollectionUtils.nullSafeHasContents(parameters))
      {
         methodForProcess.setParameters(parameters);
      }

      //////////////////////////////////
      // build all possible responses //
      //////////////////////////////////
      Map<Integer, Response> responses = new LinkedHashMap<>();

      ApiProcessOutputInterface output = apiProcessMetaData.getOutput();
      if(!ApiProcessMetaData.AsyncMode.ALWAYS.equals(apiProcessMetaData.getAsyncMode()))
      {
         responses.putAll(output.getSpecResponses(apiName));
      }

      if(!ApiProcessMetaData.AsyncMode.NEVER.equals(apiProcessMetaData.getAsyncMode()))
      {
         responses.put(HttpStatus.ACCEPTED.getCode(), new Response()
            .withDescription("The process has been started asynchronously.  You can call back later to check its status.")
            .withContent(MapBuilder.of(ContentType.JSON, new Content()
               .withSchema(new Schema()
                  .withType(Type.OBJECT)
                  .withProperties(MapBuilder.of(
                     "jobId", new Schema().withType(Type.STRING).withFormat("uuid").withDescription("id of the asynchronous job")
                  ))
               )
            ))
         );
      }

      responses.putAll(buildStandardErrorResponses(apiInstanceMetaData));
      methodForProcess.withResponses(responses);

      Path path = switch(apiProcessMetaData.getMethod())
      {
         case GET -> new Path().withGet(methodForProcess);
         case POST -> new Path().withPost(methodForProcess);
         case PUT -> new Path().withPut(methodForProcess);
         case PATCH -> new Path().withPatch(methodForProcess);
         case DELETE -> new Path().withDelete(methodForProcess);
      };

      return (path);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Path generateProcessStatusSpecPathObject(ApiInstanceMetaData apiInstanceMetaData, ApiProcessMetaData apiProcessMetaData, QProcessMetaData processMetaData, List<String> tags)
   {
      ////////////////////////////////
      // start defining the process //
      ////////////////////////////////
      Method methodForProcess = new Method()
         .withOperationId("getStatusFor" + StringUtils.ucFirst(apiProcessMetaData.getApiProcessName()))
         .withTags(tags)
         .withSummary("Get Status of Job: " + getProcessSummary(apiProcessMetaData, processMetaData))
         .withDescription("Get the status for a previous asynchronous call to the process named " + processMetaData.getLabel())
         .withSecurity(getSecurity(apiInstanceMetaData, processMetaData.getName()));

      List<Parameter>                parameters      = new ArrayList<>();
      ApiProcessInput                apiProcessInput = apiProcessMetaData.getInput();
      ApiProcessInputFieldsContainer pathParams      = apiProcessInput.getPathParams();
      if(pathParams != null)
      {
         for(QFieldMetaData field : CollectionUtils.nonNullList(pathParams.getFields()))
         {
            parameters.add(processFieldToParameter(apiInstanceMetaData, field).withIn(In.PATH));
         }
      }

      parameters.add(new Parameter()
         .withName("jobId")
         .withIn(In.PATH)
         .withRequired(true)
         .withDescription("Id of the job, as returned by the API call that started it.")
         .withSchema(new Schema().withType(Type.STRING).withFormat("uuid")));

      ////////////////////////////////////////////////////////
      // add the async input for optionally-async processes //
      ////////////////////////////////////////////////////////
      methodForProcess.setParameters(parameters);

      //////////////////////////////////
      // build all possible responses //
      //////////////////////////////////
      Map<Integer, Response> responses = new LinkedHashMap<>();
      responses.put(HttpStatus.ACCEPTED.getCode(), new Response()
         .withDescription("The process is still running.  You can call back later to get its final status.")
         .withContent(MapBuilder.of(ContentType.JSON, new Content()
            .withSchema(new Schema()
               .withType(Type.OBJECT)
               .withProperties(MapBuilder.of(
                  "jobId", new Schema().withType(Type.STRING).withFormat("uuid").withDescription("id of the asynchronous job"),
                  "message", new Schema().withNullable(true).withType(Type.STRING).withDescription("a status message about the progress of the job").withExample("Processing records"),
                  "current", new Schema().withNullable(true).withType(Type.INTEGER).withDescription("for jobs that count progress, indicator of the current number being processed").withExample(7),
                  "total", new Schema().withNullable(true).withType(Type.INTEGER).withDescription("for jobs that count progress, indicator of the total number being processed").withExample(9)
               ))
            )
         ))
      );

      ApiProcessOutputInterface output = apiProcessMetaData.getOutput();
      responses.putAll(output.getSpecResponses(apiInstanceMetaData.getName()));
      responses.putAll(buildStandardErrorResponses(apiInstanceMetaData));

      methodForProcess.withResponses(responses);
      return (new Path().withGet(methodForProcess));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Parameter processFieldToParameter(ApiInstanceMetaData apiInstanceMetaData, QFieldMetaData field)
   {
      String apiName = apiInstanceMetaData.getName();

      ApiFieldMetaDataContainer apiFieldMetaDataContainer = ApiFieldMetaDataContainer.ofOrNew(field);
      ApiFieldMetaData          apiFieldMetaData          = apiFieldMetaDataContainer.getApiFieldMetaData(apiName);

      String fieldName  = ApiFieldMetaData.getEffectiveApiFieldName(apiName, field);
      String fieldLabel = field.getLabel();
      if(!StringUtils.hasContent(fieldLabel))
      {
         fieldLabel = QInstanceEnricher.nameToLabel(fieldName);
      }

      String description = "Value for the " + fieldLabel + " field.";
      if(apiFieldMetaData != null && apiFieldMetaData.getDescription() != null)
      {
         description = apiFieldMetaData.getDescription();
      }

      if(field.getDefaultValue() != null)
      {
         description += " Default value is " + field.getDefaultValue() + ", if not given.";
      }

      Schema fieldSchema = getFieldSchema(field, description, apiInstanceMetaData);

      Parameter parameter = new Parameter()
         .withName(fieldName)
         .withDescription(description)
         .withRequired(field.getIsRequired())
         .withSchema(fieldSchema);

      if(apiFieldMetaData != null)
      {
         if(apiFieldMetaData.getExample() != null)
         {
            parameter.withExamples(Map.of("example", apiFieldMetaData.getExample()));
         }
         else if(apiFieldMetaData.getExamples() != null)
         {
            parameter.withExamples(apiFieldMetaData.getExamples());
         }
      }

      return (parameter);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Pair<ApiProcessMetaData, QProcessMetaData>> getProcessesUnderTable(QTableMetaData table, String apiName, APIVersion apiVersion)
   {
      List<Pair<ApiProcessMetaData, QProcessMetaData>> apiProcessMetaDataList = new ArrayList<>();
      for(QProcessMetaData processMetaData : CollectionUtils.nonNullMap(QContext.getQInstance().getProcesses()).values())
      {
         if(!table.getName().equals(processMetaData.getTableName()))
         {
            continue;
         }

         ApiProcessMetaDataContainer apiProcessMetaDataContainer = ApiProcessMetaDataContainer.of(processMetaData);
         if(apiProcessMetaDataContainer == null)
         {
            continue;
         }

         ApiProcessMetaData apiProcessMetaData = apiProcessMetaDataContainer.getApis().get(apiName);
         if(apiProcessMetaData == null)
         {
            continue;
         }

         if(!apiProcessMetaData.getApiVersionRange().includes(apiVersion))
         {
            continue;
         }

         apiProcessMetaDataList.add(Pair.of(apiProcessMetaData, processMetaData));
      }

      /////////////////////////////////////////////////////////////////////
      // sort by process summary (for stability, and just to be better) //
      /////////////////////////////////////////////////////////////////////
      apiProcessMetaDataList.sort(Comparator.comparing(apiProcessMetaDataQProcessMetaDataPair -> getProcessSummary(apiProcessMetaDataQProcessMetaDataPair.getA(), apiProcessMetaDataQProcessMetaDataPair.getB())));

      return (apiProcessMetaDataList);
   }



   /*******************************************************************************
    ** written for the use-case of, generating a single table's api, but it has
    ** associations that it references, so we need their schemas too - so, make
    ** sure they are all added to the componentSchemas map.
    *******************************************************************************/
   private void ensureAllNeededTableSchemasExist(ApiInstanceMetaData apiInstanceMetaData, String version, LinkedHashMap<String, Schema> componentSchemas) throws QException
   {
      String apiName = apiInstanceMetaData.getName();

      boolean addedAny;
      do
      {
         //////////////////////////////////////////////////////////////////////////
         // mmm, kinda odd loops, to avoid concurrent modification, and so-forth //
         //////////////////////////////////////////////////////////////////////////
         addedAny = false;
         for(String neededTableSchema : neededTableSchemas)
         {
            if(!componentSchemas.containsKey(neededTableSchema))
            {
               LOG.debug("Adding needed schema: " + neededTableSchema);
               QTableMetaData table = QContext.getQInstance().getTable(neededTableSchema);

               ApiTableMetaDataContainer apiTableMetaDataContainer = ApiTableMetaDataContainer.of(table);
               ApiTableMetaData          apiTableMetaData          = apiTableMetaDataContainer.getApiTableMetaData(apiName);

               String tableApiName = StringUtils.hasContent(apiTableMetaData.getApiTableName()) ? apiTableMetaData.getApiTableName() : table.getName();

               List<QFieldMetaData> tableApiFields = new GetTableApiFieldsAction().execute(new GetTableApiFieldsInput()
                  .withTableName(table.getName())
                  .withVersion(version)
                  .withApiName(apiName)).getFields();

               componentSchemas.put(tableApiName, buildTableSchema(apiInstanceMetaData, version, table, tableApiFields));
               addedAny = true;
               break;
            }
         }
      }
      while(addedAny);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Schema buildTableSchema(ApiInstanceMetaData apiInstanceMetaData, String version, QTableMetaData table, List<QFieldMetaData> tableApiFields)
   {
      LinkedHashMap<String, Schema> tableFields = new LinkedHashMap<>();
      Schema tableSchema = new Schema()
         .withType(Type.OBJECT)
         .withProperties(tableFields);

      for(QFieldMetaData field : tableApiFields)
      {
         String fieldLabel = field.getLabel();
         String fieldName  = ApiFieldMetaData.getEffectiveApiFieldName(apiInstanceMetaData.getName(), field);
         if(!StringUtils.hasContent(fieldLabel))
         {
            fieldLabel = QInstanceEnricher.nameToLabel(fieldName);
         }

         String defaultDescription = fieldLabel + " for the " + table.getLabel() + ".";
         Schema fieldSchema        = getFieldSchema(field, defaultDescription, apiInstanceMetaData);
         tableFields.put(fieldName, fieldSchema);
      }

      //////////////////////////////////
      // recursively add associations //
      //////////////////////////////////
      addAssociations(apiInstanceMetaData.getName(), version, table, tableSchema);

      return (tableSchema);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addAssociations(String apiName, String version, QTableMetaData table, Schema tableSchema)
   {
      ApiTableMetaData thisApiTableMetaData = ObjectUtils.tryElse(() -> ApiTableMetaDataContainer.of(table).getApiTableMetaData(apiName), new ApiTableMetaData());

      for(Association association : CollectionUtils.nonNullList(table.getAssociations()))
      {
         String           associatedTableName        = association.getAssociatedTableName();
         QTableMetaData   associatedTable            = QContext.getQInstance().getTable(associatedTableName);
         ApiTableMetaData associatedApiTableMetaData = ObjectUtils.tryAndRequireNonNullElse(() -> ApiTableMetaDataContainer.of(associatedTable).getApiTableMetaData(apiName), new ApiTableMetaData());
         String           associatedTableApiName     = StringUtils.hasContent(associatedApiTableMetaData.getApiTableName()) ? associatedApiTableMetaData.getApiTableName() : associatedTableName;

         ApiAssociationMetaData apiAssociationMetaData = thisApiTableMetaData.getApiAssociationMetaData().get(association.getName());
         if(apiAssociationMetaData != null)
         {
            if(BooleanUtils.isTrue(apiAssociationMetaData.getIsExcluded()))
            {
               logOmittedTable("Omitting table [" + table.getName() + "] association [" + association.getName() + "] because it is marked as excluded.");
               continue;
            }

            APIVersionRange apiVersionRange = apiAssociationMetaData.getApiVersionRange();
            if(!apiVersionRange.includes(new APIVersion(version)))
            {
               logOmittedTable("Omitting table [" + table.getName() + "] association [" + association.getName() + "] because its api version range [" + apiVersionRange + "] does not include this version [" + version + "]");
               continue;
            }
         }

         neededTableSchemas.add(associatedTable.getName());

         tableSchema.getProperties().put(association.getName(), new Schema()
            .withType(Type.ARRAY)
            .withItems(new Schema().withRef("#/components/schemas/" + associatedTableApiName)));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Schema getFieldSchema(QFieldMetaData field, String defaultDescription, ApiInstanceMetaData apiInstanceMetaData)
   {
      ApiFieldMetaDataContainer apiFieldMetaDataContainer = ApiFieldMetaDataContainer.ofOrNew(field);
      ApiFieldMetaData          apiFieldMetaData          = apiFieldMetaDataContainer.getApiFieldMetaData(apiInstanceMetaData.getName());

      String description = defaultDescription;
      if(field.getType().equals(QFieldType.BLOB))
      {
         description = "Base64 encoded " + description;
      }

      if(apiFieldMetaData != null && StringUtils.hasContent(apiFieldMetaData.getDescription()))
      {
         description = apiFieldMetaData.getDescription();
      }

      Schema fieldSchema = new Schema()
         .withType(getFieldType(field))
         .withFormat(getFieldFormat(field))
         .withDescription(description);

      if(!field.getIsEditable())
      {
         fieldSchema.setReadOnly(true);
      }

      if(!field.getIsRequired())
      {
         fieldSchema.setNullable(true);
      }

      if(field.getType().isStringLike() && field.getMaxLength() != null)
      {
         fieldSchema.setMaxLength(field.getMaxLength());
      }

      if(StringUtils.hasContent(field.getPossibleValueSourceName()))
      {
         QPossibleValueSource possibleValueSource = QContext.getQInstance().getPossibleValueSource(field.getPossibleValueSourceName());
         if(QPossibleValueSourceType.ENUM.equals(possibleValueSource.getType()))
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // by default, we will list all enum values in the docs - but - a field's api-meta-data object can opt out //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if(apiFieldMetaData == null || apiFieldMetaData.getListEnumPossibleValues())
            {
               List<String> enumValues  = new ArrayList<>();
               List<String> enumMapping = new ArrayList<>();
               for(QPossibleValue<?> enumValue : possibleValueSource.getEnumValues())
               {
                  enumValues.add(String.valueOf(enumValue.getId()));
                  enumMapping.add(enumValue.getId() + "=" + enumValue.getLabel());
               }
               fieldSchema.setEnumValues(enumValues);
               fieldSchema.setDescription(fieldSchema.getDescription() + "  Value definitions are: " + StringUtils.joinWithCommasAndAnd(enumMapping));
            }
         }
         else if(QPossibleValueSourceType.TABLE.equals(possibleValueSource.getType()))
         {
            QTableMetaData sourceTable = QContext.getQInstance().getTable(possibleValueSource.getTableName());
            fieldSchema.setDescription(fieldSchema.getDescription() + "  Values in this field come from the primary key of the " + sourceTable.getLabel() + " table");
         }
      }
      return fieldSchema;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Response buildMultiStatusResponse(String tableLabel, String primaryKeyApiName, QFieldMetaData primaryKeyField, String method)
   {
      List<Object> example = switch(method.toLowerCase())
      {
         case "post" -> ListBuilder.of(
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.CREATED.getCode())
               .with("statusText", HttpStatus.CREATED.getMessage())
               .with(primaryKeyApiName, "47").build(),
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.BAD_REQUEST.getCode())
               .with("statusText", HttpStatus.BAD_REQUEST.getMessage())
               .with("error", "Could not create " + tableLabel + ": Duplicate value in unique key field.").build()
         );
         case "patch" -> ListBuilder.of(
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.NO_CONTENT.getCode())
               .with("statusText", HttpStatus.NO_CONTENT.getMessage())
               .with(primaryKeyApiName, "47").build(),
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.BAD_REQUEST.getCode())
               .with("statusText", HttpStatus.BAD_REQUEST.getMessage())
               .with("error", "Could not update " + tableLabel + ": Missing value in required field: My Field.")
               .with(primaryKeyApiName, "47").build(),
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.NOT_FOUND.getCode())
               .with("statusText", HttpStatus.NOT_FOUND.getMessage())
               .with("error", "The requested " + tableLabel + " to update was not found.")
               .with(primaryKeyApiName, "47").build()
         );
         case "delete" -> ListBuilder.of(
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.NO_CONTENT.getCode())
               .with("statusText", HttpStatus.NO_CONTENT.getMessage())
               .with(primaryKeyApiName, "47").build(),
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.BAD_REQUEST.getCode())
               .with("statusText", HttpStatus.BAD_REQUEST.getMessage())
               .with("error", "Could not delete " + tableLabel + ": Foreign key constraint violation.")
               .with(primaryKeyApiName, "47").build(),
            MapBuilder.of(LinkedHashMap::new)
               .with("statusCode", HttpStatus.NOT_FOUND.getCode())
               .with("statusText", HttpStatus.NOT_FOUND.getMessage())
               .with("error", "The requested " + tableLabel + " to delete was not found.")
               .with(primaryKeyApiName, "47").build()
         );
         default -> throw (new IllegalArgumentException("Unrecognized method: " + method));
      };

      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("statusCode", new Schema().withType(Type.INTEGER));
      properties.put("statusText", new Schema().withType(Type.STRING));
      properties.put("error", new Schema().withType(Type.STRING));
      properties.put(primaryKeyApiName, new Schema().withType(getFieldType(primaryKeyField)));

      return new Response()
         .withDescription("Multiple statuses.  See body for details.")
         .withContent(MapBuilder.of("application/json", new Content()
            .withSchema(new Schema()
               .withType(Type.ARRAY)
               .withItems(new Schema()
                  .withType(Type.OBJECT)
                  .withProperties(properties))
               .withExample(example))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, Example> buildOrderByExamples(String apiName, String primaryKeyApiName, List<? extends QFieldMetaData> tableApiFields)
   {
      Map<String, Example> rs = new LinkedHashMap<>();

      List<String> fieldsForExample4 = new ArrayList<>();
      List<String> fieldsForExample5 = new ArrayList<>();
      for(QFieldMetaData tableApiField : tableApiFields)
      {
         String name = ApiFieldMetaData.getEffectiveApiFieldName(apiName, tableApiField);
         if(primaryKeyApiName.equals(name) || fieldsForExample4.contains(name) || fieldsForExample5.contains(name))
         {
            continue;
         }

         if(!fieldsForExample4.contains(name) && fieldsForExample4.size() < 2)
         {
            fieldsForExample4.add(name);
         }
         else if(!fieldsForExample5.contains(name) && fieldsForExample5.size() < 3)
         {
            fieldsForExample5.add(name);
         }

         if(fieldsForExample5.size() == 3)
         {
            break;
         }
      }

      rs.put("default", new ExampleWithListValue()
         .withSummary("default:  order by " + primaryKeyApiName + " descending")
         .withValue(ListBuilder.of("")));

      rs.put(primaryKeyApiName, new ExampleWithSingleValue()
         .withSummary("order by " + primaryKeyApiName + " (ascending, since ASC/DESC was not specified)")
         .withValue("id"));

      rs.put(primaryKeyApiName + "Desc", new ExampleWithSingleValue()
         .withSummary("order by " + primaryKeyApiName + " descending")
         .withValue("id DESC"));

      rs.put(primaryKeyApiName + "Asc", new ExampleWithSingleValue()
         .withSummary("order by " + primaryKeyApiName + " ascending")
         .withValue("id ASC"));

      if(fieldsForExample4.size() == 2)
      {
         String a = fieldsForExample4.get(0);
         String b = fieldsForExample4.get(1);

         String name = a + "And" + StringUtils.ucFirst(b);
         rs.put(name, new ExampleWithSingleValue()
            .withSummary("order by " + a + ", then by " + b + " (both ascending)")
            .withValue(a + ", " + b));
      }

      if(fieldsForExample5.size() == 3)
      {
         String a = fieldsForExample5.get(0);
         String b = fieldsForExample5.get(1);
         String c = fieldsForExample5.get(2);

         String name = a + "And" + StringUtils.ucFirst(b) + "And" + StringUtils.ucFirst(c);
         rs.put(name, new ExampleWithSingleValue()
            .withSummary("order by " + a + " descending, then by " + b + " ascending, then by " + c)
            .withValue(a + " DESC, " + b + " ASC, " + c));
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getFieldFormat(QFieldMetaData field)
   {
      return (getFieldFormat(field.getType()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getFieldFormat(QFieldType type)
   {
      return switch(type)
      {
         case DATE -> "date";
         case TIME -> "time"; // non-standard format...
         case DATE_TIME -> "date-time";
         case PASSWORD -> "password";
         case BLOB -> "byte"; // base-64-encoded, per https://swagger.io/docs/specification/data-models/data-types/#file
         default -> null;
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void logOmittedTable(String message)
   {
      if(LOG_OMITTED_TABLES)
      {
         LOG.debug(message);
      }
   }

}
