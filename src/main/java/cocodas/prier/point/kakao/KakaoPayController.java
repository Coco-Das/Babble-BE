package cocodas.prier.point.kakao;

import cocodas.prier.point.kakao.request.RefundRequest;
import cocodas.prier.point.kakao.response.BaseResponse;
import cocodas.prier.point.kakao.response.KakaoCancelResponse;
import cocodas.prier.point.kakao.response.PayApproveResDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class KakaoPayController {

    private final KakaoPayService kakaoPayService;

    private static String getToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new RuntimeException("JWT Token is missing");
        }

        return auth.substring(7);
    }

    @PostMapping("/ready")
    public ResponseEntity<?> getRedirectUrl(@RequestBody PayInfoDto payInfoDto,
                                            @RequestHeader("Authorization") String auth) {

        String token = getToken(auth);

        try {
            return ResponseEntity.ok(kakaoPayService.getRedirectUrl(payInfoDto, token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }

    @GetMapping("/success")
    public void afterGetRedirectUrl(HttpServletResponse response,
                                    @RequestParam("id") Long id,
                                    @RequestParam("pg_token") String pgToken) {
        try {
            PayApproveResDto kakaoApprove = kakaoPayService.getApprove(pgToken, id);
            response.sendRedirect("http://localhost:3000/store");


        } catch (Exception e) {
            log.info("에러 발생");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }

    @GetMapping("/cancel")
    public void cancel(HttpServletResponse response) {
        try {
            response.sendRedirect("http://localhost:3000/store");
        } catch (Exception e) {
            log.info("에러 발생");
        }
//        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
//                .body(new BaseResponse<>(HttpStatus.EXPECTATION_FAILED.value(),"사용자가 결제를 취소하였습니다."));
    }

    @GetMapping("/fail")
    public void fail(HttpServletResponse response) {

        try {
            response.sendRedirect("http://localhost:3000/store");
        } catch (Exception e) {
            log.info("에러 발생");
        }

//        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
//                .body(new BaseResponse<>(HttpStatus.EXPECTATION_FAILED.value(),"결제가 실패하였습니다."));

    }

    // 환불
    @PostMapping("/refund")
    public ResponseEntity<?> refund(@RequestBody RefundRequest refundRequest,
                                    @RequestHeader("Authorization") String auth) {
        String token = getToken(auth);
        try {
            KakaoCancelResponse kakaoCancelResponse = kakaoPayService.kakaoCancel(
                    refundRequest.getTid(),
                    refundRequest.getCancelAmount(),
                    refundRequest.getCancelTaxFreeAmount(),
                    token);

            return new ResponseEntity<>(kakaoCancelResponse, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
        }
    }

}