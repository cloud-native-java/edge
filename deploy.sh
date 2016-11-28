#!/usr/bin/env bash

set -e

source $BUILD_DIRECTORY/utils/cf-common.sh

mvn -DskipTests clean install


# EUREKA SERVICE
res=service-registry
cf ds -f $res
cf a | grep $res && cf d -f $res
deploy_app $res
deploy_service $res

# AUTH SERVICE
as=auth-service
cf ds -f $as
cf d -f $as
deploy_app $as
deploy_service $as

# GREETINGS SERVICE
gs=greetings-service
cf d -f $gs
deploy_app $gs

# GREETINGS CLIENT
gc=edge-service
cf d -f $gc
deploy_app $gc

# HTML5 CLIENT
h5=html5-client
cf d -f $h5
deploy_app $h5
