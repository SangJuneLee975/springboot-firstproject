package com.example.apitest.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoardHashtag {
    private Long boardId;
    private Long hashtagId;
}
