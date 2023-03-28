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