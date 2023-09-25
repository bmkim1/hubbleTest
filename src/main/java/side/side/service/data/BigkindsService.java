package side.side.service.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import side.side.config.pool.HttpConnectionPool;
import side.side.config.props.Props;
import side.side.domain.data.BigKindsRequestParam;
import side.side.domain.data.BigKindsResultWrapper;
import side.side.domain.data.BigkindsArticle;
import side.side.exception.data.ApiFailureException;
import side.side.util.Util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BigkindsService {

    public static final int FAILURE_RESULT_VALUE = -1;
    private final HttpConnectionPool httpPool;

    private final Props props;

    private static final Logger log = LogManager.getLogger(BigkindsService.class);

    public BigkindsService(HttpConnectionPool httpPool, Props props) {
        this.httpPool = httpPool;
        this.props = props;
    }

    public List<BigkindsArticle> searchByMinDate(String keyword, LocalDateTime minDateTime) {
        log.info(String.format("Bigkinds search keyword %s", keyword));

        final String baseUrl = props.getApiBigkindsPath();
        final String accessKey = props.getApiBigkindsAccessKey();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Referer", "https://www.bigkinds.or.kr/v2/news/search.do");

        BigKindsRequestParam bigKindsRequestParam = new BigKindsRequestParam(keyword);
        if (Util.isNotEmpty(minDateTime)) {
            // API 는 타임을 받지 않음
            LocalDate minDate = minDateTime.toLocalDate();
            bigKindsRequestParam.setStartDate(minDate);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("access_key", accessKey);
        map.put("argument", bigKindsRequestParam);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(map, headers);

        BigKindsResultWrapper bkRstWrp = httpPool.getLongWaitRestTemplate()
                .postForEntity(baseUrl, request, BigKindsResultWrapper.class).getBody();

        if (Util.isEmpty(bkRstWrp) || Objects.requireNonNull(bkRstWrp).getResult() == FAILURE_RESULT_VALUE) {
            throw new ApiFailureException();
        }

        log.info("빅카인즈 키워드[{}] 기사 갯수 : {}", keyword, bkRstWrp.getBigkindsResult().getCount());

        return  bkRstWrp.getBigkindsResult().getBigkindsArticles();
    }
}
