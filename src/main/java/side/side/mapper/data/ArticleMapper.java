package side.side.mapper.data;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;
import side.side.domain.data.Article;
import side.side.domain.data.ArticleUpdateDatetime;
import side.side.domain.data.CompanySearchParam;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ArticleMapper {

    void addArticleList(List<Article> articleList);

    List<Article> getArticleListByIdSeq(@Param("idSeq") int idSeq);

    void addArticleUpdateDatetime (int idSeq);

    ArticleUpdateDatetime getArticleUpdateDatetime (int idSeq);

    void updateArticleUpdateDate(@Param("idSeq") int idSeq, @Param("datetime") LocalDateTime datetime);

    String getPublisherByDomain(String domain);

    void addDomainEmptyPublisher(@Param("host") String host, @Param("domain") String domain);
    void addInvestArticleList(List<Article> articleList);
    List<CompanySearchParam> getTestCompanySearchParams(int size);
}
