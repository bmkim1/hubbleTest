package side.side.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import side.side.domain.ApiResponse;
import side.side.domain.data.CompanySearchParam;
import side.side.factory.ApiResponseFactory;
import side.side.service.data.ArticleService;
import side.side.util.Util;

import java.net.URISyntaxException;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/article")
public class ArticleRestController {

    private final ArticleService articleService;

    private final ApiResponseFactory apiResponseFactory;

    public ArticleRestController(ArticleService articleService, ApiResponseFactory apiResponseFactory) {
        this.articleService = articleService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse> search(@RequestBody CompanySearchParam cp,
                                              @RequestParam(required = false)Optional<Integer> maxSize) throws URISyntaxException {

        if (Util.isEmpty(cp)) {
            throw new RuntimeException("error");
        }


        //EncryptionUtil.deckey(cp);;
        articleService.newsFetch(cp, maxSize.orElse(100));

        return apiResponseFactory.createApiResponse(ApiResponse.builder().build(), HttpStatus.OK);
    }
}
