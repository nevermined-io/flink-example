package io.keyko.nevermined.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.keyko.common.helpers.EthereumHelper;
import io.keyko.common.helpers.HttpHelper;
import io.keyko.common.models.HttpResponse;
import io.keyko.nevermined.api.NeverminedAPI;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.Config;
import io.keyko.nevermined.exceptions.*;
import io.keyko.nevermined.external.GatewayService;
import io.keyko.nevermined.models.DDO;
import io.keyko.nevermined.models.DID;
import io.keyko.nevermined.models.asset.AssetMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import io.keyko.nevermined.models.asset.OrderResult;
import io.keyko.nevermined.models.service.ProviderConfig;
import io.keyko.nevermined.models.service.Service;
import io.keyko.nevermined.models.service.types.ComputingService;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

public class Demo {

    public NeverminedAPI neverminedAPI;
    public ProviderConfig providerConfig;

    public static void main(String [] args) throws Exception {
        // Setup Nevermined
        Demo demo = new Demo();

        // Execute the Demo and start the Flink compute job
        String executionId = demo.run();

        // Monitor the execution of the compute job
        System.out.println("Waiting for compute job...");
        DID didOutput;
        try {
            didOutput = monitorComputeJob(executionId);
        } catch (Exception e) {
            System.out.println("Compute job has failed!: " + e.getMessage());
            throw e;
        }
        System.out.println("Compute job finished successfully!\n");
        System.out.println("Downloading file(s) for asset: " + didOutput.toString());
        Boolean downloaded = demo.download(didOutput, "./downloads");
        System.out.println("File(s) successfully downloaded");
    }

    public String run() throws Exception {

        // create compute to the data asset
        String computingJson = "src/main/resources/computing-provider-example.json";
        String computingJsonContent = new String(Files.readAllBytes(Paths.get(computingJson)));
        ComputingService.Provider computingProvider = DDO.fromJSON(new TypeReference<>() {
        }, computingJsonContent);
        String metadataJson = "src/main/resources/metadata.json";
        String metadataJsonContent = new String(Files.readAllBytes(Paths.get(metadataJson)));
        AssetMetadata metadata = DDO.fromJSON(new TypeReference<>() {
        }, metadataJsonContent);
        metadata.attributes.main.dateCreated = new Date();

        // publish compute to the data asset
        DDO ddoCompute = neverminedAPI.getAssetsAPI().createComputeService(metadata, providerConfig, computingProvider);
        DID didCompute = new DID(ddoCompute.id);
        System.out.println("[DATA_PROVIDER --> NEVERMINED] Publishing compute to the data asset: " + didCompute.did);

        /// create algorithm asset
        String algorithmJson = "src/main/resources/metadata-algorithm.json";
        String algorithmJsonContent = new String(Files.readAllBytes(Paths.get(algorithmJson)));
        AssetMetadata metadataAlgorithm = DDO.fromJSON(new TypeReference<>() {
        }, algorithmJsonContent);
        metadataAlgorithm.attributes.main.dateCreated = new Date();

        // publish the algorithm
        DDO ddoAlgorithm = neverminedAPI.getAssetsAPI().create(metadataAlgorithm, providerConfig);
        DID didAlgorithm = new DID(ddoAlgorithm.id);
        System.out.println("[DATA_CONSUMER --> NEVERMINED] Publishing algorithm asset: " + didAlgorithm.did);

        // create workflow
        String workflowJson = "src/main/resources/metadata-workflow.json";
        String workflowJsonContent = new String(Files.readAllBytes(Paths.get(workflowJson)));
        AssetMetadata metadataWorkflow = DDO.fromJSON(new TypeReference<>() {
        }, workflowJsonContent);

        metadataWorkflow.attributes.main.dateCreated = new Date();
        metadataWorkflow.attributes.main.workflow.stages.get(0).input.get(0).id = didCompute;
        metadataWorkflow.attributes.main.workflow.stages.get(0).transformation.id = didAlgorithm;

        // publish workflow
        DDO ddoWorkflow = neverminedAPI.getAssetsAPI().create(metadataWorkflow, providerConfig);
        DID didWorkflow = new DID(ddoWorkflow.id);
        System.out.println("[DATA_CONSUMER --> NEVERMINED] Publishing compute workflow: " + didWorkflow.did);

        // Order compute to the data
        OrderResult orderResult = neverminedAPI.getAssetsAPI().orderDirect(didCompute, Service.DEFAULT_COMPUTE_INDEX);
        System.out.println("[DATA_CONSUMER --> DATA_PROVIDER] Requesting an agreement for compute to the data: "
                + orderResult.getServiceAgreementId());

        // Execute Workflow
        GatewayService.ServiceExecutionResult executionResult = neverminedAPI.getAssetsAPI().execute(
                EthereumHelper.add0x(orderResult.getServiceAgreementId()),
                didCompute,
                Service.DEFAULT_COMPUTE_INDEX,
                didWorkflow);
        System.out.println("[DATA_CONSUMER --> DATA_PROVIDER] Requesting execution for compute to the data: "
                + executionResult.getExecutionId());

        return executionResult.getExecutionId();
    }

    public Demo() throws InvalidConfiguration, InitializationException, EthereumException {
        System.out.println("Setting up...\n");
        Config config = ConfigFactory.defaultApplication();
        neverminedAPI = NeverminedAPI.getInstance(config);

        String metadataUrl = config.getString("metadata-internal.url") + "/api/v1/metadata/assets/ddo/{did}";
        String provenanceUrl = config.getString("metadata-internal.url") + "/api/v1/metadata/assets/provenance/{did}";
        String gatewayUrl = config.getString("gateway.url");
        String consumeUrl = gatewayUrl + "/api/v1/gateway/services/access";
        String secretStoreEndpoint = config.getString("secretstore.url");
        String providerAddress = config.getString("provider.address");

        providerConfig = new ProviderConfig(consumeUrl, metadataUrl, gatewayUrl, provenanceUrl, secretStoreEndpoint, providerAddress);
        String computeServiceEndpoint = config.getString("gateway.url") + "/api/v1/gateway/services/execute";
        providerConfig.setExecuteEndpoint(computeServiceEndpoint);

        neverminedAPI.getTokensAPI().request(BigInteger.TEN);
    }

    /**
     * This is just a temporary solution calling the compute api directly to monitor the status.
     * This should be replaced once that functionality is on the sdk
     *
     * @param executionId
     * @return
     * @throws Exception
     */

    public static DID monitorComputeJob(String executionId) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        while (true) {
            Thread.sleep(5*1000);

            HttpResponse httpResponse = HttpHelper.httpClientGet("http://localhost:8050/api/v1/nevermined-compute-api/status/" + executionId);
            if (httpResponse.getStatusCode() != 200) {
                System.out.println(httpResponse.getBody());
                throw new Exception("Failed to get job status from compute api: " + httpResponse.getStatusCode());
            }

            JsonNode status = mapper.readTree(httpResponse.getBody());
            String jobStatus = status.get("status").asText();
            System.out.println(jobStatus);

            if (jobStatus.equals("Failed")) {
                throw new Exception("Compute job: " + executionId + " failed");
            } else if (jobStatus.equals("Succeeded")) {
                DID did = new DID(status.get("did").asText());
                return did;
            }
        }
    }

    public Boolean download(DID did, String basePath) {
        try {
            return neverminedAPI.getAssetsAPI().ownerDownload(did, Service.DEFAULT_ACCESS_INDEX, basePath);
        } catch (ServiceException | ConsumeServiceException e) {
            System.out.println("Failed to download files: " + e.getMessage());
            return false;
        }
    }
}
