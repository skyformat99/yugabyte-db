// Copyright (c) YugaByte, Inc.

#ifndef YB_CONSENSUS_LEADER_LEASE_H
#define YB_CONSENSUS_LEADER_LEASE_H

#include "yb/util/enums.h"

DECLARE_bool(use_leader_leases);
DECLARE_int32(leader_lease_duration_ms);

namespace yb {
namespace consensus {

YB_DEFINE_ENUM(LeaderLeaseCheckMode, (NEED_LEASE)(DONT_NEED_LEASE))

} // namespace consensus
} // namespace yb

#endif // YB_CONSENSUS_LEADER_LEASE_H
