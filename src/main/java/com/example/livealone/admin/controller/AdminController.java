package com.example.livealone.admin.controller;

import com.example.livealone.admin.dto.AdminBroadcastListResponseDto;
import com.example.livealone.admin.dto.AdminRequestDto;
import com.example.livealone.admin.dto.AdminRoleResponseDto;
import com.example.livealone.admin.dto.AdminUserListResponseDto;
import com.example.livealone.admin.service.AdminService;
import com.example.livealone.global.dto.CommonResponseDto;
import com.example.livealone.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;

  @PutMapping("/admin")
  public ResponseEntity<CommonResponseDto<Void>> registerAdmin(@RequestBody AdminRequestDto requestDto, @AuthenticationPrincipal
      UserDetailsImpl userDetails) {

    adminService.registerAdmin(userDetails.getUser(), requestDto);

    return ResponseEntity.status(HttpStatus.OK).body(
        new CommonResponseDto<>(
            HttpStatus.OK.value(),
            "관리자로 등록되었습니다.",
            null)
    );
  }

  @GetMapping("/admin")
  public ResponseEntity<CommonResponseDto<AdminRoleResponseDto>> getUserRole(@AuthenticationPrincipal UserDetailsImpl userDetails) {
    AdminRoleResponseDto adminRoleResponseDto = adminService.getUserRole(userDetails.getUser());
    return ResponseEntity.status(HttpStatus.OK).body(
        new CommonResponseDto<>(
            HttpStatus.OK.value(),
            "권한을 조회하였습니다",
            adminRoleResponseDto)
    );
  }

  @GetMapping("/admin/broadcasts")
  public ResponseEntity<CommonResponseDto<Page<AdminBroadcastListResponseDto>>> getBroadcasts(@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestParam(defaultValue = "1") int page) {
    Page<AdminBroadcastListResponseDto> adminBroadcastListResponseDtoPage = adminService.getBroadcasts(userDetails.getUser(), page);

    return ResponseEntity.status(HttpStatus.OK).body(
        new CommonResponseDto<>(
            HttpStatus.OK.value(),
            "권한을 조회하였습니다",
            adminBroadcastListResponseDtoPage)
    );
  }

  @GetMapping("/admin/users")
  public ResponseEntity<CommonResponseDto<Page<AdminUserListResponseDto>>> getUsers(@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestParam(defaultValue = "1") int page) {
    Page<AdminUserListResponseDto> adminUserListResponseDtoPage = adminService.getUsers(userDetails.getUser(), page);

    return ResponseEntity.status(HttpStatus.OK).body(
        new CommonResponseDto<>(
            HttpStatus.OK.value(),
            "권한을 조회하였습니다",
            adminUserListResponseDtoPage)
    );
  }

  @GetMapping("/admin/broadcast/{broadcastId}")
  public void getBroadcast(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long broadcastId) {
    System.out.println("input!!!!!");
    System.out.println(broadcastId);
  }
}
