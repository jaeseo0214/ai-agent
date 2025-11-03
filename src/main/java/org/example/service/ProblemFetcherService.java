package org.example.service;

import org.example.entity.Problem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.example.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ProblemFetcherService {

    private final ProblemRepository problemRepository;

    /**
     * 백준 문제를 크롤링해 Problem 엔티티로 저장
     */
    public Problem fetchFromBaekjoon(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36")
                .referrer("https://www.acmicpc.net/")
                .timeout(10_000)
                .get();

        // ✅ 제목
        String title = doc.selectFirst("span#problem_title") != null ?
                doc.selectFirst("span#problem_title").text() : "제목 없음";

        // ✅ 설명 (HTML 그대로)
        String descriptionHtml = doc.selectFirst("div#problem_description") != null ?
                doc.selectFirst("div#problem_description").html() : "";

        // ✅ 입력/출력 설명
        String inputDesc = doc.selectFirst("div#problem_input") != null ?
                doc.selectFirst("div#problem_input").text() : "";
        String outputDesc = doc.selectFirst("div#problem_output") != null ?
                doc.selectFirst("div#problem_output").text() : "";

        // ✅ 예시 입력/출력 (첫 번째 예시 기준)
        String sampleInput = doc.selectFirst("pre#sample-input-1") != null ?
                doc.selectFirst("pre#sample-input-1").text() : "";
        String sampleOutput = doc.selectFirst("pre#sample-output-1") != null ?
                doc.selectFirst("pre#sample-output-1").text() : "";

        // ✅ Problem 생성
        Problem problem = Problem.builder()
                .title(title)
                .description(descriptionHtml + "\n\n입력 설명: " + inputDesc + "\n출력 설명: " + outputDesc)
                .level(1)
                .sourceUrl(url)
                .exampleInput(sampleInput)
                .exampleOutput(sampleOutput)
                .build();

        // ✅ DB 저장
        return problemRepository.save(problem);
    }
}