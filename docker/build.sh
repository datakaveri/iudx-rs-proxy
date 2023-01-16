#!/bin/bash

# To be executed from project root
docker build -t iudx/rs-proxy-depl:latest -f docker/depl.dockerfile .
docker build -t iudx/rs-proxy-dev:latest -f docker/dev.dockerfile .
