#!/bin/bash
##===========SETUP===============##
sudo yum install -y java-17-amazon-corretto-devel && sudo yum -y update && sudo yum -y install docker && sudo yum -y install git && sudo yum remove -y python3.7 && sudo yum update -y && sudo yum groupinstall "Development Tools" -y && sudo yum erase openssl-devel -y && sudo yum install openssl11 openssl11-devel libffi-devel bzip2-devel wget -y
sudo service docker start
wget https://www.python.org/ftp/python/3.9.9/Python-3.9.9.tgz
tar -xf Python-3.9.9.tgz
cd Python-3.9.9/
./configure --enable-optimizations
make -j $(nproc)
sudo make altinstall && sudo yum install -y python3-pip
cd ~
sudo update-alternatives --install /usr/bin/python3 python3 /usr/local/bin/python3.9 1

##===========OpenSearch===============##
git clone https://github.com/opensearch-project/OpenSearch.git
cd OpenSearch
git checkout feature/extensions
./gradlew clean && ./gradlew publishToMavenLocal && ./gradlew localDistro
cd ~

##===========OpenSearch-SDK-Java===============##
git clone https://github.com/opensearch-project/opensearch-sdk-java.git
cd opensearch-sdk-java
./gradlew clean && ./gradlew publishToMavenLocal
cd ~

##===========Anomaly Detection===============##
git clone https://github.com/opensearch-project/anomaly-detection.git
cd anomaly-detection
./gradlew check
nohup ./gradlew run >/home/ec2-user/AnomalyDetectionExtensionOutput.log 2>&1 &
cd ~

##===========Starting OpenSearch===============##
cd OpenSearch/distribution/archives/linux-tar/build/install/opensearch-3.0.0-SNAPSHOT/
mkdir extensions
cd extensions
{
 echo “
extensions:
  - name: ad-extension
    uniqueId: ad-extension
    hostName: 'sdk_host'
    hostAddress: '127.0.0.1'
    port: '4532'
    version: '1.0'
    description: Extension for the Opensearch SDK Repo
    opensearchVersion: '3.0.0'
    javaVersion: '14'
    className: ExtensionsRunner
    customFolderName: opensearch-sdk-java
    hasNativeController: false    
  “
} > extension.yml
cd ..
nohup ./bin/opensearch >/home/ec2-user/OpenSearchOutput.log 2>&1 &
cd ~ 

##===========OpenSearch-Benchmark===============##
git clone https://github.com/opensearch-project/opensearch-benchmark.git
pip3 install --upgrade pip && pip3 install opensearch-benchmark
opensearch-benchmark execute_test --workload=nyc_taxis --test-mode --target-hosts=localhost:9200 --pipeline benchmark-only

##===========Cleanup=================##
sudo service docker stop

