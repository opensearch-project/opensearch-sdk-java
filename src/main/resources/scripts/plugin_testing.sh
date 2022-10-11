#!/bin/bash

## Script to calculate the average performance of creating detector api
count=$1

for p in $*;
do
echo "------------"
let i=$count-1
tot=0
while [ $i -ge 0 ];
do
res=`curl -w "$i: %{time_total} %{http_code} %{size_download} %{url_effective}\n" --location --request POST 'http://localhost:5601/api/anomaly_detectors/detectors' \
--header 'Cookie: security_authentication=Fe26.2**8d2be269f3e27e62b52420bb4658d58d987c099e2753c43c44f50449b5196414*LdPlLCuNd4rqMTCevRNHMQ*hjgYZ629nnIIKNw3r31KlMbCIS9azZKhuWsGHEYJOlbaZs3eOwtdslKMeIUR1KylieB39cxByqXEpeqSmrtLp6VLZABWGa-b4hO3bW11VSp_OyOWnORk7lDSHedbUplbxtKMaTXgIsJFT8gNvNBM6DeNEPqk8V2naYcW43lbslj3cE2PoSHwlMekguHQSojXFA2x-paKnEgPOiiXsoX9EFrkYLj_ZlnHyjkz9YZKsew**032e9b7b5965967558c0c31b0b94301df82b2111159de5db435f954e86699091*NnIB4AHDYcnhBoG085xZ5lYGD_E0dExvFSXqdw2FYcs' \
--header 'osd-version: 2.1.0' \
--header 'Content-Type: application/json' \
--data-raw '{"name":"demo_'$i'","description":"","indices":["security-auditlog-2022.10.05"],"filterQuery":{"match_all":{}},"uiMetadata":{"features":{"demo-feature":{"featureType":"simple_aggs","aggregationBy":"sum","aggregationOf":"audit_format_version"}},"filters":[]},"featureAttributes":[{"featureName":"demo-feature","featureEnabled":true,"importance":1,"aggregationQuery":{"demo_feature":{"sum":{"field":"audit_format_version"}}}}],"timeField":"@timestamp","detectionInterval":{"period":{"interval":10,"unit":"Minutes"}},"windowDelay":{"period":{"interval":1,"unit":"Minutes"}},"shingleSize":8,"categoryField":["audit_cluster_name.keyword","audit_node_name.keyword"],"detectionDateRange":{"startTime":1662840535386,"endTime":1665432535386}}' `
val=`echo $res | cut -f2 -d' '`
tot=`echo "scale=3;${tot}+${val}" | bc`
echo $res
let i=i-1
sleep 0.001
done

avg=`echo "scale=3; ${tot}/${count}" |bc`
echo "   ........................."
echo "   AVG: $tot/$count = $avg"
done