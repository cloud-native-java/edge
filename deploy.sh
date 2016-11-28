#!/usr/bin/env bash

set -e

source $BUILD_DIRECTORY/utils/cf-common.sh

mvn -DskipTests clean install

as=auth-service
res=service-registry
gs=greetings-service
gc=edge-service
h5=html5-client

cf d -f $res
cf d -f $as
cf d -f $gs
cf d -f $gc
cf d -f $h5

# EUREKA SERVICE
cf ds -f $res
cf a | grep $res && cf d -f $res
deploy_app $res
deploy_service $res

# AUTH SERVICE
as_db=auth-service-pgsql

cf ds -f $as
cf d -f $as
cf ds -f ${as_db}

cf cs elephantsql turtle ${as_db}

deploy_app $as
deploy_service $as

## GREETINGS SERVICE
cf d -f $gs
deploy_app $gs

## GREETINGS CLIENT
cf d -f $gc
deploy_app $gc

## HTML5 CLIENT
cf d -f $h5
deploy_app $h5
