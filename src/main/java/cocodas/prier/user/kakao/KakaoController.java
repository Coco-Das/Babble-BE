package cocodas.prier.user.kakao;


import cocodas.prier.user.UserService;
import cocodas.prier.user.dto.response.LoginSuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class KakaoController {

    private final KakaoService kakaoService;

    @Value("${kakao.get_code_path}")
    private String getCodePath;
    @Value("${kakao.client_id}")
    private String client_id;
    @Value(("${kakao.redirect_uri}"))
    private String redirect_uri;
    @GetMapping("/kakao/login")
    public String loginPage(Model model) {
        String location = getCodePath + client_id + "&redirect_uri=" + redirect_uri;
        model.addAttribute("location", location);
        return "login";
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<LoginSuccessResponse> callback(@RequestParam("code") String code) {
//        log.info("kakaoController ===> callback 요청 들어옴");
        try {
            LoginSuccessResponse userResponse = kakaoService.kakaoLogin(code);
            return ResponseEntity.ok().body(userResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
