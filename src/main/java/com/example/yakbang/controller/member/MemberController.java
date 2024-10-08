package com.example.yakbang.controller.member;

import com.example.yakbang.dto.member.ExpertMypageDTO;
import com.example.yakbang.dto.member.MemberJoinDTO;
import com.example.yakbang.dto.member.MemberModifyDTO;
import com.example.yakbang.dto.member.MemberMypageDTO;
import com.example.yakbang.service.member.ExpertService;
import com.example.yakbang.service.member.MemberService;
import com.example.yakbang.service.member.RecaptchaService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;
    private final ExpertService expertService;
    private final RecaptchaService recaptchaService;

    @GetMapping("/login")
    public String login() {
        return "member/login";
    }

    @PostMapping("/login")
    public String login(String loginId, String password, String memberType,
                        HttpSession session) {
        Long memberId = null;

        try {
            if ("general".equals(memberType)) {
                memberId = memberService.findMemberId(loginId, password);
            } else {
                memberId = expertService.findExpertId(loginId, password);
            }
        } catch (IllegalArgumentException e) {
            log.error(e.toString());
            return "member/login";
        } catch (Exception e) {
            log.error(e.toString());
            return "member/login";
        }

        // 로그인 성공 시 세션 처리
        session.setAttribute("memberId", memberId);
        return "redirect:/";
    }

    @GetMapping("/find_id")
    public String findId() {
        return "member/find_id";
    }



    @GetMapping("/find_password")
    public String findPassword() {
        return "member/find_password";
    }

    @PostMapping("/find_password")
    public String findPassword(@RequestParam("id") String loginId,
                               @RequestParam("email") String email,
                               @RequestParam("g-recaptcha-response") String recaptchaToken,
                               Model model) {

        String password = null;

        try {
            // reCAPTCHA 검증 서비스 호출
            boolean isVerified = recaptchaService.verifyRecaptcha(recaptchaToken);
            if (!isVerified) {
                model.addAttribute("msg", "reCAPTCHA 검증에 실패했습니다.");
                return "member/find_result2";
            }

            // 비밀번호 찾기 시도
            try {
                password = memberService.findPassword(loginId, email);
                log.info("memberService - ID: {}, Email: {}, Password: {}", loginId, email, password);
            } catch (Exception e) {
                // memberService에서 예외 발생 시
                log.error("Error finding password with memberService", e);
                try {
                    password = expertService.findExpertPassword(loginId, email);
                    log.info("expertService - ID: {}, Email: {}, Password: {}", loginId, email, password);
                } catch (Exception ex) {
                    // expertService에서 또 다른 예외 발생 시
                    log.error("Error finding password with expertService", ex);
                    password = "아이디, 이메일 정보가 일치하지 않습니다.";
                }
            }

        } catch (Exception e) {
            // reCAPTCHA 검증 중 예외 발생 시
            log.error("Error verifying reCAPTCHA", e);
            model.addAttribute("msg", "문제가 발생했습니다. 나중에 다시 시도해 주세요.");
            return "member/find_result2";
        }

        model.addAttribute("password", password);
        return "member/find_result2";
    }

    @GetMapping("/find_id_email")
    public String findIdEmail() {
        return "member/find_id_email";
    }

    @PostMapping("/find_id_email")
    public String findIdEmail(@RequestParam("name") String name,
                              @RequestParam("email") String email,
                              @RequestParam("g-recaptcha-response") String recaptchaToken,
                              Model model) {

        String loginId = null;

        try {
            // reCAPTCHA 검증 서비스 호출
            boolean isVerified = recaptchaService.verifyRecaptcha(recaptchaToken);
            System.out.println("isVerified = " + isVerified);

            if (isVerified) {
                // 검증 완료시 Id값 입력
                loginId = memberService.findLoginId(name, email);
            } else {
                model.addAttribute("msg", "reCAPTCHA 검증에 실패했습니다.");
                return "member/find_result";
            }

            if (loginId == null) {
                model.addAttribute("msg", "일치하는 회원의 ID가 없습니다.");
                return "member/find_result";
            }

            model.addAttribute("loginId", loginId);
            return "member/find_result";

        } catch (Exception e) {
            // 예외 처리
            e.printStackTrace(); // 디버깅 용도로 예외 스택 추적
            model.addAttribute("msg", "이름,이메일 정보가 일치하지 않습니다.");
            return "member/find_result";
        }
    }


    @GetMapping("/join")
    public String join(@ModelAttribute("memberJoinDTO") MemberJoinDTO memberJoinDTO,Model model,HttpSession session) {
        try {
            // 세션에서 kakaoId를 가져옴
            String kakaoId = (session.getAttribute("kakaoId") != null) ? session.getAttribute("kakaoId").toString() : null;
            System.out.println("kakaoId = " + kakaoId);
            // 세션에서 nickname을 가져옴
            String nickname = (session.getAttribute("nickName") != null) ? session.getAttribute("nickName").toString() : "";
            System.out.println("nickname = " + nickname);
            model.addAttribute("kakaoId", kakaoId);
            model.addAttribute("nickname", nickname);
        } catch (Exception e) {
            System.out.println("Error retrieving session attributes: " + e.getMessage());
        }


        return "member/join";
    }

    @PostMapping("/join")
    public String join(MemberJoinDTO memberJoinDTO,
                       Model model,
                       boolean check,
                       HttpSession session) {

        try {
            if (check){
                expertService.addExpert(memberJoinDTO);
                session.setAttribute("memberId", memberJoinDTO.getExpertId());
                session.setAttribute("memberType", "expert");
            }else {
                memberService.addMember(memberJoinDTO);
                session.setAttribute("memberId", memberJoinDTO.getMemberId());
                session.setAttribute("memberType", "general");
            }
        } catch (IllegalStateException e) {
            log.error(e.toString());
            model.addAttribute("memberJoinDTO", memberJoinDTO);
            model.addAttribute("duplicate", true);

            return "member/join";
        }

        System.out.println("memberJoinDTO = " + memberJoinDTO);
        return "redirect:/";
    }

    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        Long memberId = (Long) session.getAttribute("memberId");
        String memberType= (String) session.getAttribute("memberType");
        System.out.println("memberType = " + memberType);
        if ("general".equals(memberType)) {
            MemberMypageDTO memberDTO = memberService.searchMember(memberId);
            model.addAttribute("memberDTO", memberDTO);
        }else if ("expert".equals(memberType)) {
            ExpertMypageDTO expertMemberDTO = expertService.searchMember(memberId);
            log.info("////////////////////////////");
            model.addAttribute("memberType", memberType);
            model.addAttribute("memberDTO", expertMemberDTO);
        }


        return "member/mypage";
    }

    @GetMapping("/mypage-modify")
    public String mypageModify(Model model, HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        String memberType= (String) session.getAttribute("memberType");

        if ("general".equals(memberType)) {
            MemberMypageDTO memberDTO = memberService.searchMember(memberId);
            model.addAttribute("memberDTO", memberDTO);
        }else{
            ExpertMypageDTO expertMypageDTO = expertService.searchMember(memberId);
            model.addAttribute("memberType", memberType);
            model.addAttribute("memberDTO", expertMypageDTO);
        }

        return "member/mypage-modify";
    }

    @PostMapping("/mypage-modify")
    public String mypageModify(MemberModifyDTO memberModifyDTO, HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        String memberType= (String) session.getAttribute("memberType");
        if ("general".equals(memberType)){
            memberModifyDTO.setMemberId(memberId);
            memberService.modifyMemberInfo(memberModifyDTO);
        }else{
            memberModifyDTO.setExpertId(memberId);
            expertService.modifyExpertInfo(memberModifyDTO);
        }



        return "redirect:/member/mypage";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response){
        session.invalidate();
        // 로그인 ID 쿠키는 유지하고, 다른 관련 쿠키는 삭제하거나 무효화
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (!"loginId".equals(cookie.getName())) { // loginId는 유지
                    cookie.setValue(null);
                    cookie.setMaxAge(0); // 즉시 만료
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
        }
        return "redirect:/member/login";
    }

    @GetMapping("/leave")
    public String leave(HttpSession session){
        Long memberId = (Long) session.getAttribute("memberId");
        String memberType= (String) session.getAttribute("memberType");

        if ("general".equals(memberType)) {
            memberService.removeMemberInfo(memberId);
        }else{
            expertService.removeExpertInfo(memberId);
        }
        session.invalidate();
        return "/main";
    }

}