package com.jamezrin.solrj.dobby.adapter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.jamezrin.solrj.dobby.Dobby;
import com.jamezrin.solrj.dobby.DobbyException;
import com.jamezrin.solrj.dobby.DobbyUtils;
import com.jamezrin.solrj.dobby.TypeAdapter;
import com.jamezrin.solrj.dobby.TypeAdapterFactory;
import com.jamezrin.solrj.dobby.TypeToken;

/**
 * Factory for adapters that handle {@code java.time} types.
 *
 * <p>Solr stores dates as {@link java.util.Date} objects or ISO-8601 strings. This factory converts
 * between Solr's representation and modern Java time types.
 */
public final class JavaTimeAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Dobby dobby, TypeToken<T> type) {
    Class<?> raw = type.getRawType();
    if (raw == Instant.class) return DobbyUtils.uncheckedCast(INSTANT_ADAPTER);
    if (raw == LocalDate.class) return DobbyUtils.uncheckedCast(LOCAL_DATE_ADAPTER);
    if (raw == LocalDateTime.class) return DobbyUtils.uncheckedCast(LOCAL_DATE_TIME_ADAPTER);
    if (raw == ZonedDateTime.class) return DobbyUtils.uncheckedCast(ZONED_DATE_TIME_ADAPTER);
    if (raw == Date.class) return DobbyUtils.uncheckedCast(DATE_ADAPTER);
    return null;
  }

  private static Instant toInstant(Object solrValue) {
    if (solrValue instanceof Date d) return d.toInstant();
    if (solrValue instanceof String s) return Instant.parse(s);
    if (solrValue instanceof Number n) return Instant.ofEpochMilli(n.longValue());
    throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Instant");
  }

  private static final TypeAdapter<Instant> INSTANT_ADAPTER =
      new TypeAdapter<Instant>() {
        @Override
        public Instant read(Object solrValue) {
          if (solrValue == null) return null;
          return toInstant(solrValue);
        }

        @Override
        public Object write(Instant value) {
          return value == null ? null : Date.from(value);
        }

        @Override
        public String toString() {
          return "JavaTimeAdapter[Instant]";
        }
      };

  private static final TypeAdapter<LocalDate> LOCAL_DATE_ADAPTER =
      new TypeAdapter<LocalDate>() {
        @Override
        public LocalDate read(Object solrValue) {
          if (solrValue == null) return null;
          if (solrValue instanceof String s) {
            return LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
          }
          return toInstant(solrValue).atZone(ZoneOffset.UTC).toLocalDate();
        }

        @Override
        public Object write(LocalDate value) {
          if (value == null) return null;
          return Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
        }

        @Override
        public String toString() {
          return "JavaTimeAdapter[LocalDate]";
        }
      };

  private static final TypeAdapter<LocalDateTime> LOCAL_DATE_TIME_ADAPTER =
      new TypeAdapter<LocalDateTime>() {
        @Override
        public LocalDateTime read(Object solrValue) {
          if (solrValue == null) return null;
          if (solrValue instanceof String s) {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
          }
          return toInstant(solrValue).atZone(ZoneOffset.UTC).toLocalDateTime();
        }

        @Override
        public Object write(LocalDateTime value) {
          if (value == null) return null;
          return Date.from(value.toInstant(ZoneOffset.UTC));
        }

        @Override
        public String toString() {
          return "JavaTimeAdapter[LocalDateTime]";
        }
      };

  private static final TypeAdapter<ZonedDateTime> ZONED_DATE_TIME_ADAPTER =
      new TypeAdapter<ZonedDateTime>() {
        @Override
        public ZonedDateTime read(Object solrValue) {
          if (solrValue == null) return null;
          if (solrValue instanceof String s) {
            return ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME);
          }
          return toInstant(solrValue).atZone(ZoneOffset.UTC);
        }

        @Override
        public Object write(ZonedDateTime value) {
          if (value == null) return null;
          return Date.from(value.toInstant());
        }

        @Override
        public String toString() {
          return "JavaTimeAdapter[ZonedDateTime]";
        }
      };

  private static final TypeAdapter<Date> DATE_ADAPTER =
      new TypeAdapter<Date>() {
        @Override
        public Date read(Object solrValue) {
          if (solrValue == null) return null;
          if (solrValue instanceof Date d) return d;
          if (solrValue instanceof String s) return Date.from(Instant.parse(s));
          if (solrValue instanceof Number n) return new Date(n.longValue());
          throw new DobbyException("Cannot convert " + solrValue.getClass().getName() + " to Date");
        }

        @Override
        public Object write(Date value) {
          return value;
        }

        @Override
        public String toString() {
          return "JavaTimeAdapter[Date]";
        }
      };
}
