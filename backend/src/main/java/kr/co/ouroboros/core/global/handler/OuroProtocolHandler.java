package kr.co.ouroboros.core.global.handler;

import kr.co.ouroboros.core.global.spec.OuroApiSpec;

public interface OuroProtocolHandler {
    /**
 * Protocol key that this handler processes (for example "rest" or "grpc").
 *
 * @return the unique protocol name handled by this handler
 */
    String getProtocol();

    /**
 * Get the file path to the API specification used by this protocol handler.
 *
 * @return the file system path or classpath location of the Ouro API specification YAML for this protocol
 */
String getSpecFilePath();

    /**
 * Produce the current API specification by scanning code annotations.
 *
 * @return the {@code OuroApiSpec} representing the API specification discovered in the codebase via annotations
 */
    OuroApiSpec scanCurrentState();

    /**
 * Parse YAML content into the saved (desired) API specification.
 *
 * @param yamlContent the full contents of a YAML (.yml) file representing an Ouro API specification
 * @return the parsed saved (desired) API specification as an {@code OuroApiSpec}
 */
    OuroApiSpec loadFromFile(String yamlContent);

    /**
 * Compare a persisted API spec with the scanned current API spec and produce a validation spec that describes their discrepancies.
 *
 * @param fileSpec    the API specification loaded from the persisted file (desired state)
 * @param scannedSpec the API specification produced by scanning the current codebase (actual state)
 * @return            an OuroApiSpec representing validation results—detailing differences between the provided fileSpec and scannedSpec
 */
    OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec);

    /**
 * Serialize the given OuroApiSpec into a YAML-formatted string for persistence.
 *
 * @param specToSave the API specification to serialize (typically a scanned or merged spec)
 * @return the YAML representation of the provided specification
 */
    String serializeToYaml(OuroApiSpec specToSave);
}