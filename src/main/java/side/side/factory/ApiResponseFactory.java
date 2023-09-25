package side.side.factory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import side.side.domain.ApiResponse;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ApiResponseFactory {

    private final List<HttpStatus> SUCCESS_STATUS_LIST = Stream.of(HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED).collect(Collectors.toList());

    public ResponseEntity<ApiResponse> createApiResponse(ApiResponse apiResponse, HttpStatus status) {
        if(SUCCESS_STATUS_LIST.contains(status)) {
            apiResponse.setSuccess(true);
        }
        return new ResponseEntity<ApiResponse>(apiResponse, status);
    }
}
