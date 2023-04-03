# Certificate Generation

Use the script below to generate certificates for running an extension. 
The certificate for the extension needs to be in the same chain as the 
root certificate for the OpenSearch cluster defined in the `plugins.security.ssl.http.pemtrustedcas_filepath`
setting of `opensearch.yml`


## Certificate Generation Script

```
#! /bin/bash

openssl genrsa -out root-ca-key.pem 2048
openssl req -new -x509 -sha256 -key root-ca-key.pem -subj "/C=US/ST=NEW YORK/L=BROOKLYN/O=OPENSEARCH/OU=SECURITY/CN=ROOT" -out root-ca.pem -days 730

openssl genrsa -out extension-01-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in extension-01-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out extension-01-key.pem
openssl req -new -key extension-01-key.pem -subj "/C=US/ST=NEW YORK/L=BROOKLYN/O=OPENSEARCH/OU=SECURITY/CN=extension-01" -out extension-01.csr
echo 'subjectAltName=DNS:extension-01' | tee -a extension-01.ext
echo 'subjectAltName=IP:172.20.0.11' | tee -a extension-01.ext
openssl x509 -req -in extension-01.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out extension-01.pem -days 730 -extfile extension-01.ext

rm extension-01-key-temp.pem
rm extension-01.csr
rm extension-01.ext
rm root-ca.srl
```

## Certificate Generation Script for OpenSearch with single node and admin cert

```
#! /bin/bash

openssl genrsa -out admin-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in admin-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out admin-key.pem
openssl req -new -key admin-key.pem -subj "/C=US/ST=NEW YORK/L=BROOKLYN/O=OPENSEARCH/OU=SECURITY/CN=A" -out admin.csr
openssl x509 -req -in admin.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out admin.pem -days 730
openssl genrsa -out os-node-01-key-temp.pem 2048
openssl pkcs8 -inform PEM -outform PEM -in os-node-01-key-temp.pem -topk8 -nocrypt -v1 PBE-SHA1-3DES -out os-node-01-key.pem
openssl req -new -key os-node-01-key.pem -subj "/C=US/ST=NEW YORK/L=BROOKLYN/O=OPENSEARCH/OU=SECURITY/CN=os-node-01" -out os-node-01.csr
echo 'subjectAltName=DNS:os-node-01' | tee -a os-node-01.ext
echo 'subjectAltName=IP:172.20.0.11' | tee -a os-node-01.ext
openssl x509 -req -in os-node-01.csr -CA root-ca.pem -CAkey root-ca-key.pem -CAcreateserial -sha256 -out os-node-01.pem -days 730 -extfile os-node-01.ext

rm admin-key-temp.pem
rm admin.csr
rm os-node-01-key-temp.pem
rm os-node-01.csr
rm os-node-01.ext
rm root-ca.srl
```

## Install Security plugin and run in SSL only mode

To test an extension running with SSL and connected to an OpenSearch node with SSL you must install
the security plugin in the OpenSearch node and run it in SSL only mode.

Follow the steps below to test an extension running with TLS and connect to an OpenSearch node with 
the security plugin and SSL enabled:

1. Create a local distribution of [OpenSearch](https://github.com/opensearch-project/opensearch) and move the output to location you would like to install
OpenSearch into:

```
cd opensearch
./gradlew localDistro
mv distribution/archives/darwin-tar/build/install/opensearch-<OPENSEARCH_VERSION>-SNAPSHOT/ ~/opensearch
```

2. Assemble the [Security plugin](https://github.com/opensearch-project/security) and move the output to the same directory

```
cd security
./gradlew assemble
mv build/distributions/opensearch-security-<OPENSEARCH_VERSION>.0-SNAPSHOT.zip ~/opensearch
```

3. Install the Security plugin and associated certificates created above

3.1. Navigate to the root of the OpenSearch installation:

```
cd ~/opensearch/opensearch-<OPENSEARCH_VERSION>-SNAPSHOT/
./bin/opensearch-plugin install file:$HOME/opensearch/opensearch-security-<OPENSEARCH_VERSION>.0-SNAPSHOT.zip
```

3.2 Copy the certificates generated above into `config/` directory

The certificates needed are:

- `os-node-01.pem`
- `os-node-01-key.pem`
- `root-ca.pem`

3.3 Add settings in `opensearch.yml`

Add the following settings in `opensearch.yml`

```
opensearch.experimental.feature.extensions.enabled: true
plugins.security.ssl_only: true
plugins.security.ssl.transport.pemcert_filepath: os-node-01.pem
plugins.security.ssl.transport.pemkey_filepath: os-node-01-key.pem
plugins.security.ssl.transport.pemtrustedcas_filepath: root-ca.pem
plugins.security.ssl.transport.enforce_hostname_verification: false
plugins.security.ssl.http.enabled: true
plugins.security.ssl.http.pemcert_filepath: os-node-01.pem
plugins.security.ssl.http.pemkey_filepath: os-node-01-key.pem
plugins.security.ssl.http.pemtrustedcas_filepath: root-ca.pem
network.host: 0.0.0.0
```

4. Install certificates on extension

Installation for the OpenSearch node is now complete, the rest of the installation is for the extension.

Create a `config/` folder in the extension's home directory and install the certificates generated above.

The certificates you need to add are:

- `extension-01.pem`
- `extension-01-key.pem`
- `root-ca.pem`

4.1 Add references to these certifications in extension settings file and enable SSL

Add the following settings to the extension setting files. i.e. `helloworld-settings.yml`

```
ssl.transport.enabled: true
ssl.transport.pemcert_filepath: extension-01.pem
ssl.transport.pemkey_filepath: extension-01-key.pem
ssl.transport.pemtrustedcas_filepath: root-ca.pem
path.home: <path/to/extension>
```