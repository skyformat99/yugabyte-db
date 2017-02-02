# Copyright (c) YugaByte, Inc.

GMOCK_VERSION=1.7.0
GMOCK_DIR=$TP_SOURCE_DIR/gmock-$GMOCK_VERSION

build_gmock() {
  create_build_dir_and_prepare "$GMOCK_DIR"
  for SHARED in OFF ON; do
    remove_cmake_cache
    (
      set_build_env_vars
      set -x
      CXXFLAGS="$EXTRA_CXXFLAGS $EXTRA_LDFLAGS $EXTRA_LIBS" \
        cmake \
        -DCMAKE_BUILD_TYPE=Debug \
        -DCMAKE_POSITION_INDEPENDENT_CODE=On \
        -DBUILD_SHARED_LIBS=$SHARED .
      run_make
    )
  done
  log Installing gmock...
  (
    set_build_env_vars
    set -x
    cp -a libgmock.$DYLIB_SUFFIX libgmock.a $PREFIX/lib/
    rsync -av include/ $PREFIX/include/
    rsync -av gtest/include/ $PREFIX/include/
  )
}