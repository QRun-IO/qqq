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

package com.kingsrook.sampleapp;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBTableBackendDetails;
import com.kingsrook.sampleapp.metadata.FieldLabTableMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical Field Lab on an owned standalone MongoDB. This Failsafe IT reuses
 ** sample metadata; it has no dependency on another module's test fixtures.
 *******************************************************************************/
class SampleMongoDatabaseIT
{
   private static final String TABLE = FieldLabTableMetaDataProducer.NAME;
   private static final String COLLECTION = "field_lab";
   private static final String BACKEND = "sampleMongoAcceptance";
   private static final String DATABASE = "qqq_sample_" + UUID.randomUUID().toString().replace("-", "");
   private static final String USERNAME = "sample";
   private static final String PASSWORD = "sample-fixture-only";
   private static final String OWNER_KEY = "mongoSampleOwner";
   private static final String LENGTH = "sampleNameLength";
   private static GenericContainer<?> container;
   private static MongoClient client;
   private QInstance instance;
   private QSession session;
   private QTableMetaData canonical;
   private String canonicalBefore;
   private List<String> unrelatedBefore;



   /*******************************************************************************
    ** The client and container are both owned here, including failed startup.
    *******************************************************************************/
   @BeforeAll
   static void startMongo()
   {
      container = new GenericContainer<>(DockerImageName.parse("mongo:7.0"))
         .withEnv("MONGO_INITDB_ROOT_USERNAME", USERNAME).withEnv("MONGO_INITDB_ROOT_PASSWORD", PASSWORD)
         .withExposedPorts(27017).waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1));
      try
      {
         container.start();
         client = MongoClients.create(MongoClientSettings.builder()
            .applyToClusterSettings(settings -> settings.hosts(List.of(new ServerAddress(container.getHost(), container.getMappedPort(27017)))))
            .credential(MongoCredential.createCredential(USERNAME, "admin", PASSWORD.toCharArray())).build());
         client.getDatabase("admin").runCommand(new Document("ping", 1));
      }
      catch(RuntimeException failure)
      {
         stopMongo();
         throw failure;
      }
   }



   /*******************************************************************************
    ** Always close the driver before stopping its disposable server.
    *******************************************************************************/
   @AfterAll
   static void stopMongo()
   {
      try
      {
         if(client != null)
         {
            client.close();
            client = null;
         }
      }
      finally
      {
         if(container != null)
         {
            container.stop();
            container = null;
         }
      }
   }



   /*******************************************************************************
    ** Only id needs Mongo's STRING/_id mapping. A fixture-only editable INTEGER
    ** copy retains the original id's type coverage; all other fields stay intact.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      instance = SampleMetaDataProvider.defineTestInstance();
      session = new QSession();
      QContext.init(instance, session);
      new QInstanceValidator().validate(instance);
      canonical = instance.getTable(TABLE);
      canonicalBefore = JsonUtils.toJson(canonical);
      QTableMetaData mongoTable = canonical.clone();
      QFieldMetaData integer = canonical.getField("id").clone();
      integer.setName("integerValue");
      integer.setBackendName("integer_value");
      integer.setIsEditable(true);
      mongoTable.withField(integer);
      mongoTable.getSections().get(0).getFieldNames().add("integerValue");
      mongoTable.getField("id").setType(QFieldType.STRING);
      mongoTable.getField("id").setBackendName("_id");
      mongoTable.setBackendName(BACKEND);
      mongoTable.setBackendDetails(new MongoDBTableBackendDetails().withTableName(COLLECTION));
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, mongoTable);
      instance.setTables(tables);
      instance.addBackend(new MongoDBBackendMetaData().withName(BACKEND).withHost(container.getHost()).withPort(container.getMappedPort(27017))
         .withDatabaseName(DATABASE).withUsername(USERNAME).withPassword(PASSWORD).withAuthSourceDatabase("admin").withTransactionsSupported(false));
      instance.setHasBeenValidated(null);
      new QInstanceValidator().validate(instance);
      MongoDBBackendMetaData backend = (MongoDBBackendMetaData) instance.getBackend(BACKEND);
      assertNotNull(backend.getFieldFunctionAdapter(StringLengthFunction.IDENTIFIER), "New Mongo backend must have its registered native StringLength adapter");
      assertNotNull(backend.getFieldFunctionAdapter(SubStringFunction.IDENTIFIER), "New Mongo backend must have its registered native SubString adapter");
      assertCanonicalFields();
      database().drop();
      database().getCollection("unrelated").insertOne(new Document("_id", "untouched").append("payload", new Document("source", "owned sentinel")).append("bytes", new Binary(new byte[] { 0, 1, -1 })));
      unrelatedBefore = collectionSnapshot("unrelated");
   }



   /*******************************************************************************
    ** Assertions run before dropping only this generated database name.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      try
      {
         if(canonicalBefore != null && unrelatedBefore != null)
         {
            assertAll(
               () -> assertCanonicalFields(),
               () -> assertEquals(unrelatedBefore, collectionSnapshot("unrelated")),
               () -> assertSame(instance, QContext.getQInstance()),
               () -> assertSame(session, QContext.getQSession()));
         }
      }
      finally
      {
         try
         {
            if(client != null)
            {
               database().drop();
            }
         }
         finally
         {
            QContext.clear();
         }
      }
   }



   /*******************************************************************************
    ** Generated native ObjectIds map back to the same public STRING identities.
    *******************************************************************************/
   @Test
   void testGeneratedSingleAndBatchKeysRoundTripThroughGet() throws Exception
   {
      QRecord first = insert(new QRecord().withValue("name", "Single generated"));
      List<QRecord> batch = new InsertAction().execute(new InsertInput(TABLE).withRecords(List.of(
         new QRecord().withValue("name", "Batch one"), new QRecord().withValue("name", "Batch two")))).getRecords();
      assertEquals(2, batch.size());
      List<QRecord> records = new ArrayList<>(List.of(first));
      records.addAll(batch);
      List<String> ids = new ArrayList<>();
      for(QRecord record : records)
      {
         successful(record);
         String key = assertInstanceOf(String.class, record.getValue("id"));
         assertTrue(ObjectId.isValid(key));
         ids.add(key);
         Document nativeRow = collection().find(new Document("_id", new ObjectId(key))).first();
         assertNotNull(nativeRow);
         assertEquals(record.getValue("name"), nativeRow.getString("name"));
         assertEquals(true, nativeRow.getBoolean("boolean_value"));
         QRecord read = new GetAction().execute(new GetInput(TABLE).withPrimaryKey(key)).getRecord();
         assertNotNull(read);
         assertEquals(key, read.getValue("id"));
         assertEquals(nativeRow.getString("name"), read.getValue("name"));
      }
      assertEquals(3, ids.stream().distinct().count());
      assertEquals(3L, collection().countDocuments());
      GetInput missing = new GetInput(TABLE).withPrimaryKey(id(99).toHexString());
      readOnly("missing generated identity", missing, () -> assertNull(new GetAction().execute(missing).getRecord()));
   }



   /*******************************************************************************
    ** Supplied STRING native identities retain the caller's value and owner.
    ** Reading a STRING id that also names an ObjectId has a separate policy.
    *******************************************************************************/
   @Test
   void testSuppliedStringKeyAndDuplicatePreserveStoredOwner() throws Exception
   {
      String key = id(90).toHexString();
      QRecord caller = new QRecord().withValue("id", key).withValue("name", "Manual owner");
      QRecord result = insert(caller);
      assertEquals(key, result.getValue("id"));
      assertEquals(key, caller.getValue("id"));
      Document nativeRow = collection().find(new Document("_id", key)).first();
      assertNotNull(nativeRow, "Native supplied STRING identity must remain addressable");
      assertEquals("Manual owner", nativeRow.getString("name"));
      List<String> before = collectionSnapshot(COLLECTION);
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TABLE).withSkipUniqueKeyCheck(true)
         .withRecord(new QRecord().withValue("id", key).withValue("name", "Cannot replace owner"))));
      assertEquals(before, collectionSnapshot(COLLECTION));
   }



   /*******************************************************************************
    ** Scalar storage and supported public values are checked independently; decimal decoding remains provider-specific.
    *******************************************************************************/
   @Test
   void testScalarTextPasswordAndDecimalTypesRoundTrip() throws Exception
   {
      BigDecimal decimal = new BigDecimal("123456789012345.12345");
      QRecord inserted = insert(new QRecord().withValue("name", "Scalar types").withValue("integerValue", 7).withValue("longValue", 9007199254740993L)
         .withValue("decimalValue", decimal).withValue("booleanValue", false).withValue("textValue", "Text\nsecond line")
         .withValue("htmlValue", "<p>Markup</p>").withValue("passwordValue", "sample-only-password"));
      String key = inserted.getValueString("id");
      Document nativeRow = nativeRow(key);
      assertAll(
         () -> assertEquals(7, assertInstanceOf(Integer.class, nativeRow.get("integer_value"))),
         () -> assertEquals(9007199254740993L, assertInstanceOf(Long.class, nativeRow.get("long_value"))),
         () -> assertEquals(0, decimal.compareTo(assertInstanceOf(Decimal128.class, nativeRow.get("decimal_value")).bigDecimalValue())),
         () -> assertEquals(false, assertInstanceOf(Boolean.class, nativeRow.get("boolean_value"))),
         () -> assertEquals("Text\nsecond line", nativeRow.getString("text_value")),
         () -> assertEquals("<p>Markup</p>", nativeRow.getString("html_value")),
         () -> assertEquals("sample-only-password", nativeRow.getString("password_value")));
      GetInput input = fullGet(key);
      readOnly("typed scalar Get", input, () ->
      {
         QRecord read = new GetAction().execute(input).getRecord();
         assertNotNull(read);
         assertAll(
            () -> assertEquals(7, assertInstanceOf(Integer.class, read.getValue("integerValue"))),
            () -> assertEquals(9007199254740993L, assertInstanceOf(Long.class, read.getValue("longValue"))),
            () -> assertEquals(false, assertInstanceOf(Boolean.class, read.getValue("booleanValue"))),
            () -> assertEquals(nativeRow.getString("text_value"), read.getValue("textValue")),
            () -> assertEquals(nativeRow.getString("html_value"), read.getValue("htmlValue")),
            () -> assertEquals(nativeRow.getString("password_value"), read.getValue("passwordValue")));
      });
      GetInput masked = new GetInput(TABLE).withPrimaryKey(key);
      readOnly("default password privacy", masked, () ->
      {
         QRecord read = new GetAction().execute(masked).getRecord();
         assertNotNull(read);
         assertEquals("************", read.getValue("passwordValue"));
         assertFalse(JsonUtils.toJson(read).contains("sample-only-password"));
      });
   }



   /*******************************************************************************
    ** Native temporal storage and public Instant values retain millisecond precision.
    ** Public civil-date and time conversion remains provider-specific.
    *******************************************************************************/
   @Test
   void testDateTimeAndInstantTypesRetainValuesAndTypes() throws Exception
   {
      LocalDate date = LocalDate.of(2025, 2, 3);
      LocalTime time = LocalTime.of(21, 34, 56, 123000000);
      Instant instant = Instant.parse("2025-02-03T21:34:56.123Z");
      QRecord inserted = insert(new QRecord().withValue("name", "Temporal types").withValue("dateValue", date).withValue("timeValue", time)
         .withValue("dateTimeValue", instant).withValue("fixedZoneDateTime", instant).withValue("recordZoneDateTime", instant)
         .withValue("timeZone", "America/Chicago").withValue("manualDateTime", instant));
      String key = inserted.getValueString("id");
      Document nativeRow = nativeRow(key);
      assertAll(
         () -> assertEquals(date.atStartOfDay().toInstant(ZoneOffset.UTC), assertInstanceOf(Date.class, nativeRow.get("date_value")).toInstant()),
         () -> assertEquals(time.atDate(LocalDate.ofEpochDay(0)).toInstant(ZoneOffset.UTC), assertInstanceOf(Date.class, nativeRow.get("time_value")).toInstant()),
         () -> assertEquals(instant, assertInstanceOf(Date.class, nativeRow.get("date_time_value")).toInstant()));
      GetInput input = fullGet(key);
      readOnly("typed temporal Get", input, () ->
      {
         QRecord read = new GetAction().execute(input).getRecord();
         assertNotNull(read);
         assertAll(
            () -> assertEquals(instant, assertInstanceOf(Instant.class, read.getValue("dateTimeValue"))),
            () -> assertEquals(instant, assertInstanceOf(Instant.class, read.getValue("fixedZoneDateTime"))),
            () -> assertEquals(instant, assertInstanceOf(Instant.class, read.getValue("recordZoneDateTime"))),
            () -> assertEquals(instant, assertInstanceOf(Instant.class, read.getValue("manualDateTime"))));
      });
   }



   /*******************************************************************************
    ** Sparse update and delete operate on exactly the selected native owners.
    *******************************************************************************/
   @Test
   void testSparseUpdateExplicitNullAndDeletePreserveOtherRows() throws Exception
   {
      seedRows();
      List<Document> before = nativeRows();
      LocalDate startDay = LocalDate.now();
      Instant start = Instant.now();
      QRecord update = new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", id(1).toHexString())
         .withValue("name", "Updated").withValue("textValue", null).withValue("booleanValue", false))).getRecords().get(0);
      successful(update);
      Instant end = Instant.now();
      LocalDate endDay = LocalDate.now();
      Document changed = nativeRow(id(1).toHexString());
      Document expected = new Document(before.get(0));
      expected.put("name", "Updated");
      expected.put("text_value", null);
      expected.put("boolean_value", false);
      Date modified = assertInstanceOf(Date.class, changed.get("modify_date"));
      assertFalse(modified.toInstant().isBefore(start.minusMillis(1)));
      assertFalse(modified.toInstant().isAfter(end.plusMillis(1)));
      expected.put("modify_date", modified);
      Date modifiedDay = assertInstanceOf(Date.class, changed.get("modified_day"));
      assertTrue(List.of(Date.from(startDay.atStartOfDay().toInstant(ZoneOffset.UTC)), Date.from(endDay.atStartOfDay().toInstant(ZoneOffset.UTC))).contains(modifiedDay),
         "Dynamic DATE defaults use the host calendar day, stored as native UTC midnight");
      expected.put("modified_day", modifiedDay);
      List<Document> expectedRows = new ArrayList<>(before);
      expectedRows.set(0, expected);
      assertEquals(expectedRows, nativeRows());
      DeleteInput deleteInput = new DeleteInput(TABLE).withPrimaryKeys(List.of(id(2).toHexString()));
      String deleteBefore = requestState(deleteInput);
      DeleteOutput deleted = new DeleteAction().execute(deleteInput);
      assertEquals(1, deleted.getDeletedRecordCount());
      assertTrue(deleted.getRecordsWithErrors().isEmpty());
      assertEquals(deleteBefore, requestState(deleteInput));
      expectedRows.remove(1);
      assertEquals(expectedRows, nativeRows());
   }



   /*******************************************************************************
    ** Canonical normalization/defaults and per-record required validation remain.
    *******************************************************************************/
   @Test
   void testCanonicalNormalizationAndRequiredMixedBatch() throws Exception
   {
      List<QRecord> output = new InsertAction().execute(new InsertInput(TABLE).withRecords(List.of(
         new QRecord().withValue("name", "Normalized").withValue("normalizedKey", "  mixed  ").withValue("upperValue", "up")
            .withValue("trimValue", "  value  ").withValue("truncateValue", "123456789").withValue("booleanValue", false),
         new QRecord().withValue("name", " \t "),
         new QRecord().withValue("name", "Valid after rejection")))).getRecords();
      assertEquals(3, output.size());
      successful(output.get(0));
      assertFalse(output.get(1).getErrors().isEmpty());
      successful(output.get(2));
      assertEquals(2L, collection().countDocuments());
      Document first = nativeRow(output.get(0).getValueString("id"));
      assertAll(
         () -> assertEquals("MIXED", first.getString("normalized_key")),
         () -> assertEquals("UP", first.getString("upper_value")),
         () -> assertEquals("value", first.getString("trim_value")),
         () -> assertEquals("12345678", first.getString("truncate_value")),
         () -> assertEquals(false, first.getBoolean("boolean_value")),
         () -> assertEquals(true, nativeRow(output.get(2).getValueString("id")).getBoolean("boolean_value")),
         () -> assertEquals(List.of("Normalized", "Valid after rejection"), collection().find().sort(new Document("name", 1)).map(row -> row.getString("name")).into(new ArrayList<>())));
   }



   /*******************************************************************************
    ** Native sort/window and independent unpaged Count use the same base filter.
    *******************************************************************************/
   @Test
   void testSortedPagedQueryAndCountMatchNativeRows() throws Exception
   {
      seedRows();
      Document nativeFilter = new Document("long_value", new Document("$gt", 10L));
      List<Document> expected = collection().find(nativeFilter).sort(new Document("long_value", -1)).skip(1).limit(2).into(new ArrayList<>());
      assertEquals(List.of(id(3), id(2)), expected.stream().map(row -> row.getObjectId("_id")).toList());
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         QueryInput query = new QueryInput(TABLE).withInputSource(source).withFieldNamesToInclude(Set.of("id", "name", "longValue"))
            .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("longValue", QCriteriaOperator.GREATER_THAN, 10L))
               .withOrderBy(new QFilterOrderBy("longValue", false)).withSkip(1).withLimit(2));
         readOnly("native sorted page / " + source, query, () -> assertQueryRows(expected, new QueryAction().execute(query).getRecords()));
         CountInput count = countInput(source, new QQueryFilter().withCriteria(new QFilterCriteria("longValue", QCriteriaOperator.GREATER_THAN, 10L)));
         readOnly("native Count / " + source, count, () -> assertEquals(collection().countDocuments(nativeFilter), (long) new CountAction().execute(count).getCount()));
      }
   }



   /*******************************************************************************
    ** Native COUNT, SUM, MIN, MAX and group rows are compared independently.
    *******************************************************************************/
   @Test
   void testAggregateOperatorsAndGroupsMatchNativeBson() throws Exception
   {
      seedRows();
      Document nativeScalar = collection().aggregate(List.of(new Document("$group", new Document("_id", null)
         .append("count", new Document("$sum", 1)).append("sum", new Document("$sum", "$long_value"))
         .append("min", new Document("$min", "$long_value")).append("max", new Document("$max", "$long_value"))))).first();
      assertNotNull(nativeScalar);
      assertEquals(4, nativeScalar.getInteger("count"));
      assertEquals(1130L, nativeScalar.getLong("sum"));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         AggregateInput scalar = aggregateInput(source, new QQueryFilter(), new Aggregate("integerValue", AggregateOperator.COUNT),
            new Aggregate("longValue", AggregateOperator.SUM), new Aggregate("longValue", AggregateOperator.MIN), new Aggregate("longValue", AggregateOperator.MAX));
         checks.add(() -> readOnly("native aggregate scalar / " + source, scalar, () ->
         {
            AggregateResult row = onlyAggregate(scalar);
            assertEquals(4, assertInstanceOf(Integer.class, row.getAggregateValue(scalar.getAggregates().get(0))));
            assertEquals(nativeScalar.getLong("sum"), assertInstanceOf(Long.class, row.getAggregateValue(scalar.getAggregates().get(1))));
            assertEquals(nativeScalar.getLong("min"), assertInstanceOf(Long.class, row.getAggregateValue(scalar.getAggregates().get(2))));
            assertEquals(nativeScalar.getLong("max"), assertInstanceOf(Long.class, row.getAggregateValue(scalar.getAggregates().get(3))));
         }));
         GroupBy group = new GroupBy(QFieldType.BOOLEAN, "booleanValue");
         AggregateInput grouped = aggregateInput(source, new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(group, true)),
            new Aggregate("integerValue", AggregateOperator.COUNT), new Aggregate("longValue", AggregateOperator.SUM)).withGroupBy(group);
         List<Document> expected = collection().aggregate(List.of(new Document("$group", new Document("_id", "$boolean_value")
            .append("count", new Document("$sum", 1)).append("sum", new Document("$sum", "$long_value"))), new Document("$sort", new Document("_id", 1)))).into(new ArrayList<>());
         checks.add(() -> readOnly("native grouped aggregate / " + source, grouped, () ->
         {
            List<AggregateResult> rows = new AggregateAction().execute(grouped).getResults();
            assertEquals(expected.size(), rows.size());
            for(Integer index = 0; index < rows.size(); index++)
            {
               AggregateResult row = rows.get(index);
               assertEquals(expected.get(index).getBoolean("_id"), assertInstanceOf(Boolean.class, row.getGroupByValue(group)));
               assertEquals(expected.get(index).getInteger("count"), assertInstanceOf(Integer.class, row.getAggregateValue(grouped.getAggregates().get(0))));
               assertEquals(expected.get(index).getLong("sum"), assertInstanceOf(Long.class, row.getAggregateValue(grouped.getAggregates().get(1))));
            }
         }));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** USER-only READ metadata applies to virtual/inline predicates. SYSTEM retains
    ** the canonical unrestricted table; independent BSON also includes the owner.
    *******************************************************************************/
   @Test
   void testComputedPredicatesAndPersonalizedReadUseOriginalOwners() throws Exception
   {
      seedRows();
      instance.addSecurityKeyType(new QSecurityKeyType().withName(OWNER_KEY));
      session.withSecurityKeyValue(OWNER_KEY, 10L).withSecurityKeyValue(OWNER_KEY, 100L);
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(UserOwnerPolicy.class));
      table().withVirtualField(new QVirtualFieldMetaData(LENGTH, QFieldType.INTEGER).withIsQueryCriteria(true).withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction().withFieldName("name").withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         Document owner = source == QInputSource.USER ? new Document("long_value", new Document("$in", List.of(10L, 100L))) : new Document();
         for(Boolean nested : List.of(false, true))
         {
            for(Boolean virtual : List.of(false, true))
            {
               QFilterCriteria criterion = virtual ? new QFilterCriteria(LENGTH, QCriteriaOperator.GREATER_THAN, 4)
                  : new QFilterCriteria("name", QCriteriaOperator.GREATER_THAN, 4).withFieldFunction(new FieldFunction().withFieldName("name").withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER));
               QQueryFilter filter = new QQueryFilter().withCriteria(criterion);
               if(nested)
               {
                  filter = new QQueryFilter().withSubFilter(filter);
               }
               QQueryFilter request = filter;
               Document nativeFilter = new Document("$and", List.of(owner,
                  new Document("$expr", new Document("$gt", List.of(new Document("$strLenCP", "$name"), 4)))));
               checks.add(() -> assertThreeReads("computed READ / " + source + " / " + nested + " / " + virtual, source, request, nativeFilter));
            }
         }
         checks.add(() -> assertThreeReads("physical READ positive / " + source, source, new QQueryFilter(), owner));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Existing Mongo limitations fail explicitly instead of publishing a result
    ** from an ignored join, unsupported distinct operator or lost composition.
    *******************************************************************************/
   @Test
   void testUnsupportedDistinctAndCompositionRefuse() throws Exception
   {
      seedRows();
      String initial = "sampleNameInitial";
      table().withVirtualField(new QVirtualFieldMetaData(initial, QFieldType.STRING).withIsQuerySelectable(true).withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction().withFieldName("name").withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
            .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1, SubStringFunction.LENGTH_PARAM, 1))));
      QJoinMetaData join = new QJoinMetaData().withName("fieldLabSelfProbe").withLeftTable(TABLE).withRightTable(TABLE).withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("normalizedKey", "name"));
      instance.addJoin(join);
      assertEquals(0, collection().aggregate(List.of(new Document("$lookup", new Document("from", COLLECTION).append("localField", "normalized_key")
         .append("foreignField", "name").append("as", "peer")), new Document("$unwind", "$peer"))).into(new ArrayList<>()).size());
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         AggregateInput distinct = aggregateInput(source, new QQueryFilter(), new Aggregate("name", AggregateOperator.COUNT_DISTINCT));
         checks.add(() -> readOnly("unsupported count distinct / " + source, distinct, () -> refused("distinct", () -> new AggregateAction().execute(distinct))));
         QQueryFilter composed = new QQueryFilter().withCriteria(new QFilterCriteria(initial, QCriteriaOperator.EQUALS, 1)
            .withFieldFunction(new FieldFunction().withFieldName(initial).withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)));
         QueryInput query = new QueryInput(TABLE).withInputSource(source).withFilter(composed.clone());
         CountInput count = countInput(source, composed.clone());
         AggregateInput aggregate = aggregateInput(source, composed.clone(), new Aggregate("integerValue", AggregateOperator.COUNT));
         checks.add(() -> readOnly("unsupported inline virtual Query / " + source, query, () -> refused("inline", () -> new QueryAction().execute(query))));
         checks.add(() -> readOnly("unsupported inline virtual Count / " + source, count, () -> refused("inline", () -> new CountAction().execute(count))));
         checks.add(() -> readOnly("unsupported inline virtual Aggregate / " + source, aggregate, () -> refused("inline", () -> new AggregateAction().execute(aggregate))));
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** A name-filtered String key must not become an ObjectId owner with the same
    ** public hex spelling. Refusal must occur before callbacks or native writes.
    *******************************************************************************/
   @Test
   void testNameFilteredDeleteNeverRetargetsStringKeyToObjectIdDecoy() throws Exception
   {
      table().withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CaptureDeleteOwners.class));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         for(Boolean ambiguous : List.of(false, true))
         {
            checks.add(() ->
            {
               collection().deleteMany(new Document());
               seedRows();
               String sharedHex = id(1).toHexString();
               Document stringOwner = new Document("_id", sharedHex).append("name", "String owner").append("integer_value", 5)
                  .append("long_value", 5000L).append("normalized_key", "STRING OWNER").append("source", new Document("marker", "string owner"));
               collection().insertOne(stringOwner);
               assertEquals(stringOwner, collection().find(new Document("_id", sharedHex)).first());
               assertEquals("Alpha", collection().find(new Document("_id", id(1))).first().getString("name"));
               assertEquals(1L, collection().countDocuments(new Document("_id", new Document("$type", "string"))));
               assertEquals(4L, collection().countDocuments(new Document("_id", new Document("$type", "objectId"))));
               String targetName = ambiguous ? "String owner" : "Bravo";
               Document target = collection().find(new Document("name", targetName)).first();
               assertNotNull(target);
               assertEquals(ambiguous ? sharedHex : id(2), target.get("_id"));
               assertEquals(1L, collection().countDocuments(new Document("name", targetName)));
               List<Document> beforeRows = nativeRows();
               List<Document> expectedDeleted = beforeRows.stream().filter(row -> !row.get("_id").equals(target.get("_id"))).toList();
               QQueryFilter filter = new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, targetName));
               DeleteInput input = new DeleteInput(TABLE).withInputSource(source).withQueryFilter(filter);
               String inputBefore = requestState(input);
               String filterBefore = JsonUtils.toJson(filter);
               String metadataBefore = JsonUtils.toJson(instance.getTables());
               String sessionBefore = JsonUtils.toJson(session);
               CaptureDeleteOwners.received.set(null);
               try
               {
                  DeleteOutput output = null;
                  QException failure = null;
                  try
                  {
                     output = new DeleteAction().execute(input);
                  }
                  catch(QException e)
                  {
                     failure = e;
                  }
                  DeleteOutput returned = output;
                  QException refusal = failure;
                  assertAll("native Delete identity / ambiguous " + ambiguous + " / " + source,
                     () ->
                     {
                        if(refusal != null)
                        {
                           assertTrue(ambiguous, "Disjoint ObjectId selection must succeed");
                           Boolean identified = false;
                           for(Throwable cause = refusal; cause != null; cause = cause.getCause())
                           {
                              assertFalse(cause instanceof NullPointerException || cause instanceof IllegalArgumentException || cause instanceof MongoException,
                                 "Identity refusal must not be an unchecked or native failure");
                              String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
                              if(message.contains("key") || message.contains("identity") || message.contains("identifier") || message.contains("_id"))
                              {
                                 identified = true;
                              }
                           }
                           assertTrue(identified, "Refusal must identify the unsupported identity");
                        }
                        else
                        {
                           assertNotNull(returned);
                           assertEquals(1, returned.getDeletedRecordCount());
                           assertTrue(returned.getRecordsWithErrors().isEmpty());
                        }
                     },
                     () -> assertEquals(refusal == null ? expectedDeleted : beforeRows, nativeRows()),
                     () ->
                     {
                        if(refusal != null)
                        {
                           assertNull(CaptureDeleteOwners.received.get(), "Identity refusal must precede PRE_DELETE");
                        }
                        else
                        {
                           assertNotNull(CaptureDeleteOwners.received.get());
                           assertEquals(List.of(targetName), CaptureDeleteOwners.received.get().stream().map(record -> record.getValueString("name")).toList());
                        }
                     },
                     () -> assertTrue(inputBefore.equals(requestState(input)), "Delete input must retain caller selection"),
                     () -> assertSame(filter, input.getQueryFilter()),
                     () -> assertTrue(filterBefore.equals(JsonUtils.toJson(filter)), "Caller name filter must remain unchanged"),
                     () -> assertTrue(metadataBefore.equals(JsonUtils.toJson(instance.getTables())), "Registered metadata must remain unchanged"),
                     () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"),
                     () -> assertSame(session, QContext.getQSession()));
               }
               finally
               {
                  CaptureDeleteOwners.received.set(null);
               }
            });
         }
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** The configured logical key maps to native _id. An ordinary editable id
    ** column is data, even when its value spells a different native owner's key.
    *******************************************************************************/
   @Test
   void testRenamedPrimaryKeyUpdateIgnoresOrdinaryIdAsIdentity() throws Exception
   {
      Map<String, QTableMetaData> originalTables = instance.getTables();
      QTableMetaData originalTable = table();
      String originalMetadata = JsonUtils.toJson(originalTables);
      QTableMetaData renamed = originalTable.clone();
      QFieldMetaData ordinaryId = renamed.getField("id").clone();
      ordinaryId.setBackendName("ordinary_id");
      ordinaryId.setIsEditable(true);
      QFieldMetaData primaryKey = renamed.getField("id");
      primaryKey.setName("recordKey");
      Map<String, QFieldMetaData> fields = new LinkedHashMap<>();
      for(Map.Entry<String, QFieldMetaData> entry : renamed.getFields().entrySet())
      {
         fields.put(entry.getKey().equals("id") ? "recordKey" : entry.getKey(), entry.getValue());
      }
      fields.put("id", ordinaryId);
      renamed.setFields(fields);
      renamed.setPrimaryKeyField("recordKey");
      for(QFieldSection section : renamed.getSections())
      {
         section.setFieldNames(section.getFieldNames().stream().map(name -> name.equals("id") ? "recordKey" : name).toList());
      }
      renamed.getSections().get(0).setFieldNames(new ArrayList<>(renamed.getSections().get(0).getFieldNames()));
      renamed.getSections().get(0).getFieldNames().add("id");
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(originalTables);
      tables.put(TABLE, renamed);
      instance.setTables(tables);
      try
      {
         new QInstanceValidator().revalidate(instance);
         assertEquals("_id", table().getField("recordKey").getBackendName());
         assertEquals("ordinary_id", table().getField("id").getBackendName());
         assertEquals(QFieldType.STRING, table().getField("recordKey").getType());
         assertEquals(QFieldType.STRING, table().getField("id").getType());
         List<Executable> checks = new ArrayList<>();
         for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
         {
            for(Boolean supplyOrdinaryId : List.of(false, true))
            {
               checks.add(() ->
               {
                  collection().deleteMany(new Document());
                  seedRows();
                  for(Integer index = 1; index <= 4; index++)
                  {
                     assertEquals(1L, collection().updateOne(new Document("_id", id(index)),
                        new Document("$set", new Document("ordinary_id", id(index + 10).toHexString()))).getModifiedCount());
                  }
                  assertEquals(List.of(id(11).toHexString(), id(12).toHexString(), id(13).toHexString(), id(14).toHexString()),
                     nativeRows().stream().map(row -> row.getString("ordinary_id")).toList());
                  List<Document> expected = new ArrayList<>(nativeRows());
                  Document changed = new Document(expected.get(0));
                  changed.put("name", "Configured key target");
                  QRecord patch = new QRecord().withValue("recordKey", id(1).toHexString()).withValue("name", "Configured key target");
                  if(supplyOrdinaryId)
                  {
                     patch.withValue("id", id(2).toHexString());
                     changed.put("ordinary_id", id(2).toHexString());
                  }
                  expected.set(0, changed);
                  UpdateInput input = new UpdateInput(TABLE).withInputSource(source).withOmitModifyDateUpdate(true).withRecord(patch);
                  String inputBefore = requestState(input);
                  String recordBefore = JsonUtils.toJson(patch);
                  String metadataBefore = JsonUtils.toJson(instance.getTables());
                  String sessionBefore = JsonUtils.toJson(session);
                  assertAll("native renamed primary key / ordinary id supplied " + supplyOrdinaryId + " / " + source,
                     () ->
                     {
                        List<QRecord> output = new UpdateAction().execute(input).getRecords();
                        assertEquals(1, output.size());
                        successful(output.get(0));
                        assertEquals(id(1).toHexString(), output.get(0).getValue("recordKey"));
                        assertEquals(supplyOrdinaryId, output.get(0).getValues().containsKey("id"));
                        if(supplyOrdinaryId)
                        {
                           assertEquals(id(2).toHexString(), output.get(0).getValue("id"));
                        }
                     },
                     () -> assertEquals(expected, nativeRows()),
                     () -> assertTrue(inputBefore.equals(requestState(input)), "Update input must retain caller keys and values"),
                     () -> assertTrue(recordBefore.equals(JsonUtils.toJson(patch)), "Caller patch must remain unchanged"),
                     () -> assertTrue(metadataBefore.equals(JsonUtils.toJson(instance.getTables())), "Renamed metadata must remain unchanged"),
                     () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"),
                     () -> assertSame(session, QContext.getQSession()));
               });
            }
         }
         assertAll(checks);
      }
      finally
      {
         instance.setTables(originalTables);
         assertSame(originalTable, table());
         assertTrue(originalMetadata.equals(JsonUtils.toJson(originalTables)), "Original registered tables must remain unchanged");
      }
   }



   /*******************************************************************************
    ** Ordinary Query can project a virtual _id value. DML must reject that
    ** native-key overwrite before selecting owners or invoking PRE_DELETE.
    *******************************************************************************/
   @Test
   void testVirtualNativeIdShadowRefusesDeleteBeforeCallbacks() throws Exception
   {
      table().withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CaptureDeleteOwners.class));
      table().withVirtualField(new QVirtualFieldMetaData("_id", QFieldType.STRING).withIsQuerySelectable(true).withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction().withFieldName("textValue").withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
            .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1, SubStringFunction.LENGTH_PARAM, 24))));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() ->
         {
            collection().deleteMany(new Document());
            seedRows();
            for(Integer index = 1; index <= 4; index++)
            {
               assertEquals(1L, collection().updateOne(new Document("_id", id(index)), new Document("$set",
                  new Document("text_value", id(index == 1 ? 2 : index).toHexString()))).getModifiedCount());
            }
            assertEquals(List.of(id(2).toHexString(), id(2).toHexString(), id(3).toHexString(), id(4).toHexString()),
               nativeRows().stream().map(row -> row.getString("text_value")).toList());
            assertEquals(4L, collection().countDocuments(new Document("_id", new Document("$type", "objectId"))));
            List<Document> projected = collection().aggregate(List.of(new Document("$match", new Document("name", "Alpha")),
               new Document("$addFields", new Document("_id", new Document("$substrCP", List.of("$text_value", 0, 24)))),
               new Document("$project", new Document("name", 1).append("_id", 1)))).into(new ArrayList<>());
            assertEquals(List.of(new Document("_id", id(2).toHexString()).append("name", "Alpha")), projected);
            QQueryFilter nameFilter = new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Alpha"));
            QueryInput query = new QueryInput(TABLE).withInputSource(source).withFieldNamesToInclude(Set.of("name")).withFilter(nameFilter.clone());
            DeleteInput delete = new DeleteInput(TABLE).withInputSource(source).withQueryFilter(nameFilter);
            CaptureDeleteOwners.received.set(null);
            try
            {
               assertAll("native virtual _id projection boundary / " + source,
                  () -> readOnly("ordinary computed projection", query, () ->
                  {
                     List<QRecord> rows = new QueryAction().execute(query).getRecords();
                     assertEquals(1, rows.size());
                     assertEquals(Set.of("name", "_id"), rows.get(0).getValues().keySet());
                     assertEquals(projected.get(0).getString("name"), assertInstanceOf(String.class, rows.get(0).getValue("name")));
                     assertEquals(projected.get(0).getString("_id"), assertInstanceOf(String.class, rows.get(0).getValue("_id")));
                  }),
                  () -> readOnly("DML refuses virtual native-key overwrite", delete, () -> refused("key", () -> new DeleteAction().execute(delete))),
                  () -> assertSame(nameFilter, delete.getQueryFilter()),
                  () -> assertNull(CaptureDeleteOwners.received.get(), "Virtual native-key overwrite must be refused before PRE_DELETE"));
            }
            finally
            {
               CaptureDeleteOwners.received.set(null);
            }
         });
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** A native unstructured id must not replace the configured key decoded from
    ** _id during DML prefetch. Explicit public projection excludes that extra key.
    *******************************************************************************/
   @Test
   void testUnstructuredLogicalIdShadowRefusesDeleteBeforeCallbacks() throws Exception
   {
      table().withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CaptureDeleteOwners.class));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         checks.add(() ->
         {
            collection().deleteMany(new Document());
            seedRows();
            assertEquals(1L, collection().updateOne(new Document("_id", id(1)),
               new Document("$set", new Document("id", id(2).toHexString()))).getModifiedCount());
            Document nativeOwner = collection().find(new Document("name", "Alpha")).first();
            assertNotNull(nativeOwner);
            assertEquals(id(1), assertInstanceOf(ObjectId.class, nativeOwner.get("_id")));
            assertEquals(id(2).toHexString(), assertInstanceOf(String.class, nativeOwner.get("id")));
            assertEquals(4L, collection().countDocuments(new Document("_id", new Document("$type", "objectId"))));
            Document projected = collection().find(new Document("name", "Alpha")).projection(new Document("_id", 1).append("name", 1)).first();
            assertEquals(new Document("_id", id(1)).append("name", "Alpha"), projected);
            QQueryFilter nameFilter = new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Alpha"));
            QueryInput query = new QueryInput(TABLE).withInputSource(source).withFieldNamesToInclude(Set.of("id", "name")).withFilter(nameFilter.clone());
            DeleteInput delete = new DeleteInput(TABLE).withInputSource(source).withQueryFilter(nameFilter);
            CaptureDeleteOwners.received.set(null);
            try
            {
               assertAll("native unstructured logical-key boundary / " + source,
                  () -> readOnly("ordinary explicit configured-key projection", query, () ->
                  {
                     List<QRecord> rows = new QueryAction().execute(query).getRecords();
                     assertEquals(1, rows.size());
                     assertEquals(Set.of("id", "name"), rows.get(0).getValues().keySet());
                     assertEquals(projected.getObjectId("_id").toHexString(), assertInstanceOf(String.class, rows.get(0).getValue("id")));
                     assertEquals(projected.getString("name"), assertInstanceOf(String.class, rows.get(0).getValue("name")));
                  }),
                  () -> readOnly("DML refuses decoded logical-key overwrite", delete, () -> refused("key", () -> new DeleteAction().execute(delete))),
                  () -> assertSame(nameFilter, delete.getQueryFilter()),
                  () -> assertNull(CaptureDeleteOwners.received.get(), "Decoded key overwrite must be refused before PRE_DELETE"));
            }
            finally
            {
               CaptureDeleteOwners.received.set(null);
            }
         });
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** Replace must preserve the native identity found by its configured unique
    ** key before handing any discovered key to Update or a PRE_UPDATE callback.
    *******************************************************************************/
   @Test
   void testReplaceUniqueMatchRefusesStringObjectIdCollisionBeforeUpdate() throws Exception
   {
      assertTrue(table().getUniqueKeys().stream().anyMatch(key -> key.getFieldNames().equals(List.of("normalizedKey"))));
      table().withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(CaptureReplaceOwners.class));
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         for(Boolean ambiguous : List.of(false, true))
         {
            checks.add(() ->
            {
               collection().deleteMany(new Document());
               seedRows();
               Document stringOwner = new Document("_id", id(1).toHexString()).append("name", "String owner").append("integer_value", 5)
                  .append("normalized_key", "STRINGOWNER").append("text_value", "Original String payload").append("long_value", 5000L);
               collection().insertOne(stringOwner);
               assertEquals(stringOwner, collection().find(new Document("_id", id(1).toHexString())).first());
               assertEquals("OWNER1", collection().find(new Document("_id", id(1))).first().getString("normalized_key"));
               String matchingKey = ambiguous ? "STRINGOWNER" : "OWNER2";
               List<Document> nativeMatches = collection().find(new Document("normalized_key", matchingKey)).into(new ArrayList<>());
               assertEquals(1, nativeMatches.size());
               assertEquals(ambiguous ? id(1).toHexString() : id(2), nativeMatches.get(0).get("_id"));
               List<Document> beforeRows = nativeRows();
               QRecord patch = new QRecord().withValue("normalizedKey", matchingKey).withValue("textValue", "Matched owner update");
               List<QRecord> callerRecords = List.of(patch);
               UniqueKey key = new UniqueKey("normalizedKey");
               ReplaceInput input = new ReplaceInput().withKey(key).withRecords(callerRecords).withPerformDeletes(false);
               input.setTableName(TABLE);
               input.setInputSource(source);
               String metadataBefore = JsonUtils.toJson(instance.getTables());
               String sessionBefore = JsonUtils.toJson(session);
               String keyBefore = JsonUtils.toJson(key);
               CaptureReplaceOwners.received.set(null);
               try
               {
                  if(ambiguous)
                  {
                     assertAll("native Replace ambiguous unique-match / " + source,
                        () -> readOnly("Replace matching refuses native String identity", input, () -> refused("key", () -> new ReplaceAction().execute(input))),
                        () -> assertNull(CaptureReplaceOwners.received.get(), "Ambiguous native match must be refused before PRE_UPDATE"),
                        () -> assertFalse(patch.getValues().containsKey("id"), "Refused native match must not publish a discovered key"));
                  }
                  else
                  {
                     LocalDate startDay = LocalDate.now();
                     Instant start = Instant.now();
                     assertAll("native Replace disjoint ObjectId unique-match / " + source,
                        () ->
                        {
                           ReplaceOutput output = new ReplaceAction().execute(input);
                           Instant end = Instant.now();
                           LocalDate endDay = LocalDate.now();
                           assertTrue(output.getInsertOutput().getRecords().isEmpty());
                           assertEquals(1, output.getUpdateOutput().getRecords().size());
                           successful(output.getUpdateOutput().getRecords().get(0));
                           assertEquals(id(2).toHexString(), output.getUpdateOutput().getRecords().get(0).getValue("id"));
                           Document changed = collection().find(new Document("_id", id(2))).first();
                           assertNotNull(changed);
                           Document expected = new Document(nativeMatches.get(0));
                           expected.put("text_value", "Matched owner update");
                           Date modified = assertInstanceOf(Date.class, changed.get("modify_date"));
                           assertFalse(modified.toInstant().isBefore(start.minusMillis(1)));
                           assertFalse(modified.toInstant().isAfter(end.plusMillis(1)));
                           expected.put("modify_date", modified);
                           Date modifiedDay = assertInstanceOf(Date.class, changed.get("modified_day"));
                           assertTrue(List.of(Date.from(startDay.atStartOfDay().toInstant(ZoneOffset.UTC)), Date.from(endDay.atStartOfDay().toInstant(ZoneOffset.UTC))).contains(modifiedDay));
                           expected.put("modified_day", modifiedDay);
                           List<Document> expectedRows = beforeRows.stream().map(row -> row.get("_id").equals(id(2)) ? expected : row).toList();
                           assertEquals(expectedRows, nativeRows());
                           assertEquals(id(2).toHexString(), patch.getValue("id"));
                        },
                        () ->
                        {
                           assertNotNull(CaptureReplaceOwners.received.get());
                           assertEquals(List.of("Bravo"), CaptureReplaceOwners.received.get().stream().map(record -> record.getValueString("name")).toList());
                           assertEquals(List.of(id(2).toHexString()), CaptureReplaceOwners.received.get().stream().map(record -> record.getValueString("id")).toList());
                        },
                        () -> assertSame(callerRecords, input.getRecords()),
                        () -> assertSame(patch, input.getRecords().get(0)),
                        () -> assertSame(key, input.getKey()),
                        () -> assertTrue(keyBefore.equals(JsonUtils.toJson(key)), "Matching descriptor must remain unchanged"),
                        () -> assertEquals(matchingKey, patch.getValue("normalizedKey")),
                        () -> assertEquals("Matched owner update", patch.getValue("textValue")),
                        () -> assertFalse(input.getPerformDeletes()),
                        () -> assertNull(input.getTransaction()),
                        () -> assertEquals(source, input.getInputSource()),
                        () -> assertTrue(metadataBefore.equals(JsonUtils.toJson(instance.getTables())), "Registered metadata must remain unchanged"),
                        () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"),
                        () -> assertSame(session, QContext.getQSession()));
                  }
               }
               finally
               {
                  CaptureReplaceOwners.received.set(null);
               }
            });
         }
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** The source tables, field order and all non-id field metadata remain canonical.
    *******************************************************************************/
   private void assertCanonicalFields()
   {
      assertTrue(canonicalBefore.equals(JsonUtils.toJson(canonical)), "Original canonical Field Lab metadata must remain unchanged");
      assertEquals(new ArrayList<>(canonical.getFields().keySet()), table().getFields().keySet().stream().filter(name -> !name.equals("integerValue")).toList());
      for(String field : canonical.getFields().keySet())
      {
         if(!field.equals("id"))
         {
            assertTrue(JsonUtils.toJson(canonical.getField(field)).equals(JsonUtils.toJson(table().getField(field))), "Canonical metadata changed for " + field);
         }
      }
      assertEquals(QFieldType.STRING, table().getField("id").getType());
      assertEquals("_id", table().getField("id").getBackendName());
      assertEquals(QFieldType.INTEGER, table().getField("integerValue").getType());
   }



   /*******************************************************************************
    ** Four independently inserted BSON owners distinguish order, predicates,
    ** averages and READ restrictions. No framework insert is used for these seeds.
    *******************************************************************************/
   private void seedRows()
   {
      List<String> names = List.of("Alpha", "Bravo", "Li", "Charlie");
      List<Long> amounts = List.of(10L, 20L, 100L, 1000L);
      List<Document> documents = new ArrayList<>();
      for(Integer index = 0; index < names.size(); index++)
      {
         documents.add(new Document("_id", id(index + 1)).append("name", names.get(index)).append("integer_value", index + 1)
            .append("long_value", amounts.get(index)).append("boolean_value", index % 2 == 0).append("normalized_key", "OWNER" + (index + 1))
            .append("text_value", "Text " + (index + 1)).append("source", new Document("marker", "native-" + index)).append("f0", "original-" + index));
      }
      collection().insertMany(documents);
      assertEquals(documents, nativeRows());
   }



   /*******************************************************************************
    ** Native count, aggregate and exact public projection have independent oracles.
    *******************************************************************************/
   private void assertThreeReads(String label, QInputSource source, QQueryFilter filter, Document nativeFilter) throws Exception
   {
      List<Document> expected = collection().find(nativeFilter).sort(new Document("_id", 1)).into(new ArrayList<>());
      Long expectedCount = collection().countDocuments(nativeFilter);
      Document sums = collection().aggregate(List.of(new Document("$match", nativeFilter), new Document("$group", new Document("_id", null)
         .append("count", new Document("$sum", 1)).append("sum", new Document("$sum", "$long_value"))))).first();
      assertNotNull(sums);
      assertEquals(expected.size(), expectedCount.intValue());
      assertEquals(expected.size(), sums.getInteger("count"));
      QueryInput query = new QueryInput(TABLE).withInputSource(source).withFieldNamesToInclude(Set.of("id", "name", "longValue"))
         .withFilter(filter.clone().withOrderBy(new QFilterOrderBy("id")));
      CountInput count = countInput(source, filter.clone());
      AggregateInput aggregate = aggregateInput(source, filter.clone(), new Aggregate("integerValue", AggregateOperator.COUNT), new Aggregate("longValue", AggregateOperator.SUM));
      assertAll(label,
         () -> readOnly("Query", query, () -> assertQueryRows(expected, new QueryAction().execute(query).getRecords())),
         () -> readOnly("Count", count, () -> assertEquals(expectedCount.longValue(), (long) new CountAction().execute(count).getCount())),
         () -> readOnly("Aggregate", aggregate, () ->
         {
            AggregateResult result = onlyAggregate(aggregate);
            assertEquals(sums.getInteger("count"), assertInstanceOf(Integer.class, result.getAggregateValue(aggregate.getAggregates().get(0))));
            assertEquals(sums.getLong("sum"), assertInstanceOf(Long.class, result.getAggregateValue(aggregate.getAggregates().get(1))));
         }));
   }



   /*******************************************************************************
    ** Existing Mongo selectable virtuals are included by its native projection.
    *******************************************************************************/
   private void assertQueryRows(List<Document> expected, List<QRecord> records)
   {
      assertEquals(expected.size(), records.size());
      for(Integer index = 0; index < records.size(); index++)
      {
         QRecord record = records.get(index);
         Set<String> keys = table().getVirtualField(LENGTH) == null ? Set.of("id", "name", "longValue") : Set.of("id", "name", "longValue", LENGTH);
         assertEquals(keys, record.getValues().keySet());
         assertEquals(expected.get(index).getObjectId("_id").toHexString(), assertInstanceOf(String.class, record.getValue("id")));
         assertEquals(expected.get(index).getString("name"), record.getValue("name"));
         assertEquals(expected.get(index).getLong("long_value"), assertInstanceOf(Long.class, record.getValue("longValue")));
         if(table().getVirtualField(LENGTH) != null)
         {
            assertEquals(expected.get(index).getString("name").length(), assertInstanceOf(Integer.class, record.getValue(LENGTH)));
         }
      }
   }



   /*******************************************************************************
    ** Active metadata aliases may legitimately change on USER personalization.
    ** Every other serialized input property and all stored BSON are preserved.
    *******************************************************************************/
   private void readOnly(String label, AbstractTableActionInput input, Executable action) throws Exception
   {
      String inputBefore = requestState(input);
      String metadataBefore = JsonUtils.toJson(List.of(instance.getTables(), instance.getJoins(), instance.getSecurityKeyTypes(), instance.getSupplementalCustomizers()));
      String sessionBefore = JsonUtils.toJson(session);
      Map<String, List<String>> storedBefore = snapshot();
      assertAll(label,
         action,
         () -> assertTrue(inputBefore.equals(requestState(input)), "Read input must remain unchanged except its active table metadata"),
         () -> assertTrue(metadataBefore.equals(JsonUtils.toJson(List.of(instance.getTables(), instance.getJoins(), instance.getSecurityKeyTypes(), instance.getSupplementalCustomizers()))), "Registered metadata must remain unchanged"),
         () -> assertEquals(storedBefore, snapshot()),
         () -> assertSame(session, QContext.getQSession()),
         () -> assertTrue(sessionBefore.equals(JsonUtils.toJson(session)), "Session must remain unchanged"),
         () -> assertSame(input.getTable(), input.getTableMetaData()),
         () -> assertEquals(TABLE, input.getTableName()));
   }



   /*******************************************************************************
    ** The private metadata object has a dedicated preservation oracle above.
    *******************************************************************************/
   private String requestState(AbstractTableActionInput input) throws Exception
   {
      Map<?, ?> properties = JsonUtils.toObject(JsonUtils.toJson(input), Map.class);
      properties.remove("table");
      properties.remove("tableMetaData");
      return JsonUtils.toJson(properties);
   }



   /*******************************************************************************
    ** Unsupported-operation errors must not be an invalid native request or NPE.
    *******************************************************************************/
   private void refused(String reason, Executable operation)
   {
      QException failure = assertThrows(QException.class, operation);
      Boolean found = false;
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         assertFalse(cause instanceof NullPointerException || cause instanceof IllegalArgumentException || cause instanceof MongoException);
         if(cause.getMessage() != null && cause.getMessage().toLowerCase().contains(reason))
         {
            found = true;
         }
      }
      assertTrue(found, "Unsupported reason must identify " + reason);
   }



   /*******************************************************************************
    ** The captured copy records the identity delivered before any native delete.
    *******************************************************************************/
   public static class CaptureDeleteOwners implements TableCustomizerInterface
   {
      private static final AtomicReference<List<QRecord>> received = new AtomicReference<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         received.set(records.stream().map(QRecord::new).toList());
         return records;
      }
   }



   /*******************************************************************************
    ** Capture the old owner handed to Replace's Update phase without changing it.
    *******************************************************************************/
   public static class CaptureReplaceOwners implements TableCustomizerInterface
   {
      private static final AtomicReference<List<QRecord>> received = new AtomicReference<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecords)
      {
         received.set(oldRecords.orElse(List.of()).stream().map(QRecord::new).toList());
         return records;
      }
   }



   /*******************************************************************************
    ** USER-only metadata adds a READ lock without changing canonical fields.
    *******************************************************************************/
   public static class UserOwnerPolicy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!TABLE.equals(input.getTableName()) || !QInputSource.USER.equals(input.getInputSource()))
         {
            return input.getTable();
         }
         QTableMetaData active = input.getTable().clone();
         active.setRecordSecurityLocks(List.of(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType(OWNER_KEY)
            .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY)));
         return active;
      }
   }



   /*******************************************************************************
    ** Small public-action and native-driver accessors keep ownership explicit.
    *******************************************************************************/
   private QRecord insert(QRecord record) throws Exception
   {
      QRecord inserted = new InsertAction().executeForRecord(new InsertInput(TABLE).withRecord(record));
      successful(inserted);
      return inserted;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void successful(QRecord record)
   {
      assertTrue(record.getErrors().isEmpty(), record.getErrorsAsString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private GetInput fullGet(String key)
   {
      return new GetInput(TABLE).withPrimaryKey(key).withShouldFetchHeavyFields(true).withShouldMaskPasswords(false).withShouldGenerateDisplayValues(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CountInput countInput(QInputSource source, QQueryFilter filter)
   {
      CountInput input = new CountInput();
      input.setTableName(TABLE);
      input.setInputSource(source);
      input.setFilter(filter);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput aggregateInput(QInputSource source, QQueryFilter filter, Aggregate... aggregates)
   {
      return new AggregateInput(TABLE).withInputSource(source).withFilter(filter).withAggregates(List.of(aggregates));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateResult onlyAggregate(AggregateInput input) throws Exception
   {
      List<AggregateResult> results = new AggregateAction().execute(input).getResults();
      assertEquals(1, results.size());
      return results.get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData table()
   {
      return instance.getTable(TABLE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static MongoDatabase database()
   {
      return client.getDatabase(DATABASE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static MongoCollection<Document> collection()
   {
      return database().getCollection(COLLECTION);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ObjectId id(Integer value)
   {
      return new ObjectId(String.format("%024x", value));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Document nativeRow(String key)
   {
      Document row = collection().find(new Document("_id", new ObjectId(key))).first();
      assertNotNull(row);
      return row;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Document> nativeRows()
   {
      return collection().find().sort(new Document("_id", 1)).into(new ArrayList<>());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> collectionSnapshot(String name)
   {
      JsonWriterSettings settings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
      return database().getCollection(name).find().sort(new Document("_id", 1)).map(document -> document.toJson(settings)).into(new ArrayList<>());
   }



   /*******************************************************************************
    ** Every native collection is captured, including unstructured values.
    *******************************************************************************/
   private Map<String, List<String>> snapshot()
   {
      Map<String, List<String>> result = new LinkedHashMap<>();
      for(String name : database().listCollectionNames().into(new ArrayList<>()).stream().sorted().toList())
      {
         result.put(name, collectionSnapshot(name));
      }
      return result;
   }
}
