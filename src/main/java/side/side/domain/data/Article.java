package side.side.domain.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import side.side.domain.secured.SecuredKey;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor(staticName = "of")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Article {

    @SecuredKey
    private Integer articleSeq;

    @SecuredKey
    private Integer idSeq;

    private LocalDateTime createDatetime;

    private LocalDateTime updateDatetime;

    @NonNull
    private ArticleSource source;

    @NonNull
    private String link;

    private String newsId;

    @NonNull
    private String title;

    @NonNull
    private String prevContents;

    @NonNull
    private LocalDateTime publishDatetime;

    private String publisher;

    private String fullContents;

    private String author;

    private String images;

    private String originLink;

    // TODO: POC 이후 삭제
    private Boolean keyword1;

    private Boolean keyword2;

    private Boolean keyword3;

    private Boolean keyword4;
}
