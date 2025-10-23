package com.example.demo.service;

import com.example.demo.entity.Notice;
import com.example.demo.entity.NoticeType;
import com.example.demo.entity.TargetRole;
import com.example.demo.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository repo;

    @Transactional(readOnly=true)
    public Page<Notice> list(String q,String role,NoticeType type,Pageable pageable){
        TargetRole t = role==null ? TargetRole.ALL : TargetRole.valueOf(role);
        return repo.findForClient(q,type,t,pageable);
    }

    @Transactional
    public Notice findAndIncrease(Long id,boolean inc){
        Notice n=repo.findById(id).orElseThrow();
        if(inc) n.setViewCount(n.getViewCount()+1);
        return n;
    }
}
