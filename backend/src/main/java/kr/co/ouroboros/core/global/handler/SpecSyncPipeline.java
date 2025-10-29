package kr.co.ouroboros.core.global.handler;

import kr.co.ouroboros.core.global.spec.OuroApiSpec;

public interface SpecSyncPipeline {
    OuroApiSpec validate(OuroApiSpec fileSpec, OuroApiSpec scannedSpec);
}
