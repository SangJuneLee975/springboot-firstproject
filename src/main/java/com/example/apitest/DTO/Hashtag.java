package com.example.apitest.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hashtag {
    private Long id;
    private String name; // 해시태그 이름

    // String 타입의 name을 인자로 받는 생성자 추가
    public Hashtag(String name) {
        this.name = name;
    }
}
