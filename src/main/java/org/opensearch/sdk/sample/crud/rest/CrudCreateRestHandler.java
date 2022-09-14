package org.opensearch.sdk.sample.crud.rest;

import jakarta.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.opensearch.action.index.IndexAction;
import org.opensearch.action.index.IndexRequestBuilder;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.indices.Alias;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.ExistsResponse;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.rest.RestRequest;
import org.opensearch.sdk.ExtensionRestResponse;
import org.opensearch.sdk.NamedWriteableRegistryAPI;
import org.opensearch.sdk.SDKClient;
import org.opensearch.sdk.handlers.ExtensionRouteRequestHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opensearch.rest.RestStatus.OK;

public class CrudCreateRestHandler implements ExtensionRouteRequestHandler {
    private final Logger logger = LogManager.getLogger(CrudCreateRestHandler.class);

    private static final String CREATE_SUCCESS = "PUT /create successful, detector created";
    private static final String EXAMPLE_DETECTOR = null;

    private static final String SAMPLE_DOCUMENT = "{\n" +
            "   \"name\": {\n" +
            "      \"first name\": \"Steve\",\n" +
            "      \"last name\": \"Jobs\"\n" +
            "   }\n" +
            "}";

    @Override
    public ExtensionRestResponse handleRequest(RestRequest.Method method, String uri) {
        logger.info("CrudCreateRestHandler.handleRequest");
        SDKClient sdkClient = new SDKClient();
        try {
            OpenSearchClient client = sdkClient.initializeClient("localhost", 9200);
            logger.info("Checking to see if detectors index exists");
            BooleanResponse er = client.indices().exists(new ExistsRequest.Builder().index("detectors").build());
            logger.info("Detectors index exists: " + er.value());
            if (!er.value()) {
                logger.info("Creating detectors index");
                CreateIndexResponse cir = client.indices().create(new CreateIndexRequest.Builder().index("detectors").build());
                logger.info("Create index response: " + cir.acknowledged());
            }
            logger.info("Creating document");
            Document doc = new Document();
            doc.add(new StringField("field", "value", Field.Store.YES));
            IndexResponse ir = client.index(new IndexRequest.Builder().index("detectors").document(doc).build());
            logger.info("IndexResponse: " + ir.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ExtensionRestResponse(OK, CREATE_SUCCESS, List.of());
    }
}
