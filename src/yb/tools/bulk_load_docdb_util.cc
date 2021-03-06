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

#include "yb/rocksdb/env.h"
#include "yb/rocksdb/statistics.h"
#include "yb/docdb/docdb_compaction_filter.h"
#include "yb/rocksutil/yb_rocksdb.h"
#include "yb/tools/bulk_load_docdb_util.h"
#include "yb/util/env.h"
#include "yb/util/path_util.h"

DECLARE_int32(num_memtables);

namespace yb {
namespace tools {

BulkLoadDocDBUtil::BulkLoadDocDBUtil(const std::string& tablet_id,
                                     const std::string& base_dir,
                                     const size_t memtable_size,
                                     int num_memtables,
                                     int max_background_flushes)
    : DocDBRocksDBUtil(OpId()),
      tablet_id_(tablet_id),
      base_dir_(base_dir),
      memtable_size_(memtable_size),
      num_memtables_(num_memtables),
      max_background_flushes_(max_background_flushes) {
}

Status BulkLoadDocDBUtil::InitRocksDBDir() {
  rocksdb_dir_ = JoinPathSegments(base_dir_, tablet_id_);
  RETURN_NOT_OK(Env::Default()->DeleteRecursively(rocksdb_dir_));
  return Status::OK();
}

Status BulkLoadDocDBUtil::InitRocksDBOptions() {
  RETURN_NOT_OK(InitCommonRocksDBOptions());
  rocksdb_options_.max_write_buffer_number = num_memtables_;
  rocksdb_options_.write_buffer_size = memtable_size_;
  rocksdb_options_.allow_concurrent_memtable_write = true;
  rocksdb_options_.enable_write_thread_adaptive_yield = true;
  rocksdb_options_.max_background_flushes = max_background_flushes_;
  rocksdb_options_.env->SetBackgroundThreads(max_background_flushes_, rocksdb::Env::Priority::HIGH);
  return Status::OK();
}

std::string BulkLoadDocDBUtil::tablet_id() {
  return tablet_id_;
}

const std::string& BulkLoadDocDBUtil::rocksdb_dir() {
  return rocksdb_dir_;
}

} // namespace tools
} // namespace yb
