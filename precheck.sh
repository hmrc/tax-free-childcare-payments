#!/bin/bash
sbt clean scalafmt test:scalafmt coverage test it/test coverageReport