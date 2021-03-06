// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
// The following only applies to changes made to this file as part of YugaByte development.
//
// Portions Copyright (c) YugaByte, Inc.
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
#ifndef YB_TABLET_MULTI_COLUMN_WRITER_H
#define YB_TABLET_MULTI_COLUMN_WRITER_H

#include <glog/logging.h>
#include <map>
#include <vector>

#include "yb/common/schema.h"
#include "yb/fs/fs_manager.h"
#include "yb/gutil/macros.h"

namespace yb {

class RowBlock;
class Schema;

namespace cfile {
class CFileWriter;
} // namespace cfile

namespace fs {
class ScopedWritableBlockCloser;
} // namespace fs

namespace tablet {

// Wrapper which writes several columns in parallel corresponding to some
// Schema.
class MultiColumnWriter {
 public:
  MultiColumnWriter(FsManager* fs,
                    const Schema* schema);

  virtual ~MultiColumnWriter();

  // Open and start writing the columns.
  CHECKED_STATUS Open();

  // Append the given block to the output columns.
  //
  // Note that the selection vector here is ignored.
  CHECKED_STATUS AppendBlock(const RowBlock& block);

  // Close the in-progress files.
  //
  // The file's blocks may be retrieved using FlushedBlocks().
  CHECKED_STATUS Finish();

  // Close the in-progress CFiles, releasing the underlying writable blocks
  // to 'closer'.
  CHECKED_STATUS FinishAndReleaseBlocks(fs::ScopedWritableBlockCloser* closer);

  // Return the number of bytes written so far.
  size_t written_size() const;

  cfile::CFileWriter* writer_for_col_idx(int i) {
    DCHECK_LT(i, cfile_writers_.size());
    return cfile_writers_[i];
  }

  // Return the block IDs of the written columns, keyed by column ID.
  //
  // REQUIRES: Finish() already called.
  void GetFlushedBlocksByColumnId(std::map<ColumnId, BlockId>* ret) const;

 private:
  FsManager* const fs_;
  const Schema* const schema_;

  bool finished_;

  std::vector<cfile::CFileWriter *> cfile_writers_;
  std::vector<BlockId> block_ids_;

  DISALLOW_COPY_AND_ASSIGN(MultiColumnWriter);
};

} // namespace tablet
} // namespace yb
#endif /* YB_TABLET_MULTI_COLUMN_WRITER_H */
