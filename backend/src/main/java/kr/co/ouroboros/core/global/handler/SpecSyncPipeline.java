package kr.co.ouroboros.core.global.handler;

import kr.co.ouroboros.core.global.spec.OuroApiSpec;

public interface SpecSyncPipeline {
    /**
 * Validates and reconciles an API specification from a file with a scanned (discovered) specification.
 *
 * @param fileSpec    the specification loaded from a file, used as the authoritative or baseline spec
 * @param scannedSpec the specification obtained from scanning or discovery, used to validate and update the baseline
 * @return the resulting {@code OuroApiSpec} after validation and reconciliation of differences between the two inputs
 */
OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec);
}