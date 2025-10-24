package kr.co.ouroboros.core.rest.spec.service;

import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiRequest;
import kr.co.ouroboros.core.rest.spec.dto.CreateRestApiResponse;

public interface RestApiSpecService {
    CreateRestApiResponse createRestApiSpec(CreateRestApiRequest request) throws Exception;
}
