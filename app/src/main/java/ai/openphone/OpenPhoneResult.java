package ai.openphone;

/**
 * Mock implementation of OpenPhoneResult
 */
public class OpenPhoneResult {
    private final String response;
    
    public OpenPhoneResult(String response) {
        this.response = response;
    }
    
    public String getResponse() {
        return response;
    }
}