package side.side.service.data;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import side.side.config.pool.HttpConnectionPool;
import side.side.config.props.Props;
import side.side.domain.data.NaverArticle;
import side.side.domain.data.NaverResultWrapper;
import side.side.exception.data.ApiFailureException;
import side.side.util.Util;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class NaverService {

    private final Logger log = LogManager.getLogger(NaverService.class);

    private final HttpConnectionPool httpPool;

    private final Props props;

    public NaverService(HttpConnectionPool httpPool, Props props) {
        this.httpPool = httpPool;
        this.props = props;
    }

    public enum Sort {
        SIM("sim"), DATE("date");

        private final String name;

        Sort(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }
    }

    AtomicLong KEY_COUNT = new AtomicLong(1);

    public NaverResultWrapper fetch(String keyword, Integer display, Integer start, Sort sort) {
        try {
            final URIBuilder uriBuilder = new URIBuilder(props.getApiNaverPath());

            uriBuilder.addParameter("query", keyword);

            uriBuilder.addParameter("display", Integer.toString(display));

            uriBuilder.addParameter("start", Integer.toString(start));

            uriBuilder.addParameter("sort", sort.toString());

            org.springframework.http.HttpHeaders headers = new HttpHeaders();
            if (KEY_COUNT.get() <= 25000) {
                headers.set("X-Naver-Client-Id", "4YiTysikkf5h22QoZZB7");
                headers.set("X-Naver-Client-Secret", "IAjxIy2K0n");
            } else if (KEY_COUNT.get() <= 50000) {
                headers.set("X-Naver-Client-Id", "326CrAi_Bren51Q8ahaN");
                headers.set("X-Naver-Client-Secret", "rv26VIZwOj");
            } else if (KEY_COUNT.get() <= 75000) {
                headers.set("X-Naver-Client-Id", "Tku_BTHiqsyRWpRRNcDH");
                headers.set("X-Naver-Client-Secret", "UuQKov_4NP");
            } else if (KEY_COUNT.get() <= 100000) {
                headers.set("X-Naver-Client-Id", "g8mt0jGgrsZUFP27oqPJ");
                headers.set("X-Naver-Client-Secret", "LK7vBXyHDX");
            } else if (KEY_COUNT.get() <= 125000) {
                headers.set("X-Naver-Client-Id", "4YiTysikkf5h22QoZZB7");
                headers.set("X-Naver-Client-Secret", "IAjxIy2K0n");
            } else if (KEY_COUNT.get() <= 150000) {
                headers.set("X-Naver-Client-Id", "RScx0eObPOQVagyKUGCz");
                headers.set("X-Naver-Client-Secret", "JO4sXHh50n");
            } else if (KEY_COUNT.get() <= 175000) {
                headers.set("X-Naver-Client-Id", "4_f3MwdeFbq5a6HWWja0");
                headers.set("X-Naver-Client-Secret", "g0RP27dSJ9");
            } else if (KEY_COUNT.get() <= 200000) {
                headers.set("X-Naver-Client-Id", "UOT7OWV60TADPIrfj7G3");
                headers.set("X-Naver-Client-Secret", "5gsf8SqOD8");
            } else if (KEY_COUNT.get() <= 225000) {
                headers.set("X-Naver-Client-Id", "VzZeJnYlCkArBX1dyZIL");
                headers.set("X-Naver-Client-Secret", "ghiKNNiSJz");
            } else if (KEY_COUNT.get() <= 250000) {
                headers.set("X-Naver-Client-Id", "UQNTZnGt0LSmCl3_HP1H");
                headers.set("X-Naver-Client-Secret", "k8iyrnh_G9");
            }
            log.info("키 사용 횟수: {}", KEY_COUNT.get());

            final NaverResultWrapper nrw = httpPool.getShortWaitRestTemplate().exchange(
                    uriBuilder.build(), HttpMethod.GET, new HttpEntity<>(headers), NaverResultWrapper.class
            ).getBody();

            if (Util.isNotEmpty(nrw) && Util.isNotEmpty(Objects.requireNonNull(nrw).getErrorCode())) {
                throw new ApiFailureException(nrw.getErrorMessage());
            }

            KEY_COUNT.incrementAndGet();

            return nrw;
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("URI 빌드에 실패했습니다. %s", e));
        }
    }


    public List<NaverArticle> searchByMinDate(String keyword, LocalDateTime minDate) throws InterruptedException {

        List<NaverArticle> accumulatedArticleList = new ArrayList<>();
        int start = 1;
        LocalDateTime tempMinDate = null;
        List<NaverArticle> articleList = null;

        while (articleList == null || (start <= 1000 && articleList.size() >= 100 && (Util.isEmpty(tempMinDate) || Util.isEmpty(minDate) || Objects.requireNonNull(tempMinDate).isAfter(minDate)))) {
            NaverResultWrapper nrw = fetch(keyword, 100, start, Sort.DATE);

            // map filter
            articleList = nrw.getArticleList().stream()
                    // .map(na -> ArticleFactory.createArticle(na, idSeq, priority))
                    // minDate 보다 이후에 일어난 일 (최근)
                    .filter(
                            a -> Util.isEmpty(minDate) || ZonedDateTime.parse(a.getPubDate(), DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime().isAfter(minDate))
                    .collect(Collectors.toList());

            // accumulate
            accumulatedArticleList.addAll(articleList);

            // update
            if (articleList.size() > 0) {
                tempMinDate = ZonedDateTime.parse(articleList.get(articleList.size() - 1).getPubDate(), DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
                start = nrw.getStart() + nrw.getDisplay();
            }

            // TODO: schedule
            // avoid request exceed to Naver API
            Thread.sleep(150);
        }

        log.info("네이버 뉴스 키워드[{}] 기사 갯수 : {}", keyword, accumulatedArticleList.size());
        return accumulatedArticleList;
    }
}
