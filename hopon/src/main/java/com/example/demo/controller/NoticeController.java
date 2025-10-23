package com.example.demo.controller;

import com.example.demo.dto.NoticeDtos;
import com.example.demo.entity.NoticeType;
import com.example.demo.service.NoticeService;
import com.example.demo.support.AppIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService service;
    private final AppIdentityResolver resolver;

    @GetMapping
    public Page<NoticeDtos.Resp> list(HttpServletRequest req,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size,
                                      @RequestParam(defaultValue="updatedAt,desc") String sort,
                                      @RequestParam(required=false) String q,
                                      @RequestParam(required=false) NoticeType type){
        var id = resolver.resolve(req);
        Pageable pageable = PageRequest.of(page,size,Sort.by(parse(sort)));
        return service.list(q,id.role(),type,pageable).map(NoticeDtos.Resp::from);
    }

    @GetMapping("/{id}")
    public NoticeDtos.Resp detail(@PathVariable Long id,@RequestParam(defaultValue="true") boolean increase){
        return NoticeDtos.Resp.from(service.findAndIncrease(id,increase));
    }

    private Sort.Order parse(String s){
        String[] arr=s.split(",");
        return new Sort.Order(arr.length>1&&"asc".equalsIgnoreCase(arr[1])?Sort.Direction.ASC:Sort.Direction.DESC,arr[0]);
    }
}
