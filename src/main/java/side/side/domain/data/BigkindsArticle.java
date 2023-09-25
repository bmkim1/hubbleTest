package side.side.domain.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BigkindsArticle {

    @JsonProperty("byline")
    private String byLine;

    @JsonProperty("dateline")
    private String date;

    @JsonProperty("news_id")
    private String newsId;

    @JsonProperty("images")
    private String images;

    @JsonProperty("provider")
    private String provider;

    @JsonProperty("provider_link_page")
    private String providerLinkPage;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

}
