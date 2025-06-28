package io.github.bluething.java.bolttrack.exception;

public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resourceName, String identifier) {
        super(ErrorCode.NOT_FOUND, String.format("Resource %s not found with identifier %s", resourceName, identifier));
    }
}
