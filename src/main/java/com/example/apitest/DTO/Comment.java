package com.example.apitest.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    private Long id; // 댓글의 고유 ID
    private Long boardId; // 게시글의 ID
    private String userId; // 댓글을 작성한 사용자의 ID
    private String content; // 댓글 내용
    private LocalDateTime date; // 댓글 작성 시간

}
