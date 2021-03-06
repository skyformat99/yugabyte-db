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

#include "yb/client/session-internal.h"

#include <memory>
#include <mutex>

#include "yb/client/batcher.h"
#include "yb/client/callbacks.h"
#include "yb/client/error_collector.h"

namespace yb {

namespace client {

using internal::Batcher;
using internal::ErrorCollector;

using std::shared_ptr;

YBSessionData::YBSessionData(shared_ptr<YBClient> client,
                             bool read_only,
                             const YBTransactionPtr& transaction)
    : client_(std::move(client)),
      read_only_(read_only),
      transaction_(transaction),
      error_collector_(new ErrorCollector()) {
}

YBSessionData::~YBSessionData() {
}

void YBSessionData::Init() {
  CHECK(!batcher_);
  NewBatcher();
}

scoped_refptr<Batcher> YBSessionData::NewBatcher() {
  std::lock_guard<simple_spinlock> l(lock_);

  scoped_refptr<Batcher> batcher(
    new Batcher(client_.get(), error_collector_.get(), shared_from_this(),
                external_consistency_mode_));
  if (timeout_ms_ != -1) {
    batcher->SetTimeoutMillis(timeout_ms_);
  }
  batcher.swap(batcher_);

  if (batcher) {
    CHECK(flushed_batchers_.insert(batcher.get()).second);
  }
  return batcher;
}

void YBSessionData::FlushFinished(Batcher* batcher) {
  std::lock_guard<simple_spinlock> l(lock_);
  CHECK_EQ(flushed_batchers_.erase(batcher), 1);
}

void YBSessionData::Abort() {
  if (batcher_->HasPendingOperations()) {
    NewBatcher()->Abort(STATUS(Aborted, "Batch aborted"));
  }
}

Status YBSessionData::Close(bool force) {
  if (batcher_->HasPendingOperations() && !force) {
    return STATUS(IllegalState, "Could not close. There are pending operations.");
  }
  batcher_->Abort(STATUS(Aborted, "Batch aborted"));
  return Status::OK();
}

void YBSessionData::FlushAsync(YBStatusCallback* callback) {
  CHECK_NE(flush_mode_, YBSession::AUTO_FLUSH_BACKGROUND) << "TODO: handle flush background mode";

  // Swap in a new batcher to start building the next batch.
  // Save off the old batcher.
  //
  // Send off any buffered data. Important to do this outside of the lock
  // since the callback may itself try to take the lock, in the case that
  // the batch fails "inline" on the same thread.
  NewBatcher()->FlushAsync(callback);
}

Status YBSessionData::Apply(std::shared_ptr<YBOperation> yb_op) {
  CHECK_EQ(yb_op->read_only(), read_only_);

  // Check if the operations have the hashed keys. Read operations do not require key sets.
  if (!yb_op->row().IsHashOrPrimaryKeySet() && yb_op->type() != YBOperation::QL_READ) {
    Status status = STATUS(IllegalState, "Key not specified", yb_op->ToString());
    error_collector_->AddError(yb_op, status);
    return status;
  }

  Status s = batcher_->Add(yb_op);
  if (!PREDICT_FALSE(s.ok())) {
    error_collector_->AddError(yb_op, s);
    return s;
  }

  if (flush_mode_ == YBSession::AUTO_FLUSH_SYNC) {
    return Flush();
  }

  return Status::OK();
}

Status YBSessionData::Flush() {
  Synchronizer s;
  YBStatusMemberCallback<Synchronizer> ksmcb(&s, &Synchronizer::StatusCB);
  FlushAsync(&ksmcb);
  return s.Wait();
}

}  // namespace client
}  // namespace yb
