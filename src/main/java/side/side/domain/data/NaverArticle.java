package side.side.domain.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NaverArticle {
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String pubDate;

}
