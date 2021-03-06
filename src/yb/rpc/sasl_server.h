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

#ifndef YB_RPC_SASL_SERVER_H
#define YB_RPC_SASL_SERVER_H

#include <sasl/sasl.h>

#include <set>
#include <string>
#include <vector>

#include "yb/rpc/rpc_header.pb.h"
#include "yb/rpc/sasl_common.h"
#include "yb/rpc/sasl_helper.h"
#include "yb/util/net/socket.h"
#include "yb/util/monotime.h"
#include "yb/util/status.h"

namespace yb {

class Slice;

namespace rpc {

using std::string;

class AuthStore;

// Class for doing SASL negotiation with a SaslClient over a bidirectional socket.
// Operations on this class are NOT thread-safe.
class SaslServer {
 public:
  // Does not take ownership of the socket indicated by the fd.
  SaslServer(string app_name, int fd);
  ~SaslServer();

  // Enable ANONYMOUS authentication.
  // Call after Init().
  CHECKED_STATUS EnableAnonymous();

  // Enable PLAIN authentication. TODO: Support impersonation.
  // Call after Init().
  CHECKED_STATUS EnablePlain(gscoped_ptr<AuthStore> authstore);

  // Returns mechanism negotiated by this connection.
  // Call after Negotiate().
  SaslMechanism::Type negotiated_mechanism() const;

  // Name of the user that authenticated using plain auth.
  // Call after Negotiate() and only if the negotiated mechanism was PLAIN.
  const std::string& plain_auth_user() const;

  // Specify IP:port of local side of connection.
  // Call before Init(). Required for some mechanisms.
  void set_local_addr(const Endpoint& addr);

  // Specify IP:port of remote side of connection.
  // Call before Init(). Required for some mechanisms.
  void set_remote_addr(const Endpoint& addr);

  // Specify the fully-qualified domain name of the remote server.
  // Call before Init(). Required for some mechanisms.
  void set_server_fqdn(const string& domain_name);

  // Set deadline for connection negotiation.
  void set_deadline(const MonoTime& deadline);

  // Get deadline for connection negotiation.
  const MonoTime& deadline() const { return deadline_; }

  // Initialize a new SASL server. Must be called before Negotiate().
  // Returns OK on success, otherwise RuntimeError.
  CHECKED_STATUS Init(const string& service_type);

  // Begin negotiation with the SASL client on the other side of the fd socket
  // that this server was constructed with.
  // Returns OK on success.
  // Otherwise, it may return NotAuthorized, NotSupported, or another non-OK status.
  CHECKED_STATUS Negotiate();

  // SASL callback for plugin options, supported mechanisms, etc.
  // Returns SASL_FAIL if the option is not handled, which does not fail the handshake.
  int GetOptionCb(const char* plugin_name, const char* option,
                  const char** result, unsigned* len);

  // SASL callback for PLAIN authentication via SASL_CB_SERVER_USERDB_CHECKPASS.
  int PlainAuthCb(sasl_conn_t* conn, const char* user, const char* pass,
                  unsigned passlen, struct propctx* propctx);

 private:
  // Parse and validate connection header.
  CHECKED_STATUS ValidateConnectionHeader(faststring* recv_buf);

  // Parse request body. If malformed, sends an error message to the client.
  CHECKED_STATUS ParseSaslMsgRequest(const RequestHeader& header, const Slice& param_buf,
    SaslMessagePB* request);

  // Encode and send the specified SASL message to the client.
  CHECKED_STATUS SendSaslMessage(const SaslMessagePB& msg);

  // Encode and send the specified RPC error message to the client.
  // Calls Status.ToString() for the embedded error message.
  CHECKED_STATUS SendSaslError(ErrorStatusPB::RpcErrorCodePB code, const Status& err);

  // Handle case when client sends NEGOTIATE request.
  CHECKED_STATUS HandleNegotiateRequest(const SaslMessagePB& request);

  // Send a NEGOTIATE response to the client with the list of available mechanisms.
  CHECKED_STATUS SendNegotiateResponse(const std::set<string>& server_mechs);

  // Handle case when client sends INITIATE request.
  CHECKED_STATUS HandleInitiateRequest(const SaslMessagePB& request);

  // Send a CHALLENGE response to the client with a challenge token.
  CHECKED_STATUS SendChallengeResponse(const char* challenge, unsigned clen);

  // Send a SUCCESS response to the client with an token (typically empty).
  CHECKED_STATUS SendSuccessResponse(const char* token, unsigned tlen);

  // Handle case when client sends RESPONSE request.
  CHECKED_STATUS HandleResponseRequest(const SaslMessagePB& request);

  string app_name_;
  Socket sock_;
  std::vector<sasl_callback_t> callbacks_;
  gscoped_ptr<sasl_conn_t, SaslDeleter> sasl_conn_;
  SaslHelper helper_;

  // Authentication store used for PLAIN authentication.
  gscoped_ptr<AuthStore> authstore_;

  // The successfully-authenticated user, if applicable.
  string plain_auth_user_;

  SaslNegotiationState::Type server_state_;

  // The mechanism we negotiated with the client.
  SaslMechanism::Type negotiated_mech_;

  // Intra-negotiation state.
  bool nego_ok_;  // During negotiation: did we get a SASL_OK response from the SASL library?

  // Negotiation timeout deadline.
  MonoTime deadline_;

  DISALLOW_COPY_AND_ASSIGN(SaslServer);
};

} // namespace rpc
} // namespace yb

#endif // YB_RPC_SASL_SERVER_H
