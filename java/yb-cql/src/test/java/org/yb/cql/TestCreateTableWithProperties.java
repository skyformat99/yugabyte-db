// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//
package org.yb.cql;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

public class TestCreateTableWithProperties extends BaseCQLTest {
  @Test
  public void testCreateTable() throws Exception {
    LOG.info("Begin test");

    // With default_time_to_live.
    String create_stmt = "CREATE TABLE test_create " +
                         "(h1 int, h2 varchar, r1 int, r2 varchar, v1 int, v2 varchar, " +
                         "primary key ((h1, h2), r1, r2)) " +
                         "WITH bloom_filter_fp_chance = 0.01 " +
                         "AND comment = '' " +
                         "AND crc_check_chance = 1.0 " +
                         "AND dclocal_read_repair_chance = 0.1 " +
                         "AND default_time_to_live = 0 " +
                         "AND gc_grace_seconds = 864000 " +
                         "AND max_index_interval = 2048 " +
                         "AND memtable_flush_period_in_ms = 0 " +
                         "AND min_index_interval = 128 " +
                         "AND read_repair_chance = 0.0 " +
                         "AND speculative_retry = '99.0PERCENTILE' " +
                         "AND caching = { " +
                         "    'keys' : 'ALL', " +
                         "    'rows_per_partition' : 'NONE' " +
                         "} " +
                         "AND compression = { " +
                         "    'chunk_length_in_kb' : 64, " +
                         "    'class' : 'LZ4Compressor', " +
                         "    'enabled' : true " +
                         "} " +
                         "AND compaction = { " +
                         "    'base_time_seconds' : 60, " +
                         "    'class' : 'DateTieredCompactionStrategy', " +
                         "    'enabled' : true, " +
                         "    'max_sstable_age_days' : 365, " +
                         "    'max_threshold' : 32, " +
                         "    'min_threshold' : 4, " +
                         "    'timestamp_resolution' : 'MICROSECONDS', " +
                         "    'tombstone_compaction_interval' : 86400, " +
                         "    'tombstone_threshold' : 0.2, " +
                         "    'unchecked_tombstone_compaction' : false " +
                         "} AND COMPACT STORAGE; ";

    session.execute(create_stmt);

    String drop_stmt = "DROP TABLE test_create;";
    session.execute(drop_stmt);

    // With CLUSTERING ORDER BY and default_time_to_live.
    create_stmt = "CREATE TABLE test_create " +
        "(agg_time_hour int, site_id int, lastrun_time_minute int, value int, " +
        "PRIMARY KEY (agg_time_hour, site_id)) " +
        "WITH CLUSTERING ORDER BY (site_id ASC) " +
        "AND bloom_filter_fp_chance = 0.01 " +
        "AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'} " +
        "AND comment = '' " +
        "AND compaction = { " +
        "    'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', " +
        "    'max_threshold': '32', " +
        "    'min_threshold': '4' " +
        "} " +
        "AND compression = { " +
        "   'chunk_length_kb': '64', " +
        "   'class': 'org.apache.cassandra.io.compress.LZ4Compressor' " +
        "} " +
        "AND crc_check_chance = 1.0 " +
        "AND dclocal_read_repair_chance = 0.1 " +
        "AND default_time_to_live = 0 " +
        "AND gc_grace_seconds = 864000 "  +
        "AND max_index_interval = 2048 " +
        "AND memtable_flush_period_in_ms = 0 " +
        "AND min_index_interval = 128 " +
        "AND read_repair_chance = 0.0 " +
        "AND speculative_retry = '99PERCENTILE' " +
        "AND COMPACT STORAGE; ";

    session.execute(create_stmt);

    session.execute(drop_stmt);

    LOG.info("End test");
  }

  private String CreateTableStmt(String property) {
    return String.format("CREATE TABLE test_create (h1 int, r1 int, v1 int, " +
                         "primary key ((h1), r1)) WITH %s", property);
  }

  private void RunInvalidTableProperty(String property) throws Exception {
    runInvalidStmt(CreateTableStmt(property));
  }

  private void RunValidTableProperty(String property) throws Exception {
    session.execute(CreateTableStmt(property));
    dropTable("test_create");
  }

  private void testInvalidString(String property) throws Exception {
    RunInvalidTableProperty(String.format("%s = 1", property));
    RunInvalidTableProperty(String.format("%s = 1.0", property));
    RunInvalidTableProperty(String.format("%s = true", property));
    RunInvalidTableProperty(String.format("%s = false", property));
    RunInvalidTableProperty(String.format("%s = { 'a' : 1 }", property));
  }

  @Test
  public void testComment() throws Exception {
    testInvalidString("comment");
    RunValidTableProperty("comment = 'a'");
    RunValidTableProperty("comment = ''");
  }

  @Test
  public void testCompaction() throws Exception {
    RunInvalidTableProperty("compaction = 1");
    RunInvalidTableProperty("compaction = 1.0");
    RunInvalidTableProperty("compaction = true");
    RunInvalidTableProperty("compaction = false");
    RunInvalidTableProperty("compaction = ''");
    RunInvalidTableProperty("compaction = { 'a' : 1 }");
    RunValidTableProperty("compaction = { 'class' : 'SizeTieredCompactionStrategy' }");
    RunInvalidTableProperty("compaction = { 'class' : 'SizeTieredCompactionStrategy-' }");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'bucket_high' : 3.3 " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'bucket_high' : 1.3, " +
        "  'bucket_low' : 3.3 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'enabled' : true " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'enabled' : false " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'enabled' : 1 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'log_all' : true " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'log_all' : 1 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'max_threshold' : 10 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'min_threshold' : 3 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'max_threshold' : 5, " +
        "  'min_threshold' : 3 " +
        "}");

    // Less than default min_threshold = 4.
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'max_threshold' : 3   " +
        "}");

    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'min_threshold' : 0 " +
        "}");

    // More than default max_threshold = 32.
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'min_threshold' : 33 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'min_sstable_size' : 0 " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'min_sstable_size' : -1 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'only_purge_repaired_tombstones' : true " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'only_purge_repaired_tombstones' : 1 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'tombstone_compaction_interval' : 1 " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'tombstone_compaction_interval' : 0 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'tombstone_threshold' : 1.0 " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'tombstone_threshold' : 0 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'unchecked_tombstone_compaction' : true " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'SizeTieredCompactionStrategy', " +
        "  'unchecked_tombstone_compaction' : 1 " +
        "}");

    // DateTieredCompactionStrategy tests.
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'bucket_high' : 3.3 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'enabled' : true " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'log_all' : true " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'max_sstable_age_days' : 0 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'max_sstable_age_days' : 0.1 " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'max_sstable_age_days' : -0.1 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'max_window_size_seconds' : 0 " +
        "}");
    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'max_window_size_seconds' : 1 " +
        "}");
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'max_window_size_seconds' : -1 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'max_threshold' : 4, " +
        "  'min_threshold' : 3 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'timestamp_resolution' : 'MICROSECONDS' " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'timestamp_resolution' : 'MILLISECONDS' " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'tombstone_compaction_interval' : 1 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'tombstone_threshold' : 1.0 " +
        "}");

    RunValidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'unchecked_tombstone_compaction' : true " +
        "}");

    // Invalid options for class DateTieredCompactionStrategy.
    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'only_purge_repaired_tombstones' : true " +
        "}");

    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'bucket_high' : 5.5 " +
        "}");

    RunInvalidTableProperty("compaction = " +
        "{ " +
        "  'class' : 'DateTieredCompactionStrategy', " +
        "  'bucket_low' : 3.3 " +
        "}");
  }

  @Test
  public void testDefaultTimeToLive() throws Exception {
    testInvalidNumericValues("default_time_to_live");
    RunInvalidTableProperty("default_time_to_live = -0.01");
    RunInvalidTableProperty("default_time_to_live = 1.01");
    RunInvalidTableProperty("default_time_to_live = 34123412341234123421");
    RunValidTableProperty("default_time_to_live = 0");
    RunValidTableProperty("default_time_to_live = '1'");
    RunValidTableProperty("default_time_to_live = 1");
    RunValidTableProperty("default_time_to_live = 100000");
  }

  private void testDoubleMinValue(double min, String property) throws Exception {
    testInvalidNumericValues(property);
    RunInvalidTableProperty(String.format("%s = %f", property, min - .01));
    RunInvalidTableProperty(String.format("%s = '%f'", property, min - .01));
    RunValidTableProperty(String.format("%s = %f", property, min));
    RunValidTableProperty(String.format("%s = '%f'", property, min));
  }

  private void testDoubleMaxValue(double max, String property) throws Exception {
    testInvalidNumericValues(property);
    RunInvalidTableProperty(String.format("%s = %f", property, max + .01));
    RunInvalidTableProperty(String.format("%s = '%f'", property, max + .01));
    RunValidTableProperty(String.format("%s = %f", property, max));
    RunValidTableProperty(String.format("%s = '%f'", property, max));
  }

  private void testDoubleMinMaxValue(double min, double max, String property) throws Exception {
    testDoubleMinValue(min, property);
    testDoubleMaxValue(max, property);
    RunValidTableProperty(String.format("%s = %f", property, (min + max) / 2.0));
    RunValidTableProperty(String.format("%s = '%f'", property, (min + max) / 2.0));
  }

  private void testInvalidNumericValues(String property) throws Exception {
    RunInvalidTableProperty(String.format("%s = true", property));
    RunInvalidTableProperty(String.format("%s = false", property));
    RunInvalidTableProperty(String.format("%s = 'a'", property));
    RunInvalidTableProperty(String.format("%s = ''", property));
    RunInvalidTableProperty(String.format("%s = { 'a' : 1 }", property));
  }

  private void testAnyDoubleValue(String property) throws Exception {
    testInvalidNumericValues(property);
    double random = new Random().nextDouble();
    RunInvalidTableProperty(String.format("%s = %f", property, random));
  }

  private void testIntMinValue(long min, String property) throws Exception {
    testInvalidNumericValues(property);
    double d = min + .01;
    RunInvalidTableProperty(String.format("%s = %f", property, d));
    RunInvalidTableProperty(String.format("%s = %d", property, min - 1));
    RunValidTableProperty(String.format("%s = %d", property, min));
  }

  @Test
  public void testBloomFilterFpChance() throws Exception {
    testDoubleMinMaxValue(0.00001, .99999, "bloom_filter_fp_chance");
  }

  @Test
  public void testCrcCheckChance() throws Exception {
    testDoubleMinMaxValue(0, 1, "crc_check_chance");
  }

  @Test
  public void testDclocalReadRepairChance() throws Exception {
    testDoubleMinMaxValue(0, 1, "dclocal_read_repair_chance");
  }

  @Test
  public void testReadRepairChance() throws Exception {
    testDoubleMinMaxValue(0, 1, "read_repair_chance");
  }

  @Test
  public void testGcGraceSeconds() throws Exception {
    testIntMinValue(0, "gc_grace_seconds");
  }

  @Test
  public void testIndexInterval() throws Exception {
    testIntMinValue(1, "index_interval");
  }

  @Test
  public void testMinIndexInterval() throws Exception {
    testIntMinValue(1, "min_index_interval");
  }

  @Test
  public void testMaxIndexInterval() throws Exception {
    testIntMinValue(1, "max_index_interval");
  }

  @Test
  public void testCaching() throws Exception {
    RunInvalidTableProperty("caching = 1");
    RunInvalidTableProperty("caching = 1.0");
    RunInvalidTableProperty("caching = true");
    RunInvalidTableProperty("caching = false");
    RunInvalidTableProperty("caching = ''");
    RunInvalidTableProperty("caching = { 'a' : 1 }");
    RunValidTableProperty("caching = { 'keys' : 'ALL' }");
    RunValidTableProperty("caching = { 'keys' : 'All' }");
    RunValidTableProperty("caching = { 'keys' : 'NONE' }");
    RunValidTableProperty("caching = { 'keys' : 'None' }");
    RunInvalidTableProperty("caching = { 'keys' : 'ABC' }");
    RunValidTableProperty("caching = { 'rows_per_partition' : 'NONE' }");
    RunValidTableProperty("caching = { 'rows_per_partition' : 'ALL' }");
    RunValidTableProperty("caching = { 'rows_per_partition' : '1' }");
    RunValidTableProperty("caching = { 'rows_per_partition' : 1 }");
    RunInvalidTableProperty("caching = { 'rows_per_partition' : 'ABC' }");
    RunValidTableProperty("caching = { 'keys' : 'ALL',  'rows_per_partition' : 'NONE' }");
    RunValidTableProperty("caching = { 'keys' : 'ALL',  'rows_per_partition' : 'ALL' }");
    RunValidTableProperty("caching = { 'keys' : 'ALL',  'rows_per_partition' : '1' }");
    RunValidTableProperty("caching = { 'keys' : 'ALL',  'rows_per_partition' : 1 }");
    RunValidTableProperty("caching = { 'keys' : 'NONE',  'rows_per_partition' : 'NONE' }");
    RunValidTableProperty("caching = { 'keys' : 'NONE',  'rows_per_partition' : 'ALL' }");
    RunValidTableProperty("caching = { 'keys' : 'NONE',  'rows_per_partition' : '1' }");
    RunValidTableProperty("caching = { 'keys' : 'NONE',  'rows_per_partition' : 1 }");
    RunInvalidTableProperty("caching = { 'keys' : 'ABC',  'rows_per_partition' : 'NONE' }");
    RunInvalidTableProperty("caching = { 'keys' : 'ABC',  'rows_per_partition' : 'ALL' }");
    RunInvalidTableProperty("caching = { 'keys' : 'ABC',  'rows_per_partition' : '1' }");
    RunInvalidTableProperty("caching = { 'keys' : 'ABC',  'rows_per_partition' : 1 }");
    RunInvalidTableProperty("caching = { 'keys' : 'ALL',  'rows_per_partition' : 'ABC' }");
    RunInvalidTableProperty("caching = { 'keys' : 'NONE',  'rows_per_partition' : 'ABC' }");
  }

  @Test
  public void testSpeculativeRetry() throws Exception {
    testInvalidString("speculative_retry");
    RunInvalidTableProperty("speculative_retry = 'ms'");
    RunValidTableProperty("speculative_retry = '3ms'");
    RunValidTableProperty("speculative_retry = 'ALWAYS'");
    RunValidTableProperty("speculative_retry = 'Always'");
    RunValidTableProperty("speculative_retry = '99PERCENTILE'");
    RunValidTableProperty("speculative_retry = '99Percentile'");
    RunValidTableProperty("speculative_retry = 'NONE'");
    RunValidTableProperty("speculative_retry = 'None'");
  }

  @Test
  public void testCompression() throws Exception {
    RunValidTableProperty("compression = {'sstable_compression' : ''}");
    RunInvalidTableProperty(
        "compression = {'class' : 'LZ4Compressor', 'sstable_compression' : ''}");
  }

}
