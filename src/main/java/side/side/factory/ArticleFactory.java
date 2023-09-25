package side.side.factory;

import com.google.common.net.InternetDomainName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import side.side.domain.data.Article;
import side.side.domain.data.ArticleSource;
import side.side.domain.data.BigkindsArticle;
import side.side.domain.data.NaverArticle;
import side.side.mapper.data.ArticleMapper;
import side.side.util.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ArticleFactory {

    private final Logger log = LogManager.getLogger(ArticleFactory.class);

    final ArticleMapper articleMapper;

    public ArticleFactory(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public Article createArticle(NaverArticle naverArticle, Integer idSeq, Integer priority) {
        String publisher = null;
        String link = naverArticle.getLink();

        try {
            publisher = getPublisherByLink(link);
        } catch (MalformedURLException e) {
            log.warn(String.format("옳바르지 않은 url 입니다. %s", e));
            link = "";
        }

        return Article.of(
                null, idSeq, null, null,
                ArticleSource.NAVER,
                link,
                null,
                naverArticle.getTitle(),
                naverArticle.getDescription(),
                ZonedDateTime.parse(naverArticle.getPubDate(), DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime(),
                publisher,
                null,
                null,
                null,
                naverArticle.getOriginallink(),
                priority == 1,
                priority == 2,
                priority == 3,
                priority == 4
        );
    }

    public Article createArticle(BigkindsArticle bigkindsArticle, Integer idSeq) {
        String publisher = bigkindsArticle.getProvider();
        String link = bigkindsArticle.getProviderLinkPage();

        if (Util.isEmpty(publisher)) {
            try {
                publisher = getPublisherByLink(link);
            } catch (MalformedURLException e) {
                log.warn(String.format("옳바르지 않은 url 입니다. %s", e));
                link = "";
            }
        }

        return Article.of(
                null, idSeq, null, null,
                ArticleSource.BIGKINDS,
                link,
                bigkindsArticle.getNewsId(),
                bigkindsArticle.getTitle(),
                bigkindsArticle.getContent(),
                LocalDateTime.parse(bigkindsArticle.getDate(), DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                publisher,
                null,
                bigkindsArticle.getByLine(),
                bigkindsArticle.getImages(),
                null,
                true,
                false,
                false,
                false
        );
    }

    public String getPublisherByLink(String link) throws MalformedURLException {
        // ex. link: http://isplus.live.joins.com/news/article/article.asp?total_id=24003986&ctg=1401&tm=i_b&tag=
        // ex. host: isplus.live.joins.com
        // ex. domain: joins
        String hostName = new URL(link).getHost();
        String mainDomainName = getDomainByHost(hostName);
        String publisher = articleMapper.getPublisherByDomain(mainDomainName);

        if (Util.isEmpty(publisher)) {
            // 맵핑 테이블에 채워줌
            articleMapper.addDomainEmptyPublisher(hostName, mainDomainName);
        }

        return publisher;
    }

    public String getDomainByHost(String host) throws MalformedURLException {
        // topPrivateDomain: joins.com
        final InternetDomainName topPrivateDomain = InternetDomainName.from(host).topPrivateDomain();

        // ex. com
        final String topLevelDomain = topPrivateDomain.publicSuffix().toString();

        // ex. joins
        return topPrivateDomain.toString().replace("." + topLevelDomain, "");
    }
}
