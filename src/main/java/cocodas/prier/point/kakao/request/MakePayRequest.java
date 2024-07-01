package cocodas.prier.point.kakao.request;

import cocodas.prier.point.kakao.PayInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

@Component
@RequiredArgsConstructor
public class MakePayRequest {

    public PayRequest getReadyRequest(Long id, PayInfoDto payInfoDto) {
        LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();

        String orderId = "point" + id;

        map.add("cid", "TC0ONETIME");
        map.add("partner_order_id", orderId);
        map.add("partner_user_id", "prier");

        map.add("item_name",payInfoDto.getItemName());
        map.add("quantity", "1");
        map.add("total_amount", payInfoDto.getPrice()+"");
        map.add("tax_free_amount", "0");

        map.add("approval_url", "http://3.35.18.26:8080/api/payment/success?id=" + id); // 성공 시 redirect url
        map.add("cancel_url", "http://3.35.18.26:8080/api/payment/cancel"); // 취소 시 redirect url
        map.add("fail_url", "http://3.35.18.26:8080/api/payment/fail"); // 실패 시 redirect url

        return new PayRequest("https://kapi.kakao.com/v1/payment/ready", map);
    }

    public PayRequest getApproveRequest(String tid, Long id, String pgToken) {
        LinkedMultiValueMap<String,String> map=new LinkedMultiValueMap<>();

        String orderId = "point" + id;

        map.add("cid", "TC0ONETIME");

        map.add("tid", tid);
        map.add("partner_order_id", orderId);
        map.add("partner_user_id", "prier");

        map.add("pg_token", pgToken);

        return new PayRequest("https://kapi.kakao.com/v1/payment/approve", map);
    }
}