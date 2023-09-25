package side.side.service.data;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import side.side.domain.data.*;
import side.side.exception.data.ApiFailureException;
import side.side.factory.ArticleFactory;
import side.side.mapper.data.ArticleMapper;
import side.side.util.Util;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ArticleService {

    private static final Logger log = LogManager.getLogger(ArticleService.class);
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
    private final Map<Integer, CompanySearchParam> paramMap = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> failMap = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<CompanySearchParam> failQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CompanySearchParam> workQueue = new ConcurrentLinkedQueue<>();

    private final NaverService naverService;
    private final BigkindsService bigkindsService;
    private final ArticleFactory articleFactory;
    private final ArticleMapper articleMapper;

    private final int retryNumber = 0;

    public ArticleService(NaverService naverService, BigkindsService bigkindsService, ArticleFactory articleFactory, ArticleMapper articleMapper) {
        this.naverService = naverService;
        this.bigkindsService = bigkindsService;
        this.articleFactory = articleFactory;
        this.articleMapper = articleMapper;
    }

    public void newsFetch(CompanySearchParam companySearchParam, Integer maxSize) {

        final String keyword = buildSearchKeyword(companySearchParam, ArticleSource.NAVER);
        try {
            naverService.fetch(keyword, maxSize, 1, NaverService.Sort.DATE);
        } catch (Exception e) {
            addToWorkQueue(companySearchParam);
            throw new ApiFailureException(e.getMessage());
        }
        addToWorkQueue(companySearchParam);
    }

    public List<Article> search(CompanySearchParam companySearchParam, Integer maxSize) {

        final LocalDateTime articleUpdateDatetime = companySearchParam.getArticleUpdateDatetime();
        final String keyword = buildSearchKeyword(companySearchParam, ArticleSource.NAVER);
        List<Article> articleList;

        try {
            articleList = naverService.fetch(keyword, maxSize, 1, NaverService.Sort.DATE).getArticleList().parallelStream()
                    .map(article -> articleFactory.createArticle(article, companySearchParam.getIdSeq(), 1))
                    .filter(article -> Util.isEmpty(articleUpdateDatetime) || !articleUpdateDatetime.isAfter(article.getPublishDatetime()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            addToWorkQueue(companySearchParam);
            throw new ApiFailureException(e.getMessage());
        }

        addToWorkQueue(companySearchParam);

        log.info(String.format("요청에 대하여 네이버 뉴스 %d 건과 함께 응답합니다.", articleList.size()));
        return articleList;
    }

    public static String buildSearchKeyword(CompanySearchParam companySearchParam, ArticleSource articleSource) {
        switch (articleSource) {
            case NAVER:
                return String.format("\"%s\" +%s", companySearchParam.getCompanyName(), companySearchParam.getCeoName());
            case BIGKINDS:
                return String.format("%s %s", companySearchParam.getCompanyName(), companySearchParam.getCeoName());
            default:
                throw new RuntimeException("등록되지 않은 ArticleSource 입니다.");
        }
    }

    @PostConstruct
    private void initWorker() {
        exec.scheduleWithFixedDelay(() -> {
            if (!workQueue.isEmpty()) {
                CompanySearchParam param = workQueue.poll();
                work(param);
            } else {
                if (!failQueue.isEmpty()) {
                    CompanySearchParam param = failQueue.poll();
                    work(param);
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void addToWorkQueue(CompanySearchParam param) {

        // 해당기업이 워크 큐에도 없고 실패 큐에도 없는 경우
        if (!workQueue.contains(param)) {
            if (failQueue.contains(param)) {
                failMap.put(param.getIdSeq(), failMap.get(param.getIdSeq()) - 1);
                failQueue.remove(param);
            }

            if (!paramMap.containsKey(param.getIdSeq())) {
                log.info(String.format("%d 식별번호 기업에 대한 크롤링 작업을 큐에 넣습니다.", param.getIdSeq()));
                workQueue.add(param);
            } else log.info(String.format("%d 식별번호에 대하여 크롤링 중입니다.", param.getIdSeq()));
        } else log.info(String.format("%d 식별번호 기업이 이미 큐에 있습니다.", param.getIdSeq()));
    }

    private void work(CompanySearchParam param) {
        log.info(String.format("워크 큐 %d개 일 남음, 실패 큐 %d개 일 남음", workQueue.size(), failQueue.size()));

        try {
            paramMap.put(param.getIdSeq(), param);
            log.info(String.format("%d 식별번호 기업에 대한 크롤링을 시작합니다.", param.getIdSeq()));
            process(param);
        } catch (Exception e) {
            log.warn(String.format("%d 식별번호 기업에 대한 크롤링을 실패했습니다. 에러: %s", param.getIdSeq(), e));
            paramMap.remove(param.getIdSeq());

            Integer value = failMap.getOrDefault(param.getIdSeq(), 0);

            if (value < retryNumber) {
                if (!workQueue.contains(param) && !failQueue.contains(param)) {
                    if (!paramMap.containsKey(param.getIdSeq())) {
                        log.info(String.format("%d 번째 재시도하기 위해 큐에 넣습니다.", value + 1));
                        failMap.put(param.getIdSeq(), value + 1);
                        failQueue.add(param);
                    } else log.info(String.format("%d 식별번호에 대하여 크롤링 중입니다.", param.getIdSeq()));
                } else log.info(String.format("%d 식별번호 기업이 이미 큐에 있습니다.", param.getIdSeq()));
            } else {
                log.error("재시도 회수를 초과했습니다.");
            }
        }
    }

    private void process (CompanySearchParam companySearchParam) {

        final int idSeq = companySearchParam.getIdSeq();
        final LocalDateTime minDate = companySearchParam.getArticleUpdateDatetime();

        final List<String> keywords = new ArrayList<>();
        if (Util.isNotEmpty(companySearchParam.getCompanyName()) && Util.isNotEmpty(companySearchParam.getCeoName())) {
            keywords.add(String.format("\"%s\" +%s", companySearchParam.getCompanyName(), companySearchParam.getCeoName()));
            keywords.add(String.format("%s %s", companySearchParam.getCompanyName(), companySearchParam.getCeoName()));
        } else {
            keywords.add(null);
            keywords.add(null);
        }

        if (Util.isNotEmpty(companySearchParam.getCompanyName()))
            keywords.add(String.format("\"%s\"", companySearchParam.getCompanyName()));
        else keywords.add(null);
        if (Util.isNotEmpty(companySearchParam.getCeoName()))
            keywords.add(String.format("\"%s\"", companySearchParam.getCeoName()));
        else keywords.add(null);

        int keywordDepth = companySearchParam.getKeywordDepth();
        IntStream.range(0, keywordDepth)
                .forEach(priority -> {
                    try {
                        // 1 -> 4 순으로 가져와서 바로 저장, 동일 뉴스에 대하여 최 우선순위 외에는 중복 제거 될 것임.
                        if (keywords.get(priority) != null) {
                            // fetch
                            List<NaverArticle> naverArticleList = naverService.searchByMinDate(keywords.get(priority), minDate);
                            List<Article> articleList = naverArticleList.stream()
                                    .map(a -> articleFactory.createArticle(a, idSeq, priority + 1))
                                    .filter(a -> Util.isNotEmpty(a.getLink()))
                                    .collect(Collectors.toList());
                            // save
                            addArticleList( articleList );
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        List<BigkindsArticle> bigkindsArticleList = bigkindsService.searchByMinDate(buildSearchKeyword(companySearchParam, ArticleSource.BIGKINDS), minDate);
        List<Article> articleList = bigkindsArticleList.stream()
                .map(a -> articleFactory.createArticle(a, idSeq))
                .filter(a -> Util.isNotEmpty(a.getLink()) && Util.isNotEmpty(a.getPublisher()))
                .collect(Collectors.toList());
        // save
        addArticleList( articleList );
        log.info("빅카인즈와 네이버 뉴스 저장을 완료했습니다.");

        // 최신 업데이트 날짜 갱신
        articleMapper.updateArticleUpdateDate(idSeq, LocalDateTime.now());
        paramMap.remove(idSeq);
    }

    private void addArticleList( List<Article> articleList ) {
        if (Util.isNotEmpty(articleList)) {
            if(articleList.size() <= 100) {
                articleMapper.addArticleList(articleList);
            } else {
                List<List<Article>> subPartList = Lists.partition(articleList, 100);
                for( List<Article> list : subPartList ) {
                    articleMapper.addArticleList(list);
                }
            }
        }
    }





}