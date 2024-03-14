package com.example.apitest.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 알려지지 않은 속성 무시
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값이 아닌 필드만 포함
public class Board {
    private Long id;
    private String title;
    private String content;
    private String writer; // 작성자 필드 추가
    private LocalDateTime date; // 작성일 필드 추가

    private Long categoryId; //카테고리ID

    private List<Hashtag> hashtags; // 해시태그 리스트 추가
}