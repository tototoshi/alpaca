#!/usr/bin/env bash
cd $(dirname $0)
./sbt 'project lang' startScript "testkit/run $(ls -1 test/*.alpacat)"
