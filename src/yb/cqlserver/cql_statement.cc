//--------------------------------------------------------------------------------------------------
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
//--------------------------------------------------------------------------------------------------

#include "yb/cqlserver/cql_statement.h"

#include <sasl/md5global.h>
#include <sasl/md5.h>

namespace yb {
namespace cqlserver {

//------------------------------------------------------------------------------------------------
CQLStatement::CQLStatement(
    const string& keyspace, const string& ql_stmt, const CQLStatementListPos pos)
    : Statement(keyspace, ql_stmt), pos_(pos) {
}

CQLStatement::~CQLStatement() {
}

CQLMessage::QueryId CQLStatement::GetQueryId(const string& keyspace, const string& ql_stmt) {
  unsigned char md5[16];
  MD5_CTX md5ctx;
  _sasl_MD5Init(&md5ctx);
  _sasl_MD5Update(&md5ctx, util::to_uchar_ptr(keyspace.data()), keyspace.length());
  _sasl_MD5Update(&md5ctx, util::to_uchar_ptr(ql_stmt.data()), ql_stmt.length());
  _sasl_MD5Final(md5, &md5ctx);
  return CQLMessage::QueryId(util::to_char_ptr(md5), sizeof(md5));
}

}  // namespace cqlserver
}  // namespace yb
