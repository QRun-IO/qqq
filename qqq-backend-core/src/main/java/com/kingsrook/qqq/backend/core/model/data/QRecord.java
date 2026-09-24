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

package com.kingsrook.qqq.backend.core.model.data;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.QErrorMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.SerializationUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Data Record within qqq.  e.g., a single row from a database.
 **
 ** Actual values (e.g., as stored in the backend system) are in the `values`
 ** map.  Keys in this map are fieldNames from the QTableMetaData.
 **
 ** "Display values" (e.g., labels for possible values, or formatted numbers
 ** (e.g., quantities with commas)) are in the displayValues map.
 **
 ** backendDetails are additional data about a record, that aren't strictly
 ** values, but are more like meta-data - e.g., for a file-backend, what file the
 ** record came from.
 **
 ** Errors are meant to hold information about things that went wrong when
 ** processing a record - e.g., in a list of records that may be the output of an
 ** action, like a bulk load.  Warnings play a similar role, but are just advice
 ** - they don't mean that the action was failed, just something you may need to know.
 *******************************************************************************/
public class QRecord implements Serializable
{
   private static final QLogger LOG = QLogger.getLogger(QRecord.class);

   private String tableName;
   private String recordLabel;

   private Map<String, Serializable> values         = new LinkedHashMap<>();
   private Map<String, String>       displayValues  = new LinkedHashMap<>();
   private Map<String, Serializable> backendDetails = new LinkedHashMap<>();

   private List<QErrorMessage>   errors   = new ArrayList<>();
   private List<QWarningMessage> warnings = new ArrayList<>();

   private Map<String, List<QRecord>> associatedRecords = new HashMap<>();

   @JsonIgnore
   private transient PrimaryKeyIdentity primaryKeyIdentity;

   @JsonIgnore
   private transient Object copyIdentity;

   ////////////////////////////////////////////////
   // well-known keys for the backendDetails map //
   ////////////////////////////////////////////////
   public static final String BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT  = "jsonSourceObject"; // String of JSON
   public static final String BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS = "heavyFieldLengths"; // Map<fieldName, length>



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public QRecord()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecord(QTableMetaData tableMetaData, Serializable primaryKeyValue)
   {
      setTableName(tableMetaData.getName());
      setValue(tableMetaData.getPrimaryKeyField(), primaryKeyValue);
   }



   /*******************************************************************************
    ** Copy constructor.  Makes a deep clone.
    **
    *******************************************************************************/
   public QRecord(QRecord record)
   {
      this.tableName = record.tableName;
      this.recordLabel = record.recordLabel;

      this.values = deepCopySimpleMap(record.values);
      this.displayValues = deepCopySimpleMap(record.displayValues);
      this.backendDetails = deepCopySimpleMap(record.backendDetails);

      this.errors = record.errors == null ? null : new ArrayList<>(record.errors);
      this.warnings = record.warnings == null ? null : new ArrayList<>(record.warnings);

      this.associatedRecords = deepCopyAssociatedRecords(record.associatedRecords);
      this.primaryKeyIdentity = record.primaryKeyIdentity;
      this.copyIdentity = record.copyIdentity;
   }



   /*******************************************************************************
    ** Begin private, in-process copy tracking before passing a record to callbacks.
    ** Subsequent QRecord copies share this opaque identity; serialization does not.
    *******************************************************************************/
   @JsonIgnore
   public Object trackCopies()
   {
      if(copyIdentity == null)
      {
         copyIdentity = new Object();
      }
      return copyIdentity;
   }



   /*******************************************************************************
    ** Treat this input as an independent root for a new operation, even if it was
    ** copied from a record participating in an earlier operation.
    *******************************************************************************/
   @JsonIgnore
   public Object startCopyTracking()
   {
      copyIdentity = new Object();
      return copyIdentity;
   }



   /*******************************************************************************
    ** Preserve an internal identity separately from its current presentation.
    ** Framework privacy cleanup may recapture the same identity after sanitizing.
    *******************************************************************************/
   public void capturePrimaryKey(QTableMetaData table, Serializable primaryKey) throws QException
   {
      QFieldMetaData field = identityField(table);
      validateIdentityTable(table, field);
      try
      {
         Serializable typedKey = ValueUtils.getValueAsFieldType(field.getType(), primaryKey);
         if(typedKey == null || (primaryKeyIdentity != null && !identityValuesEqual(primaryKeyIdentity.primaryKey(), typedKey)))
         {
            throw new QException("Invalid primary key identity");
         }
         boolean present = values != null && values.containsKey(table.getPrimaryKeyField());
         Serializable presentationValue = present ? ValueUtils.getValueAsFieldType(field.getType(), values.get(table.getPrimaryKeyField())) : null;
         primaryKeyIdentity = new PrimaryKeyIdentity(table.getName(), table.getPrimaryKeyField(), field.getType(),
            snapshotIdentityValue(typedKey), present, snapshotIdentityValue(presentationValue));
      }
      catch(RuntimeException e)
      {
         throw new QException("Invalid primary key identity");
      }
   }



   /*******************************************************************************
    ** Resolve an operational key without restoring it to values or displayValues.
    ** Public map edits must not silently retarget a captured record.
    *******************************************************************************/
   public Serializable resolvePrimaryKey(QTableMetaData table) throws QException
   {
      QFieldMetaData field = identityField(table);
      validateIdentityTable(table, field);
      try
      {
         boolean present = values != null && values.containsKey(table.getPrimaryKeyField());
         Serializable value = present ? ValueUtils.getValueAsFieldType(field.getType(), values.get(table.getPrimaryKeyField())) : null;
         if(primaryKeyIdentity == null)
         {
            return snapshotIdentityValue(value);
         }
         if(present != primaryKeyIdentity.present() || !identityValuesEqual(value, primaryKeyIdentity.presentationValue()))
         {
            throw new QException("Primary key presentation changed after identity capture");
         }
         return snapshotIdentityValue(primaryKeyIdentity.primaryKey());
      }
      catch(RuntimeException e)
      {
         throw new QException("Invalid primary key identity");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QFieldMetaData identityField(QTableMetaData table) throws QException
   {
      if(table == null || !StringUtils.hasContent(table.getName()) || !StringUtils.hasContent(table.getPrimaryKeyField()) || table.getFields() == null)
      {
         throw new QException("Invalid primary key metadata");
      }
      QFieldMetaData field = table.getFields().get(table.getPrimaryKeyField());
      if(field == null || field.getType() == null)
      {
         throw new QException("Invalid primary key metadata");
      }
      return field;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void validateIdentityTable(QTableMetaData table, QFieldMetaData field) throws QException
   {
      if(primaryKeyIdentity != null && (!primaryKeyIdentity.tableName().equals(table.getName())
         || !primaryKeyIdentity.fieldName().equals(table.getPrimaryKeyField()) || primaryKeyIdentity.type() != field.getType()))
      {
         throw new QException("Primary key identity does not match table metadata");
      }
   }



   /*******************************************************************************
    ** Typed keys are immutable scalars except for BLOB byte arrays.
    *******************************************************************************/
   private static Serializable snapshotIdentityValue(Serializable value)
   {
      return value instanceof byte[] bytes ? bytes.clone() : value;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean identityValuesEqual(Serializable left, Serializable right)
   {
      return left instanceof BigDecimal leftDecimal && right instanceof BigDecimal rightDecimal
         ? leftDecimal.compareTo(rightDecimal) == 0 : Objects.deepEquals(left, right);
   }



   /*******************************************************************************
    ** Owned snapshots are never exposed directly; immutable copies may share them.
    *******************************************************************************/
   private record PrimaryKeyIdentity(String tableName, String fieldName, QFieldType type, Serializable primaryKey, boolean present, Serializable presentationValue)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "QRecord{tableName='" + tableName + "',id='" + getValue("id") + "'}";
   }



   /*******************************************************************************
    ** todo - move to a cloning utils maybe?
    *******************************************************************************/
   @SuppressWarnings({ "unchecked" })
   private <V extends Serializable> Map<String, V> deepCopySimpleMap(Map<String, V> map)
   {
      if(map == null)
      {
         return (null);
      }

      Map<String, V> clone = new LinkedHashMap<>(map.size());
      for(Map.Entry<String, V> entry : map.entrySet())
      {
         Serializable value = entry.getValue();

         //////////////////////////////////////////////////////////////////////////
         // not sure from where/how java.sql.Date objects are getting in here... //
         //////////////////////////////////////////////////////////////////////////
         if(value == null || value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Temporal || value instanceof Date || value instanceof byte[])
         {
            clone.put(entry.getKey(), entry.getValue());
         }
         else if(entry.getValue() instanceof ArrayList<?> arrayList)
         {
            ArrayList<?> cloneList = new ArrayList<>(arrayList);
            clone.put(entry.getKey(), (V) cloneList);
         }
         else if(entry.getValue() instanceof LinkedList<?> linkedList)
         {
            LinkedList<?> cloneList = new LinkedList<>(linkedList);
            clone.put(entry.getKey(), (V) cloneList);
         }
         else if(entry.getValue() instanceof LinkedHashMap<?, ?> linkedHashMap)
         {
            LinkedHashMap<?, ?> cloneMap = new LinkedHashMap<>(linkedHashMap);
            clone.put(entry.getKey(), (V) cloneMap);
         }
         else if(entry.getValue() instanceof HashMap<?, ?> hashMap)
         {
            HashMap<?, ?> cloneMap = new HashMap<>(hashMap);
            clone.put(entry.getKey(), (V) cloneMap);
         }
         else if(entry.getValue() instanceof QRecord otherQRecord)
         {
            clone.put(entry.getKey(), (V) new QRecord(otherQRecord));
         }
         else
         {
            //////////////////////////////////////////////////////////////////////////////
            // we know entry is serializable at this point, based on type param's bound //
            //////////////////////////////////////////////////////////////////////////////
            LOG.debug("Non-primitive serializable value in QRecord - calling SerializationUtils.clone...", logPair("key", entry.getKey()), logPair("type", value.getClass()));
            clone.put(entry.getKey(), (V) SerializationUtils.clone(entry.getValue()));
         }
      }
      return (clone);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, List<QRecord>> deepCopyAssociatedRecords(Map<String, List<QRecord>> input)
   {
      if(input == null)
      {
         return (null);
      }

      Map<String, List<QRecord>> clone = new HashMap<>();
      for(Map.Entry<String, List<QRecord>> entry : input.entrySet())
      {
         clone.put(entry.getKey(), new ArrayList<>(entry.getValue()));
      }
      return (clone);
   }



   /*******************************************************************************
    ** Added when QRecords got exposed in scripts, and passing a constant String
    ** raised a class-cast exception, because it was some nashorn non-serializable
    ** type (though once inside *this* method, the value was a java.lang.String...)
    *******************************************************************************/
   public void setValue(String fieldName, Object value)
   {
      if(value == null)
      {
         setValue(fieldName, null);
      }
      else if(value instanceof Serializable s)
      {
         setValue(fieldName, s);
      }
      else
      {
         setValue(fieldName, ValueUtils.getValueAsString(value));
      }
   }


   /***************************************************************************
    ** copy all values from 'joinedRecord' into this record's values map,
    ** prefixing field names with joinTableNam + "."
    ***************************************************************************/
   public void addJoinedRecordValues(String joinTableName, QRecord joinedRecord)
   {
      if(joinedRecord == null)
      {
         return;
      }

      for(Map.Entry<String, Serializable> entry : joinedRecord.getValues().entrySet())
      {
         setValue(joinTableName + "." + entry.getKey(), entry.getValue());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setValue(String fieldName, Serializable value)
   {
      values.put(fieldName, value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void removeValue(String fieldName)
   {
      values.remove(fieldName);
      displayValues.remove(fieldName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setValue(QFieldMetaData field, Serializable value)
   {
      values.put(field.getName(), value);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecord withValue(String fieldName, Serializable value)
   {
      setValue(fieldName, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setDisplayValue(String fieldName, String displayValue)
   {
      displayValues.put(fieldName, displayValue);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QRecord withDisplayValue(String fieldName, String displayValue)
   {
      setDisplayValue(fieldName, displayValue);
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public QRecord withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordLabel
    **
    *******************************************************************************/
   public String getRecordLabel()
   {
      return recordLabel;
   }



   /*******************************************************************************
    ** Setter for recordLabel
    **
    *******************************************************************************/
   public void setRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
   }



   /*******************************************************************************
    ** Fluent setter for recordLabel
    **
    *******************************************************************************/
   public QRecord withRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for values
    **
    *******************************************************************************/
   @JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.ALWAYS)
   public Map<String, Serializable> getValues()
   {
      return values;
   }



   /*******************************************************************************
    ** Setter for values
    **
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public Serializable getValue(String fieldName)
   {
      return (values.get(fieldName));
   }



   /*******************************************************************************
    ** Getter for displayValues
    **
    *******************************************************************************/
   public Map<String, String> getDisplayValues()
   {
      return displayValues;
   }



   /*******************************************************************************
    ** Setter for displayValues
    **
    *******************************************************************************/
   public void setDisplayValues(Map<String, String> displayValues)
   {
      this.displayValues = displayValues;
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public String getDisplayValue(String fieldName)
   {
      return (displayValues.get(fieldName));
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public String getValueString(String fieldName)
   {
      return (ValueUtils.getValueAsString(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public Integer getValueInteger(String fieldName)
   {
      return (ValueUtils.getValueAsInteger(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Getter for a single field's value
    **
    *******************************************************************************/
   public Long getValueLong(String fieldName)
   {
      return (ValueUtils.getValueAsLong(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public BigDecimal getValueBigDecimal(String fieldName)
   {
      return (ValueUtils.getValueAsBigDecimal(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Boolean getValueBoolean(String fieldName)
   {
      return (ValueUtils.getValueAsBoolean(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public LocalTime getValueLocalTime(String fieldName)
   {
      return (ValueUtils.getValueAsLocalTime(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public LocalDate getValueLocalDate(String fieldName)
   {
      return (ValueUtils.getValueAsLocalDate(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public byte[] getValueByteArray(String fieldName)
   {
      return (ValueUtils.getValueAsByteArray(getValue(fieldName)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Instant getValueInstant(String fieldName)
   {
      return (ValueUtils.getValueAsInstant(getValue(fieldName)));
   }



   /*******************************************************************************
    ** Getter for backendDetails
    **
    *******************************************************************************/
   @JsonIgnoreProperties(value = BACKEND_DETAILS_TYPE_JSON_SOURCE_OBJECT, allowSetters = true)
   public Map<String, Serializable> getBackendDetails()
   {
      return backendDetails;
   }



   /*******************************************************************************
    ** Setter for backendDetails
    **
    *******************************************************************************/
   public void setBackendDetails(Map<String, Serializable> backendDetails)
   {
      this.backendDetails = backendDetails;
   }



   /*******************************************************************************
    ** Add one backendDetail to this record
    **
    *******************************************************************************/
   public void addBackendDetail(String key, Serializable value)
   {
      this.backendDetails.put(key, value);
   }



   /*******************************************************************************
    ** Fluently Add one backendDetail to this record
    **
    *******************************************************************************/
   public QRecord withBackendDetail(String key, Serializable value)
   {
      addBackendDetail(key, value);
      return (this);
   }



   /*******************************************************************************
    ** Get one backendDetail from this record
    **
    *******************************************************************************/
   public Serializable getBackendDetail(String key)
   {
      if(!this.backendDetails.containsKey(key))
      {
         return (null);
      }

      return this.backendDetails.get(key);
   }



   /*******************************************************************************
    ** Get one backendDetail from this record as a String
    **
    *******************************************************************************/
   public String getBackendDetailString(String key)
   {
      return (String) getBackendDetail(key);
   }



   /*******************************************************************************
    ** Getter for errors
    **
    *******************************************************************************/
   public List<QErrorMessage> getErrors()
   {
      return (errors);
   }



   /*******************************************************************************
    ** Getter for errors
    **
    *******************************************************************************/
   @JsonIgnore
   public String getErrorsAsString()
   {
      if(CollectionUtils.nullSafeHasContents(errors))
      {
         return StringUtils.join("; ", errors.stream().map(e -> e.getMessage()).toList());
      }
      return ("");
   }



   /*******************************************************************************
    ** Setter for errors
    **
    *******************************************************************************/
   public void setErrors(List<QErrorMessage> errors)
   {
      this.errors = errors;
   }



   /*******************************************************************************
    ** Add one error to this record
    **
    *******************************************************************************/
   public void addError(QErrorMessage error)
   {
      this.errors.add(error);
   }



   /*******************************************************************************
    ** Fluently Add one error to this record
    **
    *******************************************************************************/
   public QRecord withError(QErrorMessage error)
   {
      addError(error);
      return (this);
   }



   /*******************************************************************************
    ** Convert this record to an QRecordEntity
    *******************************************************************************/
   public <T extends QRecordEntity> T toEntity(Class<T> c) throws QException
   {
      return (QRecordEntity.fromQRecord(c, this));
   }



   /*******************************************************************************
    ** Getter for associatedRecords
    *******************************************************************************/
   @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
   public Map<String, List<QRecord>> getAssociatedRecords()
   {
      return (this.associatedRecords);
   }



   /*******************************************************************************
    ** Setter for associatedRecords
    *******************************************************************************/
   public void setAssociatedRecords(Map<String, List<QRecord>> associatedRecords)
   {
      this.associatedRecords = associatedRecords;
   }



   /*******************************************************************************
    ** Fluent setter for associatedRecords
    *******************************************************************************/
   public QRecord withAssociatedRecords(Map<String, List<QRecord>> associatedRecords)
   {
      this.associatedRecords = associatedRecords;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for associatedRecords
    *******************************************************************************/
   public QRecord withAssociatedRecords(String name, List<QRecord> associatedRecords)
   {
      if(this.associatedRecords == null)
      {
         this.associatedRecords = new HashMap<>();
      }
      this.associatedRecords.put(name, associatedRecords);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for associatedRecord
    *******************************************************************************/
   public QRecord withAssociatedRecord(String name, QRecord associatedRecord)
   {
      if(this.associatedRecords == null)
      {
         this.associatedRecords = new HashMap<>();
      }
      this.associatedRecords.putIfAbsent(name, new ArrayList<>());
      this.associatedRecords.get(name).add(associatedRecord);
      return (this);
   }



   /*******************************************************************************
    ** Getter for warnings
    *******************************************************************************/
   public List<QWarningMessage> getWarnings()
   {
      return (this.warnings);
   }



   /*******************************************************************************
    ** Getter for warnings
    **
    *******************************************************************************/
   @JsonIgnore
   public String getWarningsAsString()
   {
      if(CollectionUtils.nullSafeHasContents(warnings))
      {
         return StringUtils.join("; ", warnings.stream().map(e -> e.getMessage()).toList());
      }
      return ("");
   }



   /*******************************************************************************
    ** Setter for warnings
    *******************************************************************************/
   public void setWarnings(List<QWarningMessage> warnings)
   {
      this.warnings = warnings;
   }



   /*******************************************************************************
    ** Fluently Add one warning to this record
    **
    *******************************************************************************/
   public QRecord withWarning(QWarningMessage warning)
   {
      addWarning(warning);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for warnings
    *******************************************************************************/
   public QRecord withWarnings(List<QWarningMessage> warnings)
   {
      this.warnings = warnings;
      return (this);
   }



   /*******************************************************************************
    ** Add one warning to this record
    **
    *******************************************************************************/
   public void addWarning(QWarningMessage warning)
   {
      this.warnings.add(warning);
   }

}
