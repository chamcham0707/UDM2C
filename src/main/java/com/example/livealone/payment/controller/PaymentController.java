package com.example.livealone.payment.controller;

import com.example.livealone.global.config.URIConfig;
import com.example.livealone.global.dto.CommonResponseDto;
import com.example.livealone.payment.dto.PaymentRequestDto;
import com.example.livealone.payment.dto.PaymentResponseDto;
import com.example.livealone.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://livealone.shop"})
@Slf4j
public class PaymentController {

	private final PaymentService paymentService;
	private final URIConfig uriConfig;

	/**
	 * 카카오페이 결제 준비
	 *
	 * @param requestDto 결제 요청 DTO
	 * @return 결제 응답 DTO
	 */
	@PostMapping("/payment/kakao/process")
	public ResponseEntity<PaymentResponseDto> createKakaoPayReady(@RequestBody PaymentRequestDto requestDto) {
		log.debug("Get kakao API : {}",requestDto.getItemName());
		PaymentResponseDto response = paymentService.createKakaoPayReady(requestDto);
		if (response.getStatus().equals("FAILED")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 카카오페이 결제 승인
	 *
	 * @param pgToken 결제 승인 토큰
	 * @param orderId 주문 ID
	 * @param userId  사용자 ID
	 * @return 결제 응답 DTO
	 */
	@PostMapping("/payment/kakao/approve")
	public ResponseEntity<PaymentResponseDto> approveKakaoPayPayment(@RequestParam String pgToken, @RequestParam Long orderId, @RequestParam Long userId) {
		log.debug("Kakao apporve controller");
		PaymentResponseDto response = paymentService.approveKakaoPayPayment(pgToken, orderId, userId);
		if (response.getStatus().equals("FAILED")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * 카카오페이 결제 완료 처리
	 *
	 * @param pgToken 결제 승인 토큰
	 * @param orderId 주문 ID
	 * @param userId  사용자 ID
	 * @return 결제 응답 DTO
	 */
	@GetMapping("/payment/kakao/complete")
	public RedirectView completeKakaoPayment(@RequestParam("pg_token") String pgToken,
											@RequestParam("order_id") Long orderId,
											@RequestParam("user_id") Long userId) {
		PaymentResponseDto response = paymentService.approveKakaoPayPayment(pgToken, orderId, userId);
		RedirectView redirectView = new RedirectView();
		if (response.getStatus().equals("FAILED")) {
			String url = "http://livealone.shop/payment";
			redirectView.setUrl(url);
		} else {
			String url = "http://livealone.shop/completepayment";
			redirectView.setUrl(url);
		}
		return redirectView;
	}

	/**
	 * 카카오페이 결제 중 취소 처리
	 * @param orderId 주문 ID
	 * @return
	 */
	@GetMapping("/payment/kakao/cancel")
	public RedirectView cancelKakaoPayment(@RequestParam("order_id") Long orderId) {
		paymentService.cancelKakaoPayment(orderId);
		RedirectView view = new RedirectView();
		view.setUrl("http://livealone.shop/streaming");
		return view;
	}

	/**
	 * 결제 상태 조회
	 *
	 * @param paymentId 결제 ID
	 * @return 결제 응답 DTO
	 */
	@GetMapping("/payment/{paymentId}/status")
	public ResponseEntity<PaymentResponseDto> getPaymentStatus(@PathVariable Long paymentId) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}


	/**
	 * 토스페이 결제 준비
	 *
	 * @param requestDto 결제 요청 DTO
	 * @return 결제 응답 DTO
	 */
	@PostMapping("/payment/toss/process")
	public ResponseEntity<PaymentResponseDto> createTossPayReady(@RequestBody PaymentRequestDto requestDto) {
		PaymentResponseDto response = paymentService.createTossPayReady(requestDto);
		if (response.getStatus().equals("FAILED")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}


	/**
	 * 리팩토링: ORDER-CHECK 처리
	 */
	@GetMapping("/ORDER-CHECK")
	public void returnOrderCheckPage(@RequestParam String orderno,
		@RequestParam String status,
		@RequestParam String orderNo,
		@RequestParam String payMethod,
		@RequestParam(required = false) String bankCode,
		@RequestParam(required = false) String cardCompany,
		HttpServletResponse response) throws IOException {
		log.debug("Ret url redirect");
		String redirectUrl = paymentService.returnOrderCheckPage(orderno, status, orderNo, payMethod, bankCode, cardCompany);
		response.sendRedirect(UriComponentsBuilder.fromHttpUrl(redirectUrl)
			.build()
			.toUriString()
		);
	}

	/**
	 *
	 * @param orderno 주문 번호 (livealone:192024-08-08)
	 * @param response 처리 후 리다이렉트 할 uri
	 * @throws IOException
	 */
	@GetMapping("/payment/toss/cancel")
	public void cancelTossPayment(@RequestParam String orderno, HttpServletResponse response) throws IOException {
		String redirectUrl = paymentService.cancelOrderCheckPage(orderno);
		response.sendRedirect(UriComponentsBuilder.fromHttpUrl(redirectUrl)
			.build()
			.toUriString()
		);
	}


	// /**
	//  * 토스페이 결제 승인
	//  *
	//  * @param payToken 결제 고유 토큰
	//  * @return 결제 응답 DTO
	//  */
	// @PostMapping("/payment/toss/approve")
	// public ResponseEntity<PaymentResponseDto> approveTossPayPayment(@RequestParam String payToken) {
	// 	log.debug("approveTossPayPayment");
	// 	PaymentResponseDto response = paymentService.approveTossPayPayment(payToken);
	// 	if (response.getStatus().equals("FAILED")) {
	// 		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	// 	}
	// 	return ResponseEntity.ok(response);
	// }

	// /**
	//  * 토스페이 결제 완료 처리
	//  *
	//  * @param payToken 결제 승인 토큰
	//  * @param orderId 주문 ID
	//  * @param userId 사용자 ID
	//  * @return 결제 응답 DTO
	//  */
	// @GetMapping("/payment/toss/complete")
	// public RedirectView completeTossPayment(@RequestParam("payToken") String payToken,
	// 	@RequestParam("order_id") Long orderId,
	// 	@RequestParam("user_id") Long userId) {
	// 	log.debug("completTossPayment");
	// 	String status = "completed"; // 토스페이의 경우 결제 완료 후에 상태가 completed로 설정됨
	// 	String orderNo = String.format("livealone:%d", orderId) + LocalDate.now();
	// 	String redirectUrl = paymentService.returnOrderCheckPage(orderNo, status, orderNo, "TOSS_PAY", null, null);
	// 	RedirectView redirectView = new RedirectView();
	// 	redirectView.setUrl(redirectUrl);
	// 	return redirectView;
	// }

	/**
	 * 사용자별 결제 내역 조회
	 *
	 * @param userId 사용자 ID
	 * @return 결제 내역 리스트
	 */
	@GetMapping("/payment/user/{userId}/completed")
	public ResponseEntity<CommonResponseDto<List<PaymentResponseDto>>> getCompletedPaymentsByUserId(@PathVariable Long userId) {
		List<PaymentResponseDto> paymentResponses = paymentService.getCompletedPaymentsByUserId(userId);
		CommonResponseDto<List<PaymentResponseDto>> responseDto = CommonResponseDto.<List<PaymentResponseDto>>builder()
			.status(200)
			.message("결제 내역 조회 성공")
			.data(paymentResponses)
			.build();
		return ResponseEntity.ok(responseDto);
	}
}
