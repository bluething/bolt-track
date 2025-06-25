# Scalable Tracking Number Generator API

### Functional Requirements
* The generated tracking number must satisfy the following:  
    - It must match the regex pattern: `^[A-Z0-9]{1,16}$.`  
    - It must be unique; no duplicate tracking numbers should be generated.  
    - The generation process should be efficient.  
    - The system should be scalable, capable of handling concurrent requests, and should be able to scale horizontally.  
* The API should return a JSON object containing at least the following fields:  
    - `tracking_number`: The generated tracking number.  
    - `created_at`: The timestamp when the tracking number was generated (in RFC 3339 format).  
    - You may include additional fields in the response if you see fit.

### Non-Functional Requirements  

* Efficiency & concurrency: The solution should be optimized for high  performance and be capable of handling multiple concurrent requests without degradation.  
* Scalability: The solution should be designed to scale horizontally across multiple instances.