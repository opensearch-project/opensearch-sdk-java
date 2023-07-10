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