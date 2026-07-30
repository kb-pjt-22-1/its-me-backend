package site.benepay.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/index", "/index.html"})
    public String home() {
        return "index";
    }

    @GetMapping({"/pay", "/pay.html"})
    public String pay() {
        return "pay";
    }

    @GetMapping({"/wallet", "/wallet.html"})
    public String wallet() {
        return "wallet";
    }

    @GetMapping({"/history", "/history.html"})
    public String history() {
        return "history";
    }

    @GetMapping({"/payment-detail", "/payment-detail.html"})
    public String paymentDetail() {
        return "payment-detail";
    }

    @GetMapping({"/cards", "/cards.html"})
    public String cards() {
        return "cards";
    }

    @GetMapping({"/nearby", "/nearby.html"})
    public String nearby() {
        return "nearby";
    }
}
