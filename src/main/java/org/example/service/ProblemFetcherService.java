package org.example.service;

import org.example.entity.Problem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class ProblemFetcherService {

    /**
     * 백준 문제 페이지를 가져와 Problem 객체로 반환한다.
     * 사이트 구조가 변경되면 selector를 수정해야 함.
     */
    public Problem fetchFromBaekjoon(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/117.0.0.0 Safari/537.36")
                .referrer("https://www.acmicpc.net/") // Referrer 추가
                .timeout(10_000)
                .get();

        // 백준의 selector 예시 (변경 가능)
        String title = doc.selectFirst("span#problem_title") != null ?
                doc.selectFirst("span#problem_title").text() : "제목 없음";

        // 본문은 HTML을 그대로 저장 (나중에 요약/표시 시 HTML->텍스트 변환 가능)
        String descriptionHtml = doc.selectFirst("div#problem_description") != null ?
                doc.selectFirst("div#problem_description").html() : "";

        Problem p = Problem.builder()
                .title(title)
                .description(descriptionHtml)
                .level(1) // 기본값
                .sourceUrl(url)
                .build();

        return p;
    }
}