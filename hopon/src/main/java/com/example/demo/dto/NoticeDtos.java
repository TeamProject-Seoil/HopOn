package com.example.demo.dto;

import com.example.demo.entity.Notice;
import com.example.demo.entity.NoticeType;
import com.example.demo.entity.TargetRole;
import java.time.LocalDateTime;

public class NoticeDtos {
    public record Resp(Long id,String title,String content,
                       NoticeType noticeType,TargetRole targetRole,
                       long viewCount,LocalDateTime createdAt,LocalDateTime updatedAt){
        public static Resp from(Notice n){
            return new Resp(n.getId(),n.getTitle(),n.getContent(),
                    n.getNoticeType(),n.getTargetRole(),
                    n.getViewCount(),n.getCreatedAt(),n.getUpdatedAt());
        }
    }
}
