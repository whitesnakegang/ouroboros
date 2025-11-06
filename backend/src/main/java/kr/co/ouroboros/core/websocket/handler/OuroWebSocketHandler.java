package kr.co.ouroboros.core.websocket.handler;

import kr.co.ouroboros.core.global.Protocol;
import kr.co.ouroboros.core.global.handler.OuroProtocolHandler;
import kr.co.ouroboros.core.global.spec.OuroApiSpec;

public class OuroWebSocketHandler implements OuroProtocolHandler {
    
    @Override
    public Protocol getProtocol() {
        return null;
    }
    
    @Override
    public String getSpecFilePath() {
        return null;
    }
    
    @Override
    public OuroApiSpec scanCurrentState() {
        return null;
    }
    
    @Override
    public OuroApiSpec loadFromFile(String yamlContent) {
        return null;
    }
    
    @Override
    public OuroApiSpec synchronize(OuroApiSpec fileSpec, OuroApiSpec scannedSpec) {
        return null;
    }
    
    @Override
    public void saveYaml(OuroApiSpec specToSave) {
    
    }
}
