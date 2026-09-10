#!/bin/bash
sbt clean scalafmt Test/scalafmt it/scalafmt scalafmtSbt coverage test it/test coverageReport