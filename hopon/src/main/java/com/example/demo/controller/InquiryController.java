// src/main/java/com/example/demo/controller/InquiryController.java
package com.example.demo.controller;

import com.example.demo.dto.InquiryDtos;
import com.example.demo.entity.InquiryAttachment;
import com.example.demo.entity.InquiryStatus;
import com.example.demo.service.InquiryService;
import com.example.demo.support.AppIdentity;
import com.example.demo.support.AppIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService service;
    private final AppIdentityResolver resolver;

    /** (공개) 목록 — 비밀글은 마스킹 */
    @GetMapping("/public")
    public Page<InquiryDtos.Resp> publicList(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(defaultValue = "createdAt,desc") String sort,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) InquiryStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parse(sort)));
        return service.listPublic(q, status, pageable)
                .map(i -> InquiryDtos.Resp.fromRedactedIfSecret(i, false));
    }

    /** 내 문의 목록 (소유자 전용) */
    @GetMapping
    public Page<InquiryDtos.Resp> list(HttpServletRequest req,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "createdAt,desc") String sort,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) InquiryStatus status) {
        var id = resolver.resolve(req);
        if (!StringUtils.hasText(id.email()))
            throw new IllegalArgumentException("X-User-Email header required");
        Pageable pageable = PageRequest.of(page, size, Sort.by(parse(sort)));
        return service.list(id.email(), q, status, pageable).map(InquiryDtos.Resp::from);
    }

    /** 내 문의 상세 (소유자 전용) */
    @GetMapping("/{id}")
    public InquiryDtos.Resp detail(HttpServletRequest req, @PathVariable Long id) {
        var me = resolver.resolve(req);
        if (!StringUtils.hasText(me.email()))
            throw new IllegalArgumentException("X-User-Email header required");
        return InquiryDtos.Resp.from(service.getMine(id, me.email()));
    }

    /** 생성 — 회원/비회원 모두 가능. 문자열 RequestParam, 파일은 RequestPart */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InquiryDtos.Resp create(HttpServletRequest req,
                                   @RequestParam("title") String title,
                                   @RequestParam("content") String content,
                                   @RequestParam(value = "name", required = false) String name,
                                   @RequestParam(value = "secret", required = false, defaultValue = "false") boolean secret,
                                   @RequestParam(value = "password", required = false) String password,
                                   @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        var id = resolver.resolve(req);
        if (!StringUtils.hasText(id.email()))
            throw new IllegalArgumentException("X-User-Email header required");

        if (secret) {
            if (!StringUtils.hasText(password) || password.trim().length() < 4 || password.length() > 64) {
                throw new IllegalArgumentException("비밀글 비밀번호는 4~64자여야 합니다.");
            }
        }
        String nm = StringUtils.hasText(name) ? name : (id.isDriver() ? "기사" : "사용자");
        var dto = new InquiryDtos.Create(nm, id.email(), id.userId(), title, content, secret, password);
        return InquiryDtos.Resp.from(service.create(dto, files));
    }

    /** (공개) 상세 — 비밀글은 비밀번호 필요 */


 @GetMapping("/{id}/public")
 public InquiryDtos.Resp publicDetail(HttpServletRequest req,
                                      @PathVariable Long id,
                                      @RequestParam(required = false) String password) {
     var me = resolver.resolve(req);
     var inq = service.get(id);
     boolean isOwner = isOwner(me, inq);

     boolean ok = service.canViewPublic(inq, password, isOwner);

     if (!ok) {
         // ✨ 틀리면 403로 명확히 실패
         throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid password");
     }
     return InquiryDtos.Resp.from(inq);
 }

    /** (공개) 첨부 */
    @GetMapping("/{inquiryId}/attachments/{attId}/public")
    public ResponseEntity<ByteArrayResource> publicDownload(HttpServletRequest req,
                                                            @PathVariable Long inquiryId,
                                                            @PathVariable Long attId,
                                                            @RequestParam(required = false) String password,
                                                            @RequestParam(defaultValue = "true") boolean inline) {
        var me = resolver.resolve(req);
        var inq = service.get(inquiryId);
        boolean isOwner = isOwner(me, inq);
        boolean ok = service.canViewPublic(inq, password, isOwner);
        if (!ok) throw new IllegalArgumentException("비밀글 비밀번호가 올바르지 않습니다.");
        InquiryAttachment a = service.getAttachment(inquiryId, attId);
        String filename = a.getFilename() == null ? "file" : a.getFilename();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encoded;

        MediaType mt;
        try { mt = MediaType.parseMediaType(a.getContentType()); }
        catch (Exception e) { mt = MediaType.APPLICATION_OCTET_STREAM; }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mt)
                .contentLength(a.getSize())
                .body(new ByteArrayResource(a.getBytes()));
    }

    /** (소유자) 첨부 */
    @GetMapping("/{inquiryId}/attachments/{attId}")
    public ResponseEntity<ByteArrayResource> downloadMine(HttpServletRequest req,
                                                          @PathVariable Long inquiryId,
                                                          @PathVariable Long attId,
                                                          @RequestParam(defaultValue = "true") boolean inline) {
        var me = resolver.resolve(req);
        if (!StringUtils.hasText(me.email()))
            throw new IllegalArgumentException("X-User-Email header required");
        InquiryAttachment a = service.getAttachmentMine(inquiryId, attId, me.email());

        String filename = a.getFilename() == null ? "file" : a.getFilename();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + encoded;

        MediaType mt;
        try { mt = MediaType.parseMediaType(a.getContentType()); }
        catch (Exception e) { mt = MediaType.APPLICATION_OCTET_STREAM; }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mt)
                .contentLength(a.getSize())
                .body(new ByteArrayResource(a.getBytes()));
    }

    /* ---- helpers ---- */
    private Sort.Order parse(String s) {
        String[] arr = s.split(",");
        String col = arr[0];
        boolean asc = arr.length > 1 && "asc".equalsIgnoreCase(arr[1]);
        return new Sort.Order(asc ? Sort.Direction.ASC : Sort.Direction.DESC, col);
    }

    private boolean isOwner(AppIdentity id, com.example.demo.entity.Inquiry inq) {
        return id != null && StringUtils.hasText(id.email()) && id.email().equalsIgnoreCase(inq.getEmail());
    }
}
